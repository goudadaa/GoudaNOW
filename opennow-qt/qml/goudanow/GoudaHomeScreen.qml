import QtQuick
import OpenNOW

// GoudaNOW HOME menu — same layout as the Android TV `TvHomeScreen`:
// profile / clock / network across the top, a row of portrait box art that
// grows 1.18x with a cyan glow and shows the title underneath, and a row of
// round buttons (All Games, Controllers, Settings, Quit) along the bottom.
FocusScope {
    id: root
    objectName: "goudaHomeScreen"

    readonly property int carouselLimit: 12
    readonly property var games: {
        const seen = {}
        const result = []
        const source = ShellStore.catalogGames || []
        for (let index = 0; index < source.length && result.length < root.carouselLimit; ++index) {
            const id = GoudaTheme.gameId(source[index])
            if (seen[id])
                continue
            seen[id] = true
            result.push(source[index])
        }
        return result
    }
    // Space between the top bar and the button row decides the card size, so
    // the buttons and their labels are never squeezed on any screen shape.
    readonly property real cardHeight: Math.max(300, Math.min(420,
        (carouselArea.height - 150) / GoudaTheme.focusedCardScale))
    readonly property real cardWidth: cardHeight * GoudaTheme.boxArtRatio

    function restoreFocus() {
        if (root.games.length === 0) {
            navRow.focusFirst()
            return
        }
        const wanted = ShellStore.focusIndex("gouda-home")
        let index = 0
        for (let i = 0; i < root.games.length; ++i) {
            if (GoudaTheme.gameId(root.games[i]) === wanted) { index = i; break }
        }
        carousel.currentIndex = index
        carousel.positionViewAtIndex(index, ListView.Contain)
        carousel.forceActiveFocus()
    }

    Component.onCompleted: Qt.callLater(root.restoreFocus)
    onGamesChanged: if (!carousel.activeFocus && !navRow.activeFocus) Qt.callLater(root.restoreFocus)

    Keys.onPressed: event => {
        // Keep the stock shell shortcuts (X = details, Y = friends) off this screen.
        if (event.key === Qt.Key_X || event.key === Qt.Key_Y)
            event.accepted = true
        else if ((event.key === Qt.Key_Escape || event.key === Qt.Key_Back) && navRow.activeFocus) {
            root.restoreFocus()
            event.accepted = true
        }
    }

    Rectangle { anchors.fill: parent; color: GoudaTheme.background }

    // ---------------------------------------------------------------- top bar
    Item {
        id: topBar
        x: GoudaTheme.sidePadding; y: 40
        width: parent.width - 2 * GoudaTheme.sidePadding
        height: 120
        Rectangle {
            id: profile
            width: 104; height: 104; radius: 52
            anchors.verticalCenter: parent.verticalCenter
            color: GoudaTheme.navButton
            readonly property var user: ShellStore.authSession ? ShellStore.authSession.user : null
            Text {
                anchors.centerIn: parent
                text: profile.user && profile.user.displayName ? profile.user.displayName.trim().charAt(0).toUpperCase() : "?"
                color: GoudaTheme.primaryText
                font.family: GoudaTheme.font; font.pixelSize: 44; font.weight: Font.Bold
            }
        }
        Row {
            anchors.right: parent.right
            anchors.verticalCenter: parent.verticalCenter
            spacing: 36
            Text {
                id: clock
                property date now: new Date()
                text: Qt.formatTime(now, Qt.locale().timeFormat(Locale.ShortFormat))
                color: GoudaTheme.primaryText
                font.family: GoudaTheme.font; font.pixelSize: 44; font.weight: Font.DemiBold
                anchors.verticalCenter: parent.verticalCenter
                Timer { interval: 5000; running: true; repeat: true; triggeredOnStart: true; onTriggered: clock.now = new Date() }
            }
            Image {
                source: ShellStore.ready ? GoudaTheme.icon("wifi", false) : GoudaTheme.greyIcon("wifi")
                width: 52; height: 52; sourceSize: Qt.size(52, 52)
                anchors.verticalCenter: parent.verticalCenter
            }
        }
    }

    // ---------------------------------------------------------------- carousel
    Item {
        id: carouselArea
        anchors.top: topBar.bottom
        anchors.bottom: navRow.top
        width: parent.width

        ListView {
            id: carousel
            objectName: "goudaHomeCarousel"
            anchors.verticalCenter: parent.verticalCenter
            width: parent.width
            height: root.cardHeight * GoudaTheme.focusedCardScale + 104
            orientation: ListView.Horizontal
            spacing: 52
            leftMargin: 176; rightMargin: 176
            clip: false
            model: root.games
            keyNavigationWraps: false
            highlightMoveDuration: GoudaTheme.animMs
            preferredHighlightBegin: 176
            preferredHighlightEnd: width - 176 - root.cardWidth
            highlightRangeMode: ListView.ApplyRange
            KeyNavigation.down: navRow
            onCurrentIndexChanged: if (activeFocus && currentIndex >= 0)
                ShellStore.rememberFocus("gouda-home", GoudaTheme.gameId(root.games[currentIndex]))

            delegate: Item {
                id: card
                required property var modelData
                required property int index
                readonly property bool focused: ListView.isCurrentItem && carousel.activeFocus
                width: root.cardWidth
                height: carousel.height
                z: focused ? 10 : 0

                Item {
                    id: art
                    width: root.cardWidth; height: root.cardHeight
                    y: root.cardHeight * (GoudaTheme.focusedCardScale - 1) / 2 + 8
                    scale: card.focused ? GoudaTheme.focusedCardScale : 1
                    Behavior on scale { NumberAnimation { duration: GoudaTheme.animMs; easing.type: Easing.OutCubic } }
                    Rectangle { anchors.fill: parent; radius: 8; color: GoudaTheme.tilePlaceholder }
                    Image {
                        anchors.fill: parent
                        source: card.modelData.imageUrl || card.modelData.keyArtUrl || ""
                        fillMode: Image.PreserveAspectCrop
                        asynchronous: true
                        sourceSize.width: 420
                    }
                    GoudaFocusRing { shown: card.focused; radius: 8; thickness: 7 }
                    MouseArea {
                        anchors.fill: parent
                        onClicked: { carousel.currentIndex = card.index; carousel.forceActiveFocus(); GoudaTheme.play(card.modelData) }
                    }
                }
                Text {
                    anchors.horizontalCenter: art.horizontalCenter
                    y: art.y + root.cardHeight * (1 + (GoudaTheme.focusedCardScale - 1) / 2) + 22
                    width: root.cardWidth * 2.2
                    horizontalAlignment: Text.AlignHCenter
                    elide: Text.ElideRight
                    text: card.modelData.title || ""
                    color: GoudaTheme.focusCyan
                    font.family: GoudaTheme.font; font.pixelSize: 40; font.weight: Font.Medium
                    opacity: card.focused ? 1 : 0
                    Behavior on opacity { NumberAnimation { duration: GoudaTheme.animMs } }
                }
            }

            Keys.onPressed: event => {
                if ((event.key === Qt.Key_Return || event.key === Qt.Key_Enter) && !event.isAutoRepeat
                        && currentIndex >= 0) {
                    GoudaTheme.play(root.games[currentIndex])
                    event.accepted = true
                }
            }
        }

        Text {
            anchors.centerIn: parent
            visible: root.games.length === 0
            text: ShellStore.catalogState === "error" ? qsTr("Couldn't load your games. Check your connection.")
                : ShellStore.catalogState === "loading" || !ShellStore.ready ? qsTr("Loading your library…")
                : qsTr("No games in your library yet. Open All Games to browse the catalogue.")
            color: GoudaTheme.secondaryText
            font.family: GoudaTheme.font; font.pixelSize: 36
        }
    }

    // ---------------------------------------------------------------- bottom buttons
    FocusScope {
        id: navRow
        anchors.bottom: parent.bottom
        width: parent.width
        height: 272
        KeyNavigation.up: root.games.length > 0 ? carousel : null

        function focusFirst() { navButtons.itemAt(0).forceActiveFocus() }

        Rectangle { x: GoudaTheme.sidePadding; width: parent.width - 2 * GoudaTheme.sidePadding; height: 2; color: GoudaTheme.divider }
        Row {
            anchors.horizontalCenter: parent.horizontalCenter
            y: 34
            spacing: 56
            Repeater {
                id: navButtons
                model: [
                    { icon: "apps", label: qsTr("All Games"), action: () => AppController.navigate("library") },
                    { icon: "controller", label: qsTr("Controllers"), action: () => { ShellStore.rememberFocus("gouda-settings-category", "controllers"); AppController.navigate("settings") } },
                    { icon: "settings", label: qsTr("Settings"), action: () => AppController.navigate("settings") },
                    { icon: "power", label: qsTr("Quit"), action: () => quitDialog.open() }
                ]
                FocusScope {
                    id: navButton
                    required property var modelData
                    required property int index
                    width: 192; height: 200
                    focus: index === 0
                    KeyNavigation.left: index > 0 ? navButtons.itemAt(index - 1) : null
                    KeyNavigation.right: index < navButtons.count - 1 ? navButtons.itemAt(index + 1) : null
                    Keys.onPressed: event => {
                        if ((event.key === Qt.Key_Return || event.key === Qt.Key_Enter) && !event.isAutoRepeat) {
                            navButton.modelData.action()
                            event.accepted = true
                        }
                    }
                    Rectangle {
                        id: circle
                        anchors.horizontalCenter: parent.horizontalCenter
                        width: 128; height: 128; radius: 64
                        color: navButton.activeFocus ? GoudaTheme.navButtonFocused : GoudaTheme.navButton
                        scale: navButton.activeFocus ? 1.12 : 1
                        Behavior on scale { NumberAnimation { duration: GoudaTheme.animMs } }
                        Image {
                            anchors.centerIn: parent
                            width: 60; height: 60; sourceSize: Qt.size(60, 60)
                            source: GoudaTheme.icon(navButton.modelData.icon, navButton.activeFocus)
                        }
                        GoudaFocusRing { shown: navButton.activeFocus; radius: 64; thickness: 6; glow: 10 }
                        MouseArea { anchors.fill: parent; onClicked: { navButton.forceActiveFocus(); navButton.modelData.action() } }
                    }
                    Text {
                        anchors.horizontalCenter: parent.horizontalCenter
                        y: circle.height + 26
                        text: navButton.modelData.label
                        color: GoudaTheme.focusCyan
                        font.family: GoudaTheme.font; font.pixelSize: 30
                        opacity: navButton.activeFocus ? 1 : 0
                        Behavior on opacity { NumberAnimation { duration: GoudaTheme.animMs } }
                    }
                }
            }
        }
    }

    GoudaDialog {
        id: quitDialog
        title: qsTr("Quit GoudaNOW?")
        actions: [
            { label: qsTr("Cancel"), action: () => root.restoreFocus() },
            { label: qsTr("Quit"), action: () => AppController.quitApplication() }
        ]
        onDismissed: navRow.focusFirst()
    }
}
