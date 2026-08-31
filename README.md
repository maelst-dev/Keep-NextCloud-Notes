# Keep NC (Nextcloud Notes Android Client)

Android client for **Nextcloud Notes**, with a UI inspired by Google Keep (card grid layout, Material 3 theming, categories, interactive checklists, offline-first sync).

---

## 📱 Features

- **Google Keep-like UI**: Staggered grid card layout with Material 3 dynamic theming.
- **Offline-First**: Local Room database acts as the single source of truth. Writes are instantaneous and synchronized bidirectionally with Nextcloud Notes server.
- **Rich Markdown Support**:
  - Interactive checklists (`- [ ]` / `- [x]`) with touch toggle.
  - Strikethrough, tables, links, soft line breaks.
  - Plain text ↔ Checklist conversion.
- **Categories**: Organize notes into categories with a dedicated bottom sheet picker.
- **Favorites & Search**: Pin important notes and search quickly across titles and content.

---

## 🛠 Tech Stack

- **Language**: Kotlin 2.1.0
- **UI**: XML Views + RecyclerView + StaggeredGridLayoutManager + Material 3
- **Architecture**: MVVM (ViewModel + StateFlow) + Single Activity
- **Database**: Room 2.7.1
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0 (Nextcloud Notes API v1 + Login Flow v2)
- **Dependency Injection**: Hilt 2.56.2
- **Markdown**: Markwon 4.6.2
- **Min SDK**: 28 (Android 9.0) | **Target SDK**: 35

---

## 🚀 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/<your-username>/<your-repo-name>.git
   ```

2. **Open in Android Studio**:
   - Open Android Studio (Ladybug / Meerkat or newer).
   - Select **Open** and choose the cloned folder.
   - Wait for Gradle sync to complete.

3. **Build & Run**:
   - Select an emulator or connected device and press **Run** (`Shift + F10`).
