import QtQuick

// One line of text that scrolls slowly when it is too long to fit, so the
// whole title can be read. Restarts from the beginning when the text changes.
Item {
    id: marquee
    property string text: ""
    property color color: GoudaTheme.focusCyan
    property int pixelSize: 42
    clip: true
    height: label.implicitHeight
    readonly property real overflow: Math.max(0, label.implicitWidth - width)

    Text {
        id: label
        anchors.verticalCenter: parent.verticalCenter
        text: marquee.text
        color: marquee.color
        font.family: GoudaTheme.font; font.pixelSize: marquee.pixelSize; font.weight: Font.Medium
    }
    onTextChanged: restart()
    onWidthChanged: restart()
    function restart() {
        scroll.stop()
        label.x = 0
        if (marquee.overflow > 0 && !AppController.reducedMotion)
            scroll.start()
    }
    SequentialAnimation {
        id: scroll
        loops: Animation.Infinite
        PauseAnimation { duration: 1200 }
        NumberAnimation { target: label; property: "x"; to: -marquee.overflow; duration: Math.max(1500, marquee.overflow * 18) }
        PauseAnimation { duration: 1400 }
        PropertyAction { target: label; property: "x"; value: 0 }
    }
}
