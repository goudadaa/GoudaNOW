# GoudaNOW on Steam Deck

GoudaNOW for Linux is the OpenNOW Qt desktop client with the GoudaNOW HOME menu,
packaged as a Flatpak (app id `com.goudanow.goudagames`). These steps build it on
GitHub and install it on a Steam Deck.

## 1. Build the Flatpak on GitHub

1. Commit the `linux/switch-home` branch and push it to your GitHub fork.
2. Pushing that branch starts **Actions → GoudaNOW Flatpak (Steam Deck)**. You can
   also start it by hand with **Run workflow**.
3. The first build takes a long time (the Rust core, the native streamer and FFmpeg
   are compiled from source). When it finishes, open the run and download the
   **goudanow-flatpak-x86_64** artifact. Unzip it to get `GoudaNOW-x86_64.flatpak`.

## 2. Install on the Steam Deck

1. Hold the power button and choose **Switch to Desktop**.
2. Copy `GoudaNOW-x86_64.flatpak` to the Deck (USB stick, network share, or download
   it on the Deck in a browser).
3. Open **Konsole** in the folder with the file and run:

   ```
   flatpak install --user GoudaNOW-x86_64.flatpak
   ```

   Flathub is already set up on SteamOS, so the KDE runtime downloads automatically.
4. Check it starts: `flatpak run com.goudanow.goudagames`.

## 3. Add it to Gaming Mode

1. Open **Steam** in Desktop Mode → **Games → Add a Non-Steam Game to My Library**.
2. Tick **GoudaNOW** and choose **Add Selected Programs**.
3. Return to Gaming Mode. GoudaNOW is in **Library → Non-Steam**.
4. Optional artwork: in the game's **Properties** you can set the icon to
   `opennow-qt/packaging/icons/opennow-512.png` from this repo.

## 4. Controller

- In GoudaNOW's controller settings (Steam button → controller icon), use the
  **Gamepad** template. The Deck then acts like an Xbox controller: A selects,
  B goes back, X searches, Y sorts in All Games.
- The Steam button is kept by Steam, so it can't open GoudaNOW's in-game menu.
  In the same controller settings, map a back grip (for example **L4**) to the
  keyboard shortcut **Ctrl + G**. That opens the stream menu during a game.

## 5. First run

Choose **Sign in** and enter the code on your phone or computer, or scan the QR code.
The streaming resolution list includes **1280 × 800**, the Deck's own screen size.

## Notes

- Updates: GoudaNOW has no update feed. Install a newer build the same way
  (`flatpak install --user` replaces the old one).
- Feedback, bug reports and anonymous error reports are switched off, because they
  would go to OpenNOW's maintainers.
- Based on OpenNOW by OpenCloudGaming (MIT licence). Not affiliated with NVIDIA,
  Valve or Nintendo.
