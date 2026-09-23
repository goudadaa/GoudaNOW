import QtQuick

// Rounded header button (Search / Sort) with the cyan focus ring.
FocusScope {
    id: pill
    property string label: ""
    property string iconName: ""
    signal clicked()
    width: content.width + 64
    height: 76
    Keys.onPressed: event => {
        if ((event.key === Qt.Key_Return || event.key === Qt.Key_Enter) && !event.isAutoRepeat) {
            pill.clicked()
            event.accepted = true
        }
    }
    Rectangle {
        anchors.fill: parent; radius: height / 2
        color: pill.activeFocus ? GoudaTheme.navButtonFocused : GoudaTheme.navButton
        GoudaFocusRing { shown: pill.activeFocus; radius: pill.height / 2; thickness: 5; glow: 0 }
    }
    Row {
        id: content
        anchors.centerIn: parent
        spacing: 14
        Image {
            visible: pill.iconName !== ""
            source: pill.iconName !== "" ? GoudaTheme.icon(pill.iconName, pill.activeFocus) : ""
            width: 38; height: 38; sourceSize: Qt.size(38, 38)
            anchors.verticalCenter: parent.verticalCenter
        }
        Text {
            text: pill.label
            color: pill.activeFocus ? GoudaTheme.focusCyan : GoudaTheme.primaryText
            font.family: GoudaTheme.font; font.pixelSize: 30
            anchors.verticalCenter: parent.verticalCenter
        }
    }
    MouseArea { anchors.fill: parent; onClicked: { pill.forceActiveFocus(); pill.clicked() } }
}
