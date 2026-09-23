import QtQuick
import OpenNOW

// GoudaNOW "All Games" — same layout as the Android TV `TvAllGamesScreen`:
// icon + title, the focused game's name (scrolling if long), Search and Sort
// fixed top-right, a six-column grid of portrait box art, and button hints.
FocusScope {
    id: root
    objectName: "goudaLibraryScreen"

    property string searchQuery: ""
    readonly property var sortModes: [
        { id: "library", label: qsTr("Library order") },
        { id: "recent", label: qsTr("Recently played") },
        { id: "title", label: qsTr("Title") }
    ]
    property int sortIndex: Math.max(0, sortModes.findIndex(mode => mode.id === String(ShellStore.focusIndex("gouda-library-sort"))))

    readonly property var games: {
        const seen = {}
        const query = root.searchQuery.trim().toLowerCase()
        const result = []
        const source = ShellStore.catalogGames || []
        for (let index = 0; index < source.length; ++index) {
            const game = source[index]
            const id = GoudaTheme.gameId(game)
            if (seen[id])
                continue
            seen[id] = true
            if (query.length && String(game.searchText || game.title || "").toLowerCase().indexOf(query) < 0)
                continue
            result.push(game)
        }
        const mode = root.sortModes[root.sortIndex].id
        if (mode === "title")
            result.sort((a, b) => String(a.title).localeCompare(String(b.title)))
        else if (mode === "recent")
            result.sort((a, b) => String(b.lastPlayed || "").localeCompare(String(a.lastPlayed || ""))
                || String(a.title).localeCompare(String(b.title)))
        return result
    }
    readonly property var focusedGame: grid.currentIndex >= 0 && grid.currentIndex < games.length ? games[grid.currentIndex] : null

    function cycleSort() {
        root.sortIndex = (root.sortIndex + 1) % root.sortModes.length
        ShellStore.rememberFocus("gouda-library-sort", root.sortModes[root.sortIndex].id)
        Qt.callLater(root.restoreFocus)
    }
    function openSearch() { keyboard.openKeyboard(root.searchQuery) }

    // Returns to the exact game last highlighted here (kept by game id).
    function restoreFocus() {
        const wanted = ShellStore.focusIndex("gouda-library")
        let index = 0
        for (let i = 0; i < root.games.length; ++i) {
            if (GoudaTheme.gameId(root.games[i]) === wanted) { index = i; break }
        }
        grid.currentIndex = root.games.length ? index : -1
        if (root.games.length) {
            grid.positionViewAtIndex(index, GridView.Contain)
            grid.forceActiveFocus()
        } else {
            searchPill.forceActiveFocus()
        }
    }
    Component.onCompleted: Qt.callLater(root.restoreFocus)

    Keys.onPressed: event => {
        if (keyboard.presented)
            return
        if (event.isAutoRepeat) {
            if (event.key === Qt.Key_X || event.key === Qt.Key_Y) event.accepted = true
            return
        }
        if (event.key === Qt.Key_X || event.key === Qt.Key_Back) {
            root.openSearch()
            event.accepted = true
        } else if (event.key === Qt.Key_Y) {
            root.cycleSort()
            event.accepted = true
        } else if (event.key === Qt.Key_Escape && root.searchQuery !== "") {
            // First B clears an active search; the next one leaves the screen.
            root.searchQuery = ""
            Qt.callLater(root.restoreFocus)
            event.accepted = true
        }
    }

    GoudaFrame {
        anchors.fill: parent
        title: qsTr("All Games")
        icon: GoudaTheme.icon("apps", false)
        hints: [
            { glyph: "X", label: qsTr("Search") },
            { glyph: "Y", label: qsTr("Sort") },
            { glyph: "B", label: qsTr("Back") },
            { glyph: "A", label: qsTr("Start") }
        ]
        headerCenter: GoudaMarquee {
            anchors.verticalCenter: parent ? parent.verticalCenter : undefined
            width: parent ? parent.width : 0
            text: root.focusedGame ? root.focusedGame.title : ""
        }
        headerTrailing: [
            GoudaPill {
                id: searchPill
                label: root.searchQuery !== "" ? qsTr("“%1”").arg(root.searchQuery) : qsTr("Search")
                iconName: "search"
                KeyNavigation.right: sortPill
                KeyNavigation.down: grid
                onClicked: root.openSearch()
            },
            GoudaPill {
                id: sortPill
                label: qsTr("Sort: %1").arg(root.sortModes[root.sortIndex].label)
                KeyNavigation.left: searchPill
                KeyNavigation.down: grid
                onClicked: root.cycleSort()
            }
        ]

        GridView {
            id: grid
            objectName: "goudaLibraryGrid"
            readonly property int columns: 6
            readonly property real gap: 44
            anchors.fill: parent
            anchors.leftMargin: 112; anchors.rightMargin: 112
            topMargin: 48; bottomMargin: 48
            cellWidth: (width - 1) / columns
            cellHeight: (cellWidth - gap) / GoudaTheme.boxArtRatio + gap
            model: root.games
            clip: true
            keyNavigationWraps: false
            highlightMoveDuration: GoudaTheme.animMs
            KeyNavigation.up: searchPill
            onCurrentIndexChanged: if (activeFocus && currentIndex >= 0 && currentIndex < root.games.length)
                ShellStore.rememberFocus("gouda-library", GoudaTheme.gameId(root.games[currentIndex]))
            Keys.onPressed: event => {
                if ((event.key === Qt.Key_Return || event.key === Qt.Key_Enter) && !event.isAutoRepeat
                        && root.focusedGame) {
                    GoudaTheme.play(root.focusedGame)
                    event.accepted = true
                } else if (event.key === Qt.Key_Up && currentIndex < columns) {
                    searchPill.forceActiveFocus()
                    event.accepted = true
                }
            }
            delegate: Item {
                id: tile
                required property var modelData
                required property int index
                readonly property bool focused: GridView.isCurrentItem && grid.activeFocus
                width: grid.cellWidth; height: grid.cellHeight
                z: focused ? 10 : 0
                Item {
                    anchors.centerIn: parent
                    width: grid.cellWidth - grid.gap
                    height: grid.cellHeight - grid.gap
                    scale: tile.focused ? 1.08 : 1
                    Behavior on scale { NumberAnimation { duration: GoudaTheme.animMs; easing.type: Easing.OutCubic } }
                    Rectangle { anchors.fill: parent; radius: 8; color: GoudaTheme.tilePlaceholder }
                    Image {
                        anchors.fill: parent
                        source: tile.modelData.imageUrl || tile.modelData.keyArtUrl || ""
                        fillMode: Image.PreserveAspectCrop
                        asynchronous: true
                        sourceSize.width: 360
                    }
                    GoudaFocusRing { shown: tile.focused; radius: 8; thickness: 7; glow: 12 }
                    MouseArea {
                        anchors.fill: parent
                        onClicked: { grid.currentIndex = tile.index; grid.forceActiveFocus(); GoudaTheme.play(tile.modelData) }
                    }
                }
            }
        }
        Text {
            anchors.centerIn: parent
            visible: root.games.length === 0
            text: ShellStore.catalogState === "loading" || !ShellStore.ready ? qsTr("Loading your library…")
                : root.searchQuery !== "" ? qsTr("No games match “%1”.").arg(root.searchQuery)
                : qsTr("No games in your library yet.")
            color: GoudaTheme.secondaryText
            font.family: GoudaTheme.font; font.pixelSize: 38
        }
    }

    // The console on-screen keyboard (works with the controller on Steam Deck).
    VirtualKeyboard {
        id: keyboard
        objectName: "goudaLibraryKeyboard"
        anchors.fill: parent
        onAccepted: value => {
            root.searchQuery = value
            Qt.callLater(root.restoreFocus)
        }
        onCanceled: Qt.callLater(root.restoreFocus)
    }
}
