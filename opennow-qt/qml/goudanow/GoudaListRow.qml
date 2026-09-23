import QtQuick

// Focusable list row with the cyan focus ring and a right-aligned value, like
// a System Settings option. A (Return/Enter) or a click activates it.
FocusScope {
    id: row
    property string label: ""
    property string value: ""
    property bool selected: false
    property color labelColor: GoudaTheme.primaryText
    signal clicked()

    width: parent ? parent.width : 600
    height: Math.max(104, labelText.implicitHeight + 40)
    activeFocusOnTab: true

    Keys.onPressed: event => {
        if ((event.key === Qt.Key_Return || event.key === Qt.Key_Enter) && !event.isAutoRepeat) {
            row.clicked()
            event.accepted = true
        }
    }

    Rectangle {
        anchors.fill: parent
        radius: 6
        color: row.activeFocus ? GoudaTheme.rowFocused : "transparent"
        GoudaFocusRing { shown: row.activeFocus; thickness: 5; glow: 0 }
    }
    Rectangle {
        visible: row.selected
        x: 36; anchors.verticalCenter: parent.verticalCenter
        width: 8; height: 52; color: GoudaTheme.focusCyan
    }
    Text {
        id: labelText
        x: row.selected ? 72 : 40
        anchors.verticalCenter: parent.verticalCenter
        width: parent.width - x - valueText.width - 72
        text: row.label
        color: row.selected ? GoudaTheme.focusCyan : row.labelColor
        font.family: GoudaTheme.font; font.pixelSize: 36
        elide: Text.ElideRight
    }
    Text {
        id: valueText
        anchors.right: parent.right; anchors.rightMargin: 40
        anchors.verticalCenter: parent.verticalCenter
        text: row.value
        color: GoudaTheme.focusCyan
        font.family: GoudaTheme.font; font.pixelSize: 34
    }
    MouseArea { anchors.fill: parent; onClicked: { row.forceActiveFocus(); row.clicked() } }
}
