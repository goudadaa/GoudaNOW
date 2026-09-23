import QtQuick
import OpenNOW

// GoudaNOW sign-in — same flow as the Android TV `TvSignInScreen`: one
// "Sign in" choice that starts the existing NVIDIA device-code login, then the
// code, the web address and a QR code in large type.
FocusScope {
    id: root
    objectName: "goudaSignInScreen"
    property double clockMs: Date.now()
    readonly property var challenge: ShellStore.authChallenge
    readonly property var qrRows: challenge && challenge.qrRows ? challenge.qrRows : []
    readonly property int qrSize: qrRows.length
    readonly property int secondsLeft: challenge ? Math.max(0, Math.ceil((Number(challenge.expiresAt) - clockMs) / 1000)) : 0
    readonly property bool busy: ShellStore.authState === "starting" || ShellStore.authState === "completing"
        || !ShellStore.ready || ShellStore.authRestorePending === true

    Timer { interval: 1000; repeat: true; running: root.challenge !== null; onTriggered: root.clockMs = Date.now() }

    onChallengeChanged: Qt.callLater(root.focusDefault)
    Component.onCompleted: Qt.callLater(root.focusDefault)
    function focusDefault() {
        if (root.challenge) cancelRow.forceActiveFocus()
        else if (ShellStore.signedIn) continueRow.forceActiveFocus()
        else signInRow.forceActiveFocus()
    }

    Keys.onPressed: event => {
        if ((event.key === Qt.Key_Escape || event.key === Qt.Key_Back) && root.challenge) {
            ShellStore.cancelDeviceLogin()
            event.accepted = true
        } else if (event.key === Qt.Key_X || event.key === Qt.Key_Y) {
            event.accepted = true
        }
    }

    GoudaFrame {
        anchors.fill: parent
        title: root.challenge ? qsTr("Link your NVIDIA account") : "GoudaNOW"
        hints: root.challenge ? [{ glyph: "B", label: qsTr("Cancel") }] : [{ glyph: "A", label: qsTr("OK") }]

        // ------------------------------------------------ start / waiting
        Column {
            anchors.centerIn: parent
            width: 1120
            spacing: 20
            visible: !root.challenge
            Image {
                anchors.horizontalCenter: parent.horizontalCenter
                source: GoudaTheme.logo
                width: 440; height: 238
                fillMode: Image.PreserveAspectFit
            }
            Text {
                width: parent.width
                horizontalAlignment: Text.AlignHCenter
                wrapMode: Text.WordWrap
                text: ShellStore.signedIn ? qsTr("Signed in as %1.").arg(ShellStore.authSession.user.displayName)
                    : root.busy ? (ShellStore.authMessage || qsTr("Getting ready…"))
                    : qsTr("Sign in with your NVIDIA account to see your games.")
                color: GoudaTheme.primaryText
                font.family: GoudaTheme.font; font.pixelSize: 44
            }
            Text {
                width: parent.width
                visible: !ShellStore.signedIn && !root.busy
                horizontalAlignment: Text.AlignHCenter
                text: qsTr("You'll get a code to enter on your phone or computer.")
                color: GoudaTheme.secondaryText
                font.family: GoudaTheme.font; font.pixelSize: 36
            }
            Text {
                width: parent.width
                visible: ShellStore.authState === "error"
                horizontalAlignment: Text.AlignHCenter
                wrapMode: Text.WordWrap
                text: ShellStore.authMessage
                color: GoudaTheme.error
                font.family: GoudaTheme.font; font.pixelSize: 32
            }
            Item { width: 1; height: 24 }
            GoudaListRow {
                id: signInRow
                visible: !ShellStore.signedIn
                width: parent.width
                label: root.busy ? qsTr("Please wait…") : qsTr("Sign in")
                KeyNavigation.down: otherRow
                onClicked: if (!root.busy) ShellStore.startDeviceLogin("")
            }
            GoudaListRow {
                id: otherRow
                visible: !ShellStore.signedIn && ShellStore.providers.length > 1
                width: parent.width
                label: ShellStore.providers.length > 1
                    ? qsTr("Sign in with %1").arg(ShellStore.providers[1].displayName) : ""
                labelColor: GoudaTheme.secondaryText
                KeyNavigation.up: signInRow
                onClicked: if (!root.busy) ShellStore.startDeviceLogin(ShellStore.providers[1].idpId)
            }
            GoudaListRow {
                id: continueRow
                visible: ShellStore.signedIn
                width: parent.width
                label: qsTr("Go to the HOME menu")
                onClicked: AppController.navigate("home")
            }
        }

        // ------------------------------------------------ code + QR
        Row {
            anchors.centerIn: parent
            spacing: 112
            visible: root.challenge !== null
            Column {
                width: 1000
                spacing: 16
                anchors.verticalCenter: parent.verticalCenter
                Repeater {
                    model: [
                        { n: "1", t: qsTr("On your phone or computer, go to:") },
                        { n: "2", t: qsTr("Enter this code:") }
                    ]
                    Column {
                        required property var modelData
                        required property int index
                        spacing: 16
                        Row {
                            spacing: 28
                            Rectangle {
                                width: 60; height: 60; radius: 30; color: GoudaTheme.focusCyan
                                Text { anchors.centerIn: parent; text: modelData.n; color: GoudaTheme.background; font.family: GoudaTheme.font; font.pixelSize: 32; font.weight: Font.Bold }
                            }
                            Text { anchors.verticalCenter: parent.verticalCenter; text: modelData.t; color: GoudaTheme.primaryText; font.family: GoudaTheme.font; font.pixelSize: 40 }
                        }
                        Text {
                            visible: index === 0
                            leftPadding: 88; bottomPadding: 36
                            text: root.challenge ? String(root.challenge.verificationUri).replace(/^https?:\/\//, "") : ""
                            color: GoudaTheme.focusCyan
                            font.family: GoudaTheme.font; font.pixelSize: 48; font.weight: Font.Medium
                        }
                        Item {
                            visible: index === 1
                            x: 88
                            width: codeText.width + 96; height: codeText.height + 40
                            Rectangle { anchors.fill: parent; radius: 12; color: GoudaTheme.rowFocused }
                            Text {
                                id: codeText
                                anchors.centerIn: parent
                                text: root.challenge ? root.challenge.userCode : ""
                                color: GoudaTheme.primaryText
                                font.family: Theme.monoFont; font.pixelSize: 88; font.weight: Font.Bold
                                font.letterSpacing: 12
                            }
                        }
                    }
                }
                Text {
                    width: parent.width
                    wrapMode: Text.WordWrap
                    leftPadding: 88; topPadding: 36
                    text: qsTr("This screen moves on by itself once you've signed in. Code expires in %1:%2.")
                        .arg(Math.floor(root.secondsLeft / 60)).arg(String(root.secondsLeft % 60).padStart(2, "0"))
                    color: GoudaTheme.secondaryText
                    font.family: GoudaTheme.font; font.pixelSize: 32
                }
                Item { width: 1; height: 24 }
                GoudaListRow {
                    id: cancelRow
                    x: 88
                    width: 480
                    label: qsTr("Cancel")
                    onClicked: ShellStore.cancelDeviceLogin()
                }
            }
            Column {
                anchors.verticalCenter: parent.verticalCenter
                spacing: 20
                visible: root.qrSize > 0
                Rectangle {
                    width: 460; height: 460; radius: 16; color: "#FFFFFF"
                    Grid {
                        id: qrGrid
                        anchors.centerIn: parent
                        columns: root.qrSize
                        property real cellSize: root.qrSize > 0 ? Math.floor(420 / root.qrSize) : 0
                        Repeater {
                            model: root.qrSize * root.qrSize
                            Rectangle {
                                required property int index
                                width: qrGrid.cellSize; height: qrGrid.cellSize
                                color: root.qrRows[Math.floor(index / root.qrSize)].charAt(index % root.qrSize) === "1" ? "#000000" : "#FFFFFF"
                            }
                        }
                    }
                }
                Text {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: qsTr("Or scan with your phone")
                    color: GoudaTheme.secondaryText
                    font.family: GoudaTheme.font; font.pixelSize: 32
                }
            }
        }
    }
}
