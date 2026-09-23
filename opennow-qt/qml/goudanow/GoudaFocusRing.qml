import QtQuick

// Cyan focus border with a soft outer glow, drawn around its parent.
Item {
    id: ring
    property bool shown: false
    property real radius: 6
    property real thickness: 6
    property real glow: 14
    anchors.fill: parent
    visible: opacity > 0
    opacity: shown ? 1 : 0
    Behavior on opacity { NumberAnimation { duration: GoudaTheme.animMs } }
    Repeater {
        model: 3
        Rectangle {
            required property int index
            anchors.fill: parent
            anchors.margins: -(ring.thickness + (index + 1) * ring.glow / 3)
            radius: ring.radius + (index + 1) * ring.glow / 3
            color: "transparent"
            border.width: ring.glow / 3
            border.color: Qt.rgba(0, 0.76, 0.89, 0.22 - index * 0.06)
        }
    }
    Rectangle {
        anchors.fill: parent
        anchors.margins: -ring.thickness
        radius: ring.radius + ring.thickness / 2
        color: "transparent"
        border.width: ring.thickness
        border.color: GoudaTheme.focusCyan
    }
}
