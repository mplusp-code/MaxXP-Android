---
name: foldable-posture-engine
description: Manages foldable posture transitions (Cover Portrait, Unfolded Dual-Pane, Unfolded Tri-Pane Workspace, Dual-Chat Split View, and Half-Folded Flex Mode) using Jetpack WindowManager and WindowSizeClass. Use when implementing navigation or screen layouts.
---

# Foldable Posture Engine for Relay

This skill outlines how to build and adapt Relay's user interface across foldable postures (Cover Screen, Unfolded Inner Display, Split Views, and Tabletop / Half-folded Flex Mode) using AndroidX WindowManager and Compose Material3 Adaptive.

---

## 1. Posture & WindowSizeClass Mappings

### A. Compact Mode (`WindowWidthSizeClass.Compact`) — Cover Screen / Standard Phone
- **Layouts:** 1-Pane Fullscreen (PDF Pages 1–5).
- **Navigation:** Bottom Navigation Bar with 4 destinations:
  - `Today` (Unread count overview, service badges with counts, quick action bar).
  - `Inbox` (Unified chronological message feed with Service / Sort dropdown filters).
  - `Accounts` (List of accounts with connection badges and Notify toggles).
  - `Setup` (Wallpaper picker, Text size slider, Alerts options).
- **Softkey Bar:** Fixed at screen bottom (`New` / Keyboard / `Menu`, etc.).

### B. Medium & Expanded Modes (`WindowWidthSizeClass.Medium` & `Expanded`) — Unfolded Inner Display
- **Navigation:** Left Vertical Navigation Rail (fixed on left side, showing 4 tab icons).
- **Layouts by Tab (PDF Pages 6–10):**
  1. **Today (Page 6):** 2-Pane Layout
     - Left Pane (~45%): Service list & unread counts.
     - Right Pane (~55%): "Latest · reply without opening" quick inline reply cards & session health list (`Relink` / `Refresh`).
  2. **Inbox — Workspace Mode (Page 7):** 3-Pane Workspace
     - Left Pane (~20%): Messengers filter list (`All inbox`, `WhatsApp`, `Telegram`, etc.).
     - Middle Pane (~35%): Unified conversation feed for the selected messenger.
     - Right Pane (~45%): Active Chat `WebView` with top URL bar and bottom service bar.
  3. **Inbox — Split View Mode (Page 8):** Dual-Chat Split View
     - Two active chat WebViews side-by-side (50% / 50%).
     - Top bar includes swap icon and close buttons.
     - Softkey bar features `New`, `Swap panes`, and `Menu`.
  4. **Accounts (Page 9):** 2-Pane Master-Detail
     - Left Pane (~45%): Accounts list with unread/status badges and notify switches.
     - Right Pane (~55%): Account detail card with live QR Code relink viewport, `Relink` / `Remove account` buttons, and toggles (`Show unread count on Today`, `Keep session alive`).
  5. **Setup (Page 10):** 2-Pane Configuration
     - Left Pane: Wallpaper theme radio cards, Text size slider, Alerts toggles with Quiet hours time pickers.
     - Right Pane: "When unfolded, open" radio choices (`Workspace`, `Split view`, `Today · quick replies`, `Last used`), "Folding and unfolding" rules (`Keep open chats`, `Open chat on unfold`, `Use Flex mode`), and Web views preferences.

---

## 2. Jetpack WindowManager FoldingFeature Handling

### Posture Detection:
```kotlin
@Composable
fun rememberDevicePosture(windowLayoutInfo: WindowLayoutInfo): DevicePosture {
    val foldingFeature = windowLayoutInfo.displayFeatures
        .filterIsInstance<FoldingFeature>()
        .firstOrNull()

    return when {
        foldingFeature == null -> DevicePosture.Normal
        foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL ->
            DevicePosture.TableTop(foldingFeature.bounds)
        foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL ->
            DevicePosture.BookPosture(foldingFeature.bounds)
        foldingFeature.isSeparating -> DevicePosture.Separating(foldingFeature.bounds)
        else -> DevicePosture.Normal
    }
}
```

### Flex Mode Integration (Half-Folded):
- When `SetupState.useFlexModeWhenHalfFolded == true` and device is in `TableTop` posture:
  - **Upper Half (Vertical display):** Active Chat message history / WebView feed.
  - **Lower Half (Horizontal flat surface):** Retro PDA full-width on-screen soft keyboard, message input box, and media controls.
