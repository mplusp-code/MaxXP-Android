# Relay — Agent Architecture & Workspace Invariants

Welcome to the **Relay** codebase. Relay is a unified, multi-messenger client for Android foldables and phones featuring an iconic classic PDA / Windows Mobile 6.5 skeuomorphic design system.

All agents, subagents, and contributors working on Relay MUST strictly observe the following core architectural invariants:

---

## 1. No WebView Reloads on Fold/Unfold
- Active `WebView` instances **MUST** be retained in a persistent, keyed session pool/holder outside transient Composable lifecycles.
- Folding, unfolding, screen rotations, swapping panes in Dual-Chat Split View, or navigating between tabs must NEVER trigger page reloads for active sessions (such as `web.whatsapp.com`, `web.telegram.org`, `instagram.com/direct`, `messenger.com`).
- Always preserve DOM state, authentication sessions, and live WebSocket connections across layout transitions.

---

## 2. Multi-Account Isolation
- Always leverage `androidx.webkit:webkit` (`ProfileStore` or distinct data directory suffixes per account).
- Multi-accounts on identical platforms (e.g., `WhatsApp · Personal` and `WhatsApp · Work`, `Instagram · studio` and `Instagram · private`) must maintain completely isolated cookies, Web Storage (`localStorage`, `sessionStorage`), IndexedDB, and Cache.
- No session or credential bleeding between accounts is permitted.

---

## 3. Mandatory Chrome Architecture
Every primary screen in Relay must strictly incorporate the complete chrome structure:
1. **Glossy Top Title Bar (`RelayTopBar`):**
   - 4-square Relay brand logo icon on the left.
   - Screen title (e.g., `Relay`, `Inbox`, `Relay · Split view`, `WhatsApp · Personal`, `Relay · Setup`).
   - Right-side contextual actions or status badges (e.g., `ok`, layout toggles, reload).
2. **Adaptive Navigation Suite:**
   - **Compact / Portrait mode (Cover screen / standard phone):** Fixed 4-tab bottom navigation bar (`Today`, `Inbox`, `Accounts`, `Setup`).
   - **Expanded / Unfolded mode (Foldable inner display / tablet):** Left-docked vertical Navigation Rail with identical 4 destinations (`Today`, `Inbox`, `Accounts`, `Setup`).
3. **Bevelled Softkey Bar (`RelaySoftkeyBar`):**
   - The bottom-most bar consisting of 3 distinct tactile slots:
     - **Left slot:** Contextual primary action (e.g., `New`, `Open`, `Account`, `Add`, `Done`).
     - **Center slot:** Retro PDA physical keyboard icon toggle button.
     - **Right slot:** Contextual secondary action (e.g., `Menu`, `Swap panes`).

---

## 4. Low-RAM Backend Compatibility
- All background synchronization and state interfaces must remain ultra-lightweight (REST/SSE JSON state repositories designed to run seamlessly against a 1 GB RAM Matrix Conduit + Mautrix bridge server or headless bridge).
- Background unread counters, notification mirrors, and "Latest · reply without opening" quick reply workflows must function without waking hibernated or backgrounded `WebView` processes.

---

## 5. Terminal & Execution Invariants
- **Remote Git & Network Commands:** Network operations (`git push`, `git pull`, `git fetch`, `git clone`, `curl/ssh` to Oracle VM) cannot run inside the offline sandbox. Agents must execute them directly with network bypass (`BypassSandbox: true`) using clean, unchained command shapes (e.g. `git push origin main`) to prevent DNS resolution failures (`fatal: Could not resolve host: github.com` / Exit code 128) and ensure clean prefix-matching for user auto-approval.
- **Local Gradle Builds:** Standard builds and tests (`./gradlew testDebugUnitTest`, `./gradlew assembleDebug`) use the auto-detecting Java wrapper in [`gradlew`](file:///Users/m_pc/Desktop/Antigravity/Max_XP/gradlew) and [`gradle.properties`](file:///Users/m_pc/Desktop/Antigravity/Max_XP/gradle.properties). When running Gradle tasks that spawn or connect to the Gradle Daemon, execute with elevated permissions so IPC loopback sockets (127.0.0.1) are accessible.
