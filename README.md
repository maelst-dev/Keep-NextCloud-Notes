# KeepNotes for Nextcloud (Android Client)

[English](#english) | [Русский](#русский)

---

<a name="русский"></a>
## 🇷🇺 Русский

**KeepNotes for Nextcloud** — Android-клиент для [Nextcloud Notes](https://apps.nextcloud.com/apps/notes) с интерфейсом и пользовательским опытом в стиле **Google Keep** (сетка заметок, тема Material 3, интерактивные чек-листы, категории, полнофункциональный офлайн-режим с двусторонней синхронизацией).

### ✨ Основные возможности

- **Интерфейс в стиле Google Keep**: адаптивная сетка заметок (Staggered Grid) с динамической темой Material 3 (светлая и тёмная темы).
- **Бесшовная авторизация**: поддержка официального **Nextcloud Login Flow v2** (вход в один клик через браузер без ввода паролей вручную).
- **Полноценный Offline-First**:
  - Локальная база данных Room выступает единым источником правды (*Single Source of Truth*).
  - Мгновенное создание, редактирование и удаление заметок без ожидания ответа сервера.
  - Двусторонняя синхронизация с разрешением конфликтов по `etag` и временным меткам.
  - Фоновая периодическая синхронизация через **WorkManager** и ручное обновление жестом *Pull-to-Refresh*.
- **Продвинутый Markdown и списки задач**:
  - **Режим редактирования (Edit)**: автодополнение списков при нажатии Enter (с сохранением отступов), быстрый выход из списка, автоматическая очистка пустых пунктов.
  - **Режим просмотра (Preview)**: крупные кликабельные чекбоксы в стиле Google Keep с автоматическим зачёркиванием выполненных пунктов (`~~текст~~`).
  - Быстрая конвертация выделенного или всего текста в список и обратно («Сделать список» / «Обычный текст»).
  - Корректная обработка переносов строк (*Soft line breaks*) и автоматическая дедупликация заголовков, созданных в веб-клиенте.
- **Организация и навигация**:
  - Навигационная шторка (Navigation Drawer) со списком всех категорий и счётчиками заметок.
  - Удобный выбор и создание категорий через BottomSheet.
  - Избранные заметки (Star / Favorite) и быстрый поиск по содержимому и заголовкам.

### 🛠 Технологический стек

| Слой | Технологии |
|---|---|
| Язык | Kotlin 2.1.0 |
| UI | XML Views + RecyclerView (StaggeredGridLayoutManager) + Material 3 Components |
| Архитектура | MVVM (ViewModel + StateFlow) + Single Activity |
| Локальная БД | Room 2.7.1 (SQLite) |
| Сеть | Retrofit 2.11.0 + OkHttp 4.12.0 + Nextcloud Notes REST API v1 |
| Внедрение зависимостей | Dagger Hilt 2.56.2 |
| Рендеринг Markdown | Markwon 4.6.2 (Custom TaskListPlugin, SoftLineBreak, Strikethrough, Tables) |
| Фоновые задачи | AndroidX WorkManager |
| Мин. / Целевая версия SDK | Min SDK 28 (Android 9.0) / Target SDK 35 (Android 15) |

### 🚀 Сборка и запуск

1. **Клонирование репозитория**:
   ```bash
   git clone https://github.com/maelst-dev/KeepNotes-for-Nextcloud.git
   ```

2. **Открытие в Android Studio**:
   - Запустите Android Studio (Ladybug / Meerkat или новее).
   - Выберите **File ➔ Open** и укажите папку репозитория.
   - Дождитесь завершения автоматической синхронизации Gradle (*Gradle Sync*).

3. **Запуск**:
   - Подключите реальное Android-устройство или запустите эмулятор (API 28+).
   - Нажмите кнопку **Run** (`Shift + F10`).

---

<a name="english"></a>
## 🇬🇧 English

**KeepNotes for Nextcloud** is an Android client for [Nextcloud Notes](https://apps.nextcloud.com/apps/notes) featuring a **Google Keep-inspired** UI and UX (staggered card grid, Material 3 theming, interactive checklists, categories, offline-first bidirectional sync).

### ✨ Features

- **Google Keep-inspired UI**: Staggered grid card layout with Material 3 dynamic theming (Light & Dark modes).
- **Seamless Authentication**: Nextcloud **Login Flow v2** support (1-click browser login without manual app password setup).
- **Robust Offline-First Architecture**:
  - Local Room database acts as the single source of truth.
  - Instant local operations (create, update, delete) without waiting for network responses.
  - Bidirectional server synchronization using `etag` and `modified` timestamps.
  - Background periodic sync powered by **WorkManager** + manual Pull-to-Refresh.
- **Rich Markdown & Interactive Checklists**:
  - **Edit Mode**: Smart Enter key handling that auto-inserts new checklist items (preserving indentation) and exits cleanly on empty items.
  - **Preview Mode**: Large Google Keep-style clickable checkboxes with automatic strikethrough (`~~text~~`) on completed items.
  - 1-tap conversion: Plain text ➔ Checklist and Checklist ➔ Plain text.
  - Unified soft line break handling and automatic title deduplication for notes created in Nextcloud web.
- **Organization & Navigation**:
  - Navigation Drawer with categories and note counts.
  - Category Picker BottomSheet to assign or create categories on the fly.
  - Starred / Favorites filter and quick search across titles and content.

### 🛠 Tech Stack

| Layer | Tech |
|---|---|
| Language | Kotlin 2.1.0 |
| UI | XML Views + RecyclerView (StaggeredGridLayoutManager) + Material 3 Components |
| Architecture | MVVM (ViewModel + StateFlow) + Single Activity |
| Local Storage | Room 2.7.1 (SQLite) |
| Networking | Retrofit 2.11.0 + OkHttp 4.12.0 + Nextcloud Notes REST API v1 |
| Dependency Injection | Dagger Hilt 2.56.2 |
| Markdown Engine | Markwon 4.6.2 (Custom TaskListPlugin, SoftLineBreak, Strikethrough, Tables) |
| Background Work | AndroidX WorkManager |
| Min / Target SDK | Min SDK 28 (Android 9.0) / Target SDK 35 (Android 15) |

### 🚀 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/maelst-dev/KeepNotes-for-Nextcloud.git
   ```

2. **Open in Android Studio**:
   - Open Android Studio (Ladybug / Meerkat or newer).
   - Select **File ➔ Open** and choose the cloned repository folder.
   - Wait for the initial Gradle sync to complete.

3. **Build & Run**:
   - Connect an Android device or start an emulator (API 28+).
   - Click **Run** (`Shift + F10`).
