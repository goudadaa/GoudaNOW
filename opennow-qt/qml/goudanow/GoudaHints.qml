import QtQuick

// Right-aligned controller hints: [{glyph: "A", label: "OK"}, ...]
Row {
    id: hints
    property var model: []
    spacing: 44
    Repeater {
        model: hints.model
        Row {
            required property var modelData
            spacing: 12
            Rectangle {
                width: 40; height: 40; radius: 20
                color: GoudaTheme.primaryText
                anchors.verticalCenter: parent.verticalCenter
                Text {
                    anchors.centerIn: parent
                    text: modelData.glyph
                    color: GoudaTheme.background
                    font.family: GoudaTheme.font; font.pixelSize: 22; font.weight: Font.Black
                }
            }
            Text {
                anchors.verticalCenter: parent.verticalCenter
                text: modelData.label
                color: GoudaTheme.primaryText
                font.family: GoudaTheme.font; font.pixelSize: 28
            }
        }
    }
}
