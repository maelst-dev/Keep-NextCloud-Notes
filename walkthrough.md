# Keep NC — Walkthrough

Приложение полностью сгенерировано. Ниже — итог того, что создано, и инструкция по запуску.

---

## Что создано

### Структура проекта (60 файлов)

```
keep.nc.local/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts          ← все зависимости
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/keepnc/
│           ├── KeepNcApp.kt      ← @HiltAndroidApp + WorkManager
│           ├── data/
│           │   ├── local/        ← NoteEntity, NoteDao, AppDatabase, Converters, SyncStatus
│           │   ├── remote/       ← NotesApi (Retrofit), AuthInterceptor, dto/NoteDto
│           │   ├── auth/         ← Credentials, TokenStorage, LoginFlowService
│           │   └── repository/   ← NotesRepository (offline-first sync)
│           ├── di/               ← AppModule, AuthModule (Hilt)
│           ├── work/             ← SyncWorker (WorkManager)
│           └── ui/
│               ├── auth/         ← LoginActivity, LoginViewModel, LoginState
│               ├── notes/        ← NotesFragment, NotesViewModel, NoteCardAdapter,
│               │                    NotesFilter, NotesUiState
│               ├── editor/       ← EditorFragment, EditorViewModel, EditorUiState
│               ├── MainActivity.kt
│               └── MainViewModel.kt
└── res/
    ├── layout/ (6 XML)
    ├── menu/ (3 XML)
    ├── navigation/nav_graph.xml
    ├── drawable/ (11 иконок + 2 фона)
    ├── anim/ (4 анимации)
    └── values/ (strings, themes, colors)
```

---

## Как открыть в Android Studio

> [!IMPORTANT]
> Нужно добавить `gradle/wrapper/gradle-wrapper.jar` — без него Gradle Wrapper не запустится.
> Android Studio сделает это автоматически при открытии проекта через **File → Open**.

1. Открой **Android Studio** (Hedgehog / Iguana / Ladybug или новее)
2. **File → Open** → выбери папку `D:\OSPanel-651\home\keep.nc.local`
3. Дождись синхронизации Gradle (первый раз скачает ~500 МБ зависимостей)
4. **Build → Make Project** (`Ctrl+F9`) — должно собраться без ошибок
5. Запусти на эмуляторе API 28+ или реальном устройстве

---

## Как авторизоваться

1. Запусти приложение → откроется экран входа
2. Введи URL своего Nextcloud сервера, например: `https://cloud.example.com`
3. Нажми **Login with Nextcloud** → откроется браузер со страницей авторизации Nextcloud
4. Войди в Nextcloud в браузере и нажми **Предоставить доступ**
5. Приложение автоматически получит app password и перейдёт на главный экран

---

## Функции v1

| Функция | Статус |
|---|---|
| Login Flow v2 (OAuth-like через браузер) | ✅ |
| Сетка карточек 2 колонки (staggered) | ✅ |
| Создание / редактирование / удаление заметок | ✅ |
| Markdown редактор + Preview (Markwon) | ✅ |
| Поиск по заметкам | ✅ |
| Фильтр по категориям (Navigation Drawer) | ✅ |
| Избранное / pinned notes (звёздочка) | ✅ |
| Офлайн-first (Room) | ✅ |
| Фоновая синхронизация (WorkManager) | ✅ |
| Pull-to-refresh | ✅ |
| Material 3 / следует системной теме | ✅ |
| Анимации переходов | ✅ |
| Безопасное хранение учётных данных | ✅ |

---

## Известные ограничения v1

> [!NOTE]
> **Динамический URL Retrofit**: Retrofit создаётся с базовым URL при старте приложения. После первого входа нужен перезапуск Activity (уже реализован через `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` при логауте и после логина). Для продакшна стоит заменить на динамический `baseUrl` через перехватчик.

> [!NOTE]
> **Цвета карточек**: не реализованы в v1, как обговорено. Легко добавить позже — маппинг Nextcloud-категории → цвет фона карточки.

> [!NOTE]
> **Launcher icon**: используется дефолтная иконка Android. Можно заменить через **Image Asset Studio** в Android Studio.

---

## Следующие шаги

- [ ] Добавить цвета карточек (по категориям)
- [ ] Добавить поддержку чек-листов (GFM task lists в Markwon)
- [ ] Добавить сортировку заметок
- [ ] Добавить виджет на рабочий стол
- [ ] Pub на Google Play / F-Droid
