---
name: retro-pda-ui
description: Applies the Windows Mobile 6.5 / classic PDA skeuomorphic design system for Relay, including glossy gradient bars, 3-slot bevelled softkey bars, square glossy service badges, and organic wave wallpapers. Use whenever building or styling UI components.
---

# Retro PDA UI Design System (Windows Mobile 6.5 / Classic PDA Skeuomorphism)

This skill provides design tokens, styling rules, and reusable Jetpack Compose components to recreate the Windows Mobile 6.5 / classic handheld PDA interface for the Relay multi-messenger client.

---

## 1. Wallpaper Themes & Organic Wave Canvas
Relay supports 4 selectable ambient themes based on deep satin/gradient backgrounds layered with subtle curved vector/Canvas organic waves:

1. **Meadow Green (Default):**
   - Background Gradient: `#1B4D20` (Dark Forest Green) to `#4C8C2B` (Vibrant Meadow Green).
   - Wave Overlay: Translucent curved bezier sweeps (`#68BB35` at 15%–25% alpha, `#143818` at 30% alpha).
2. **Deep Teal:**
   - Background Gradient: `#07262E` to `#0F4C5C`.
   - Wave Overlay: Translucent teal sweeps (`#1E6F83` at 20% alpha).
3. **Classic Blue:**
   - Background Gradient: `#0D1B2A` to `#1D3557`.
   - Wave Overlay: Translucent electric blue sweeps (`#457B9D` at 20% alpha).
4. **Amber:**
   - Background Gradient: `#4A2800` to `#B5651D`.
   - Wave Overlay: Warm golden amber sweeps (`#D48B38` at 20% alpha).

### Wave Overlay Implementation
Use a custom `Modifier.drawBehind { ... }` or `Canvas` that draws 2-3 overlapping cubic bezier curves (`drawPath`) with radial/linear brush fills to mimic the Windows Mobile 6.5 satin wave background.

---

## 2. Square Glossy Service Badges
Relay represents connected messenger services using glossy rounded-square badges with high-contrast centered initials:

| Service | Badge Letter | Background Color | Highlight / Gloss Accent |
| :--- | :---: | :---: | :---: |
| **WhatsApp** | `W` | `#4CAF50` (Green) | Gradient with top 35% white overlay (alpha 0.35) |
| **Telegram** | `T` | `#2196F3` (Light Blue) | Top-to-bottom gloss shine |
| **Instagram** | `I` | `#C2185B` (Magenta/Berry) | Top-to-bottom gloss shine |
| **Facebook** | `F` | `#1976D2` (Royal Blue) | Top-to-bottom gloss shine |
| **Viber** | `V` | `#7B1FA2` (Purple) | Top-to-bottom gloss shine |
| **Google Messages** | `G` | `#FFA000` (Amber Gold) | Top-to-bottom gloss shine |

### Badge Styling Rules:
- Corner radius: `4.dp` to `6.dp` (never a full circle; always a soft square).
- Bevel border: 1px light inner border (`#FFFFFF` with 0.4 alpha) on top/left, subtle shadow (`#000000` with 0.5 alpha) on bottom/right.
- Typography: Bold Sans-serif (`FontWeight.ExtraBold`, white, subtle drop shadow).

---

## 3. Reusable UI Components

### `RelayTopBar`
- **Height:** 48.dp.
- **Background:** Skeuomorphic glossy dark green/black horizontal gradient with a 1px bottom highlight line (`#558B2F` or `#444444`).
- **Elements:**
  - Left: 4-square Relay icon (a 2x2 grid of small rounded squares in accent green/white) and Title text (`FontWeight.Bold`).
  - Right: Contextual buttons (e.g., `ok`, layout toggles, or reload button with skeuomorphic bevelled surface).

### `RelaySoftkeyBar`
- **Height:** 48.dp.
- **Placement:** Docked to the absolute bottom of the screen (below navigation bars/content).
- **Surface:** Glossy dark bar with top bevel highlight.
- **Layout:** Exactly 3 slots:
  1. **Left slot:** Primary action text button (`New`, `Open`, `Account`, `Add`, `Done`) in crisp white sans-serif bold.
  2. **Center slot:** Retro PDA physical keyboard button (stylized hardware keyboard icon with key matrix dots inside a bevelled button frame).
  3. **Right slot:** Secondary action text button (`Menu`, `Swap panes`).

### `RelaySectionHeader`
- **Style:** Compact green-tinted header bar (height ~26.dp) with bold white title (e.g., `Messengers`, `Today`, `Sessions`, `Wallpaper`, `Alerts`).
- **Divider:** Subtle 1px bevel divider line underneath.

### `RelayDropdown` & `RelayCheckbox`
- **Dropdowns:** Skeuomorphic bevelled box with dark green background, white label, and green downward triangle button.
- **Checkboxes:** Classic square inset bevel checkboxes with crisp white checkmark or filled square.
