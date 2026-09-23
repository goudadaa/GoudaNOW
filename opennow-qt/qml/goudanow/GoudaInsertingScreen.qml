import QtQuick
import OpenNOW

// GoudaNOW queue / "getting ready" screen — same layout as the Android TV
// `TvQueueScreen`: the game's box art, its title, the queue position or launch
// step, and Cancel. Cancelling uses the stock confirmation flow.
FocusScope {
    id: root
    objectName: "goudaInsertingScreen"
    readonly property var game: ShellStore.selectedGame || ({ title: qsTr("Your game") })
    readonly property var session: ShellStore.activeSession || ({})
    readonly property bool failed: ShellStore.streamState === "error"
    readonly property int position: Number(root.session.queuePosition || 0)

    Component.onCompleted: Qt.callLater(() => (retryRow.visible ? retryRow : cancelRow).forceActiveFocus())

    Keys.onPressed: event => {
        if (event.isAutoRepeat)
            return
        if (event.key === Qt.Key_Escape || event.key === Qt.Key_Back) {
            event.accepted = true
            ShellStore.requestStreamExitConfirmation()
        } else if (event.key === Qt.Key_X || event.key === Qt.Key_Y) {
            event.accepted = true
        }
    }

    Rectangle { anchors.fill: parent; color: GoudaTheme.background }

    Row {
        anchors.centerIn: parent
        spacing: 96
        Item {
            width: 600 * GoudaTheme.boxArtRatio; height: 600
            anchors.verticalCenter: parent.verticalCenter
            Rectangle { anchors.fill: parent; radius: 8; color: GoudaTheme.tilePlaceholder }
            Image {
                anchors.fill: parent
                source: root.game.imageUrl || root.game.keyArtUrl || ""
                fillMode: Image.PreserveAspectCrop
                asynchronous: true
            }
        }
        Column {
            width: 920
            spacing: 28
            anchors.verticalCenter: parent.verticalCenter
            Text {
                width: parent.width
                text: root.game.title || ""
                wrapMode: Text.WordWrap
                color: GoudaTheme.primaryText
                font.family: GoudaTheme.font; font.pixelSize: 56; font.weight: Font.Medium
            }
            Row {
                spacing: 28
                Item {
                    width: 56; height: 56
                    visible: !root.failed
                    Rectangle {
                        anchors.fill: parent; radius: 28; color: "transparent"
                        border.width: 7; border.color: GoudaTheme.divider
                    }
                    Rectangle {
                        width: 18; height: 18; radius: 9; color: GoudaTheme.focusCyan
                        x: 19; y: -4
                    }
                    RotationAnimation on rotation { from: 0; to: 360; duration: 1100; loops: Animation.Infinite; running: !root.failed && !AppController.reducedMotion }
                }
                Text {
                    anchors.verticalCenter: parent.verticalCenter
                    text: root.failed ? qsTr("Couldn't start")
                        : root.position > 0 ? qsTr("You're number %1 in the queue").arg(root.position)
                        : qsTr("Getting a server ready…")
                    color: root.failed ? GoudaTheme.error : GoudaTheme.focusCyan
                    font.family: GoudaTheme.font; font.pixelSize: 42
                }
            }
            Text {
                width: parent.width
                wrapMode: Text.WordWrap
                text: I18n.source(ShellStore.streamMessage, I18n.revision)
                color: root.failed ? GoudaTheme.error : GoudaTheme.secondaryText
                font.family: GoudaTheme.font; font.pixelSize: 34
            }
            Item { width: 1; height: 20 }
            GoudaListRow {
                id: retryRow
                width: 620
                visible: root.failed && (ShellStore.pendingLaunchParams !== null || ShellStore.activeSession !== null || ShellStore.conflictSession !== null)
                label: qsTr("Try again")
                KeyNavigation.down: cancelRow
                onClicked: if (!ShellStore.streamBusy) ShellStore.retrySessionLaunch()
            }
            GoudaListRow {
                id: cancelRow
                width: 620
                label: root.failed ? qsTr("Back") : qsTr("Cancel")
                labelColor: root.failed ? GoudaTheme.primaryText : GoudaTheme.error
                KeyNavigation.up: retryRow.visible ? retryRow : null
                onClicked: ShellStore.requestStreamExitConfirmation()
            }
        }
    }
}
