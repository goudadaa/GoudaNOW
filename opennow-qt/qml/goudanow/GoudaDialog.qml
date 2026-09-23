import QtQuick

// Switch-style modal: centred grey panel, title and message, then a column of
// full-width buttons. Focus stays inside while it is open; B closes it.
FocusScope {
    id: dialog
    property string title: ""
    property string message: ""
    // [{label: "Quit", action: function() {...}}, ...]
    property var actions: []
    property bool opaque: false
    signal dismissed()

    anchors.fill: parent
    visible: false
    z: 2000

    function open() {
        visible = true
        Qt.callLater(() => { if (buttons.count > 0) buttons.itemAt(0).forceActiveFocus() })
    }
    function close() { visible = false; dismissed() }

    Keys.onPressed: event => {
        if (event.key === Qt.Key_Escape || event.key === Qt.Key_Back) {
            dialog.close()
        }
        // Swallow everything else so nothing underneath reacts.
        event.accepted = true
    }

    Rectangle { anchors.fill: parent; color: dialog.opaque ? GoudaTheme.background : GoudaTheme.scrim }
    MouseArea { anchors.fill: parent }

    Rectangle {
        id: panel
        anchors.centerIn: parent
        width: 1120
        height: content.height + buttonColumn.height + 2
        radius: 12
        color: GoudaTheme.dialogPanel
        border.color: GoudaTheme.divider; border.width: 2
        Column {
            id: content
            width: parent.width
            topPadding: 56; bottomPadding: 56; leftPadding: 72; rightPadding: 72
            spacing: 28
            Text {
                width: parent.width - 144
                text: dialog.title
                horizontalAlignment: Text.AlignHCenter
                wrapMode: Text.WordWrap
                color: GoudaTheme.primaryText
                font.family: GoudaTheme.font; font.pixelSize: 46; font.weight: Font.DemiBold
            }
            Text {
                visible: dialog.message !== ""
                width: parent.width - 144
                text: dialog.message
                horizontalAlignment: Text.AlignHCenter
                wrapMode: Text.WordWrap
                color: GoudaTheme.secondaryText
                font.family: GoudaTheme.font; font.pixelSize: 34
            }
        }
        Rectangle { y: content.height; width: parent.width; height: 2; color: GoudaTheme.divider }
        Column {
            id: buttonColumn
            y: content.height + 2
            width: parent.width
            padding: 16
            spacing: 8
            Repeater {
                id: buttons
                model: dialog.actions
                FocusScope {
                    id: button
                    required property var modelData
                    required property int index
                    width: buttonColumn.width - 32
                    height: 108
                    KeyNavigation.up: index > 0 ? buttons.itemAt(index - 1) : null
                    KeyNavigation.down: index < buttons.count - 1 ? buttons.itemAt(index + 1) : null
                    Keys.onPressed: event => {
                        if ((event.key === Qt.Key_Return || event.key === Qt.Key_Enter) && !event.isAutoRepeat) {
                            dialog.visible = false
                            button.modelData.action()
                            event.accepted = true
                        }
                    }
                    Rectangle {
                        anchors.fill: parent; radius: 6
                        color: button.activeFocus ? GoudaTheme.rowFocused : "transparent"
                        GoudaFocusRing { shown: button.activeFocus; thickness: 5; glow: 0 }
                    }
                    Text {
                        anchors.centerIn: parent
                        text: button.modelData.label
                        color: GoudaTheme.focusCyan
                        font.family: GoudaTheme.font; font.pixelSize: 38
                    }
                    MouseArea { anchors.fill: parent; onClicked: { dialog.visible = false; button.modelData.action() } }
                }
            }
        }
    }
}
