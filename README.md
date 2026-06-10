# VoiceSearch

Android-приложение для голосового / клавиатурного поиска по табличным данным
(XLSX/CSV) с возможностью отмечать найденные строки. Источники таблицы:
локальный файл или публичная ссылка на Яндекс.Диск / Яндекс.Документы. Файл с
отметками выгружается обратно на Яндекс.Диск пользователя.

## Возможности

- 🎙️ **Голосовой поиск** через системный `SpeechRecognizer` (Android STT)
- ⌨️ **Встроенная клавиатура** с цифрами и буквами кодов (АБВ + ABC) — на случай
  шумного места или сбоев голоса
- 📊 **Автоопределение префикса**: если у всех строк столбца общий "12345", приложение
  попросит произнести только последние N символов
- ✅ **Отметка строк** маркером (по умолчанию «Есть») в выбранном столбце
- 📤 **Шеринг файла** через стандартный `ACTION_SEND` (Telegram, Почта,
  что у вас есть)
- ☁️ **Авто-синхронизация** изменений с Яндекс.Диском (WorkManager + debounce 10с)
- 🌓 **Светлая / тёмная / системная тема**
- 🛡️ **Никакой аналитики**, никаких сторонних сервисов кроме Яндекс.Диска
  (по явному выбору пользователя)

## Стек

- Kotlin 2.0.21, Jetpack Compose, Material 3
- Hilt (DI), Room (хранилище), DataStore (preferences), EncryptedSharedPreferences (OAuth токены)
- Retrofit + OkHttp + kotlinx-serialization для Яндекс.Диск API
- WorkManager для фоновой синхронизации
- Многомодульная архитектура: `app` / `core:ui` / `core:domain` / `core:data` / `core:network`

## Установка для разработки

```bash
git clone https://github.com/sashka1850/VoiceSearch.git
cd VoiceSearch
```

Создайте `local.properties` со своими ключами Яндекс OAuth (опционально —
без них сборка пройдёт, но синхронизация с Яндекс.Диском не будет работать):

```
sdk.dir=/path/to/Android/Sdk
YANDEX_CLIENT_ID=ваш_client_id
YANDEX_CLIENT_SECRET=ваш_client_secret
```

Регистрация приложения: https://oauth.yandex.ru → «Для доступа к API или
отладки» → запросить доступы `cloud_api:disk.read`, `cloud_api:disk.write`,
`cloud_api:disk.info`.

Сборка:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest detekt :app:lintDebug
```

## Целевой стор

Основной канал распространения — **RuStore**. Приложение не использует
Google Play Services, Firebase, Google Sign-In и не зависит от GMS — собирается
без `google-services.json` и работает на устройствах без сервисов Google.

## Конфиденциальность

См. [PRIVACY_POLICY.md](./PRIVACY_POLICY.md).

Короткая версия: всё хранится локально. На сеть выходим только в Яндекс.Диск
(если пользователь явно подключил), и в системный STT для распознавания речи.

## Лицензия

MIT (см. LICENSE при наличии).
