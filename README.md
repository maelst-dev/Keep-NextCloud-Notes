# KeepNotes for Nextcloud (Android Client)

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-28-green.svg)](https://developer.android.com)

[English](#english) | [Русский](#русский)

---

<a name="русский"></a>
## 🇷🇺 Русский

**KeepNotes for Nextcloud** — быстрый, открытый Android-клиент для [Nextcloud Notes](https://apps.nextcloud.com/apps/notes) с интерфейсом и пользовательским опытом в стиле **Google Keep** (адаптивная сетка заметок, тема Material 3, интерактивные чек-листы, категории, полнофункциональный офлайн-режим с двусторонней синхронизацией).

Приложение разработано без закрытых библиотек и трекеров, не использует Google Play Services и полностью соответствует стандартам свободного ПО (F-Droid).

### ✨ Основные возможности

- **Интерфейс в стиле Google Keep**: адаптивная сетка заметок (Staggered Grid) с поддержкой динамических цветов Material 3 (светлая и тёмная темы).
- **Бесшовная авторизация**: поддержка официального **Nextcloud Login Flow v2** (вход в один клик через браузер без ручного ввода паролей приложений).
- **Полноценный Offline-First**:
  - Локальная база данных Room выступает единым источником правды (*Single Source of Truth*).
  - Мгновенное создание, редактирование и удаление заметок без ожидания ответа сервера.
  - Двусторонняя синхронизация с разрешением конфликтов по `etag` и временным меткам.
  - Фоновая периодическая синхронизация через **WorkManager**, ручное обновление жестом *Pull-to-Refresh* и тонкая полоса индикации прогресса в верхней панели.
- **Продвинутый Markdown и списки задач**:
  - **Режим редактирования (Edit)**: умное автодополнение списков при нажатии Enter (с сохранением отступов), быстрый выход из списка, автоматическая очистка пустых пунктов.
  - **Режим просмотра (Preview)**: крупные кликабельные чекбоксы в стиле Google Keep с автоматическим зачёркиванием выполненных пунктов (`~~текст~~`).
  - Быстрая конвертация выделенного или всего текста в список и обратно («Сделать список» / «Обычный текст»).
  - Корректная обработка переносов строк (*Soft line breaks*) и автоматическая дедупликация заголовков, созданных в веб-клиенте.
- **Организация и навигация**:
  - Навигационная шторка (Navigation Drawer) со списком всех категорий и счётчиками заметок.
  - Удобный выбор и создание категорий через BottomSheet.
  - Избранные заметки (Star / Favorite) и быстрый поиск по содержимому и заголовкам.
- **Безопасность и экран настроек**:
  - **Биометрическая защита (App Lock)**: блокировка приложения по отпечатку пальца, лицу или системному PIN-коду устройства (AndroidX Biometric, 100% F-Droid compliant).
  - **Размеры шрифтов**: независимый выбор размера шрифта для карточек и редактора (Маленький, Обычный, Крупный, Очень крупный).
  - **Управление подтверждениями**: включение/отключение диалогов подтверждения сохранения при выходе и удаления заметок.
- **Мультиязычность**:
  - Встроенный выбор языка (русский / английский) прямо в настройках приложения (Per-App Language).
- **Приватность**:
  - 100% открытый исходный код без Google Play Services, рекламы, телеметрии и аналитики.

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
| Безопасность | AndroidX Biometric 1.1.0 + EncryptedSharedPreferences |
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

### 📄 Лицензия

Проект распространяется под свободной лицензией **Apache License 2.0**. Полный текст доступен в файле [LICENSE](LICENSE).

---

<a name="english"></a>
## 🇬🇧 English

**KeepNotes for Nextcloud** is a fast, open-source Android client for [Nextcloud Notes](https://apps.nextcloud.com/apps/notes) featuring a **Google Keep-inspired** user experience (staggered card grid, Material 3 dynamic theming, interactive checklists, categories, offline-first bidirectional sync).

Built strictly without proprietary blobs or tracking libraries, free from Google Play Services, and fully compliant with F-Droid inclusion standards.

### ✨ Features

- **Google Keep-inspired UI**: Adaptive staggered grid card layout with Material 3 dynamic color theming (Light & Dark modes).
- **Seamless Authentication**: Nextcloud **Login Flow v2** support (1-click browser login without manual app password setup).
- **Robust Offline-First Architecture**:
  - Local Room database acts as the single source of truth.
  - Instant local operations (create, update, delete) without waiting for network responses.
  - Bidirectional server synchronization using `etag` and `modified` timestamps.
  - Background periodic sync powered by **WorkManager**, manual Pull-to-Refresh, and top progress bar indicator.
- **Rich Markdown & Interactive Checklists**:
  - **Edit Mode**: Smart Enter key handling that auto-inserts new checklist items (preserving indentation) and exits cleanly on empty items.
  - **Preview Mode**: Large Google Keep-style clickable checkboxes with automatic strikethrough (`~~text~~`) on completed items.
  - 1-tap conversion: Plain text ➔ Checklist and Checklist ➔ Plain text.
  - Unified soft line break handling and automatic title deduplication for notes created in Nextcloud web.
- **Organization & Navigation**:
  - Navigation Drawer with categories and note counts.
  - Category Picker BottomSheet to assign or create categories on the fly.
  - Starred / Favorites filter and quick search across titles and content.
- **Security & User Preferences**:
  - **Biometric App Lock**: Secure your notes with fingerprint, face recognition, or device PIN/pattern (AndroidX Biometric, 100% F-Droid compliant).
  - **Font Size Presets**: Configurable font sizes for cards and editor (Small, Medium, Large, Extra Large).
  - **Confirmation Dialogs**: Toggleable save-on-exit and note deletion confirmations.
- **In-App Language Selection**:
  - Change application language (English / Russian) directly from the settings screen.
- **Privacy First**:
  - 100% Free & Open Source software with zero tracking, zero analytics, zero ads, and no Google Play Services dependencies.

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
| Security | AndroidX Biometric 1.1.0 + EncryptedSharedPreferences |
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

### 📄 License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.
