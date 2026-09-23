pragma Singleton
import QtQuick
import OpenNOW

// GoudaNOW Basic Black palette and shared metrics. Mirrors the Android TV
// `TvSwitchColors` so both clients look the same. Sizes are console-canvas
// pixels (the console surface is a 1920-wide canvas scaled to the window).
QtObject {
    readonly property color background: "#2D2D2D"
    readonly property color tilePlaceholder: "#3A3A3A"
    readonly property color navButton: "#3C3C3C"
    readonly property color navButtonFocused: "#4A4A4A"
    readonly property color rowFocused: "#3A3A3A"
    readonly property color dialogPanel: "#3B3B3B"
    readonly property color divider: "#4B4B4B"
    readonly property color primaryText: "#FFFFFF"
    readonly property color secondaryText: "#B4B4B4"
    readonly property color focusCyan: "#00C3E3"
    readonly property color focusGlow: "#9900C3E3"
    readonly property color error: "#FF8A80"
    readonly property color scrim: "#D9000000"
    readonly property color gold: "#FFC940"
    readonly property color cream: "#FFF4DC"

    readonly property string font: Theme.bodyFont
    readonly property real boxArtRatio: 628 / 888   // GFN GAME_BOX_ART, portrait
    readonly property real focusedCardScale: 1.18
    readonly property int sidePadding: 80
    readonly property int animMs: AppController.reducedMotion ? 0 : 140

    readonly property string assetRoot: "qrc:/qt/qml/OpenNOW/res/goudanow/"
    readonly property url logo: assetRoot + "goudanow-mark.png"
    /** Monochrome icon: name is apps|controller|settings|power|search|wifi. */
    function icon(name, focused) {
        return assetRoot + name + (focused ? "-cyan.svg" : "-white.svg")
    }
    function greyIcon(name) {
        return assetRoot + name + "-grey.svg"
    }

    /** Start a game the way the Android TV client does: straight from the tile. */
    function play(game) {
        if (!game)
            return
        const variants = game.variants || []
        // More than one store copy: the stock details screen has the store picker.
        if (variants.length > 1) {
            ShellStore.openGame(game)
            return
        }
        ShellStore.selectedGame = game
        ShellStore.launchSelectedGame()
    }

    function gameId(game) {
        return game ? String(ShellStore.gameIdentity(game)) : ""
    }
}
