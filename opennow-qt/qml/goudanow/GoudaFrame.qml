import QtQuick

// Switch system-screen frame: icon + title header, divider, body, divider,
// button hints. Children go into the body.
FocusScope {
    id: frame
    property string title: ""
    property url icon: ""
    property var hints: []
    property alias headerCenter: centerSlot.data
    property alias headerTrailing: trailingSlot.data
    default property alias body: bodySlot.data

    Rectangle { anchors.fill: parent; color: GoudaTheme.background }

    Item {
        id: header
        x: GoudaTheme.sidePadding; y: 24
        width: parent.width - 2 * GoudaTheme.sidePadding
        height: 112
        Row {
            id: titleRow
            anchors.verticalCenter: parent.verticalCenter
            spacing: 28
            Image {
                visible: frame.icon != ""
                source: frame.icon
                width: 56; height: 56
                sourceSize: Qt.size(56, 56)
                anchors.verticalCenter: parent.verticalCenter
            }
            Text {
                text: frame.title
                color: GoudaTheme.primaryText
                font.family: GoudaTheme.font; font.pixelSize: 50; font.weight: Font.DemiBold
                anchors.verticalCenter: parent.verticalCenter
            }
        }
        Rectangle {
            id: centerDivider
            visible: centerSlot.children.length > 0
            x: titleRow.width + 40
            anchors.verticalCenter: parent.verticalCenter
            width: 2; height: 52
            color: GoudaTheme.divider
        }
        Item {
            id: centerSlot
            x: centerDivider.x + 40
            width: Math.max(0, trailingSlot.x - x - 48)
            height: parent.height
            clip: true
        }
        Row {
            id: trailingSlot
            anchors.right: parent.right
            anchors.verticalCenter: parent.verticalCenter
            spacing: 24
        }
    }
    Rectangle { id: topRule; x: 48; y: header.y + header.height; width: parent.width - 96; height: 2; color: GoudaTheme.divider }
    Item {
        id: bodySlot
        x: 0
        y: topRule.y + 2
        width: parent.width
        height: bottomRule.y - y
    }
    Rectangle { id: bottomRule; x: 48; y: parent.height - 108; width: parent.width - 96; height: 2; color: GoudaTheme.divider }
    GoudaHints {
        model: frame.hints
        anchors.right: parent.right
        anchors.rightMargin: GoudaTheme.sidePadding
        y: bottomRule.y + 32
    }
}
