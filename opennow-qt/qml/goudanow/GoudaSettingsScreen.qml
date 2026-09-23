import QtQuick
import OpenNOW

// GoudaNOW "System Settings" — same layout as the Android TV
// `TvSettingsScreen`: categories on the left (moving over one switches the
// panel), options on the right. Pressing A on an option steps to its next
// value through the same ShellStore setters the stock settings screen uses.
FocusScope {
    id: root
    objectName: "goudaSettingsScreen"

    readonly property var categories: [
        { id: "user", label: qsTr("User") },
        { id: "streaming", label: qsTr("Streaming") },
        { id: "controllers", label: qsTr("Controllers") },
        { id: "system", label: qsTr("System") },
        { id: "all", label: qsTr("All Settings") }
    ]
    property string category: "user"
    Component.onCompleted: {
        // Set once (not bound) so remembering the category can't feed back into it.
        const saved = String(ShellStore.focusIndex("gouda-settings-category"))
        root.category = root.categories.some(c => c.id === saved) ? saved : "user"
        Qt.callLater(root.focusCategory)
    }
    onCategoryChanged: ShellStore.rememberFocus("gouda-settings-category", category)

    readonly property var settings: ShellStore.settings || ({})
    readonly property var user: ShellStore.authSession ? ShellStore.authSession.user : null

    function next(list, current) {
        const index = list.indexOf(current)
        return list[(index + 1) % list.length]
    }
    function tierLabel(tier) {
        return String(tier || "").toLowerCase().split(/[_ ]/).filter(p => p.length)
            .map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(" ")
    }

    readonly property var options: {
        const s = root.settings
        if (root.category === "user") {
            const tier = ShellStore.subscription && ShellStore.subscription.membershipTier
                ? ShellStore.subscription.membershipTier : (root.user ? root.user.membershipTier : "")
            return [
                { section: qsTr("Signed in") },
                { label: root.user ? root.user.displayName : qsTr("Not signed in"), value: root.tierLabel(tier), action: () => AppController.navigate("settings-account") },
                { label: qsTr("Switch user"), action: () => AppController.navigate("accounts") },
                { section: qsTr("Account") },
                { label: qsTr("Sign out"), danger: true, action: () => signOutDialog.open() }
            ]
        }
        if (root.category === "streaming") {
            const resolutions = ["1280x720", "1280x800", "1920x1080", "1920x1200", "2560x1440", "3840x2160"]
            const resolution = String(s.resolution || "1920x1080")
            const entitled = ShellStore.entitledFpsForResolution(resolution)
            const fpsValues = entitled && entitled.length ? entitled : ShellStore.canonicalFpsValues()
            const bitrates = [15, 25, 35, 50, 75, 100, 150]
            const codecs = ["auto", "av1", "h264", "h265"]
            const codecLabels = { auto: qsTr("Auto"), av1: "AV1", h264: "H.264", h265: "H.265 (HEVC)" }
            const hdrAvailable = HdrOutput.supported && ShellStore.hdrDecoderAvailable()
            return [
                { section: qsTr("Quality") },
                { label: qsTr("Resolution"), value: resolution.replace("x", " × "), action: () => {
                    ShellStore.setSetting("resolution", root.next(resolutions, resolution))
                    ShellStore.clampFpsToEntitlement()
                } },
                { label: qsTr("Frame rate"), value: Number(s.fps || 60) + " fps", action: () =>
                    ShellStore.setSetting("fps", root.next(fpsValues, Number(s.fps || 60))) },
                { label: qsTr("Maximum bitrate"), value: Number(s.maxBitrateMbps || 75) + " Mbps", action: () =>
                    ShellStore.setSetting("maxBitrateMbps", root.next(bitrates, Number(s.maxBitrateMbps || 75))) },
                { label: qsTr("Video codec"), value: codecLabels[String(s.codec || "auto")] || String(s.codec), action: () =>
                    ShellStore.setSetting("codec", root.next(codecs, String(s.codec || "auto"))) },
                { label: qsTr("HDR"), value: !hdrAvailable ? qsTr("Not supported") : (s.enableHdr ? qsTr("On") : qsTr("Off")), action: () => {
                    if (hdrAvailable) ShellStore.setSetting("enableHdr", !s.enableHdr)
                } },
                { section: qsTr("More") },
                { label: qsTr("All streaming settings"), action: () => AppController.navigate("settings-video") }
            ]
        }
        if (root.category === "controllers") {
            const vibration = Number(s.controllerVibrationIntensity ?? 100)
            const deadzone = Number(s.controllerLeftStickDeadzone ?? 5)
            return [
                { section: qsTr("Controllers") },
                { label: qsTr("Vibration"), value: vibration === 0 ? qsTr("Off") : vibration + "%", action: () =>
                    ShellStore.setSetting("controllerVibrationIntensity", root.next([0, 25, 50, 75, 100], vibration)) },
                { label: qsTr("Stick dead zone"), value: deadzone + "%", action: () => {
                    const value = root.next([0, 5, 10, 15, 20], deadzone)
                    ShellStore.setSetting("controllerLeftStickDeadzone", value)
                    ShellStore.setSetting("controllerRightStickDeadzone", value)
                } },
                { section: qsTr("Buttons and shortcuts") },
                { label: qsTr("All controller settings"), action: () => AppController.navigate("settings-input") }
            ]
        }
        if (root.category === "system") {
            return [
                { section: qsTr("System") },
                { label: qsTr("Version"), value: "GoudaNOW " + Qt.application.version, action: () => {} },
                { label: qsTr("Network test"), action: () => AppController.navigate("settings-network") },
                { section: qsTr("About") },
                { label: qsTr("Based on OpenNOW by OpenCloudGaming"), value: qsTr("MIT licence"), action: () => {} },
                { label: qsTr("Not affiliated with NVIDIA or Nintendo"), action: () => {} }
            ]
        }
        return [
            { section: qsTr("All Settings") },
            { label: qsTr("Open all settings"), action: () => AppController.navigate("settings-streaming") }
        ]
    }

    function focusCategory() {
        const index = Math.max(0, root.categories.findIndex(c => c.id === root.category))
        categoryList.currentIndex = index
        categoryList.forceActiveFocus()
    }

    Keys.onPressed: event => {
        if (event.key === Qt.Key_X || event.key === Qt.Key_Y) {
            event.accepted = true
        } else if ((event.key === Qt.Key_Escape || event.key === Qt.Key_Back) && optionList.activeFocus) {
            // B in the options panel returns to the category list, as on the console.
            root.focusCategory()
            event.accepted = true
        }
    }

    GoudaFrame {
        anchors.fill: parent
        title: qsTr("System Settings")
        icon: GoudaTheme.icon("settings", false)
        hints: [{ glyph: "B", label: qsTr("Back") }, { glyph: "A", label: qsTr("OK") }]

        ListView {
            id: categoryList
            x: 48; y: 32
            width: 600; height: parent.height - 64
            spacing: 4
            model: root.categories
            keyNavigationWraps: false
            KeyNavigation.right: optionList
            onCurrentIndexChanged: if (activeFocus && currentIndex >= 0 && root.categories[currentIndex].id !== "all")
                root.category = root.categories[currentIndex].id
            delegate: GoudaListRow {
                required property var modelData
                required property int index
                width: categoryList.width
                label: modelData.label
                selected: modelData.id === root.category
                focus: ListView.isCurrentItem
                onClicked: {
                    if (modelData.id === "all")
                        AppController.navigate("settings-streaming")
                    else {
                        root.category = modelData.id
                        optionList.focusFirst()
                    }
                }
            }
        }
        Rectangle { x: 680; y: 32; width: 2; height: parent.height - 64; color: GoudaTheme.divider }
        ListView {
            id: optionList
            x: 720; y: 32
            width: parent.width - x - 80; height: parent.height - 64
            spacing: 4
            clip: true
            model: root.options
            keyNavigationWraps: false
            KeyNavigation.left: categoryList
            function focusFirst() {
                for (let i = 0; i < root.options.length; ++i) {
                    if (!root.options[i].section) { currentIndex = i; break }
                }
                forceActiveFocus()
            }
            // Section captions are not focusable: skip over them.
            Keys.onUpPressed: event => {
                let i = currentIndex - 1
                while (i >= 0 && root.options[i].section) --i
                if (i >= 0) currentIndex = i
            }
            Keys.onDownPressed: event => {
                let i = currentIndex + 1
                while (i < root.options.length && root.options[i].section) ++i
                if (i < root.options.length) currentIndex = i
            }
            delegate: Loader {
                id: optionDelegate
                required property var modelData
                required property int index
                width: optionList.width
                focus: ListView.isCurrentItem
                sourceComponent: modelData.section ? sectionComponent : rowComponent
                Component {
                    id: sectionComponent
                    GoudaSectionLabel { text: optionDelegate.modelData.section }
                }
                Component {
                    id: rowComponent
                    GoudaListRow {
                        width: optionList.width
                        label: optionDelegate.modelData.label
                        value: optionDelegate.modelData.value || ""
                        labelColor: optionDelegate.modelData.danger ? GoudaTheme.error : GoudaTheme.primaryText
                        focus: true
                        onClicked: {
                            // Changing a value rebuilds the option list; keep the highlight on
                            // the row that was pressed instead of jumping back to the top.
                            const keep = optionDelegate.index
                            optionDelegate.modelData.action()
                            Qt.callLater(() => {
                                if (!signOutDialog.visible && AppController.route === "settings") {
                                    optionList.currentIndex = keep
                                    optionList.forceActiveFocus()
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    GoudaDialog {
        id: signOutDialog
        title: qsTr("Sign out?")
        message: qsTr("You'll need to sign in again with a code to play.")
        actions: [
            { label: qsTr("Cancel"), action: () => optionList.forceActiveFocus() },
            { label: qsTr("Sign out"), action: () => ShellStore.logout() }
        ]
        onDismissed: optionList.forceActiveFocus()
    }
}
