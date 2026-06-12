// File hosts a const, a list constant, and the PrivacySection type they
// share — naming the file after any one of those misrepresents the rest,
// so we keep the topic-named file name.
@file:Suppress("MatchingDeclarationName")

package com.voicesearch.app.ui.settings

/**
 * Minimal privacy policy rendered in-app. Kept short and impersonal so it
 * satisfies RuStore's review checklist (telling users what we touch and
 * which permissions are used) without volunteering author or contact info.
 *
 * Source of truth: this list is what users actually see. PRIVACY_POLICY.md
 * in the repo mirrors the same content for anyone reading on GitHub.
 *
 * Edit by adding/removing [PrivacySection] entries — the screen renders
 * them as titled paragraphs in order.
 */
internal data class PrivacySection(val title: String, val body: String)

internal const val PRIVACY_LAST_UPDATED = "12 июня 2026 г."

internal val PRIVACY_SECTIONS: List<PrivacySection> = listOf(
    PrivacySection(
        title = "Что хранится локально",
        body = "Импортированные таблицы, отметки строк, настройки поиска " +
            "и тема оформления хранятся только во внутренней памяти " +
            "приложения. На внешние серверы эти данные не отправляются.",
    ),
    PrivacySection(
        title = "Голосовое распознавание",
        body = "При нажатии и удержании кнопки микрофона приложение " +
            "передаёт звук в системный сервис распознавания речи Android. " +
            "Распознанный текст возвращается приложению и не сохраняется. " +
            "Какой именно сервис распознаёт речь — определяет операционная " +
            "система устройства.",
    ),
    PrivacySection(
        title = "Яндекс.Диск (по выбору пользователя)",
        body = "Если пользователь подключает Яндекс.Диск для синхронизации, " +
            "приложение использует OAuth 2.0 и хранит токен доступа в " +
            "зашифрованном виде на устройстве. Сетевые запросы выполняются " +
            "только к серверам Яндекса (cloud-api.yandex.net, oauth.yandex.ru). " +
            "Запрашиваются только разрешения на чтение и запись файлов " +
            "Яндекс.Диска. Без явного подключения приложение не выходит в сеть.",
    ),
    PrivacySection(
        title = "Логи падений",
        body = "Если приложение аварийно завершается, его стек-трейс " +
            "записывается в локальный файл во внутренней памяти (хранится " +
            "не более 10 последних логов). Файлы никуда автоматически " +
            "не отправляются. В разделе «Диагностика» пользователь может " +
            "поделиться ими через системное меню или удалить.",
    ),
    PrivacySection(
        title = "Разрешения",
        body = "RECORD_AUDIO — для голосового ввода через системный " +
            "распознаватель. INTERNET — для опциональной синхронизации " +
            "с Яндекс.Диском. Других разрешений приложение не запрашивает.",
    ),
    PrivacySection(
        title = "Что НЕ собирается",
        body = "Аналитика, рекламные идентификаторы, контакты, SMS, " +
            "фотографии, геолокация, идентификаторы устройства, любые " +
            "персональные данные.",
    ),
    PrivacySection(
        title = "Изменения политики",
        body = "При существенных изменениях обновлённая версия будет " +
            "опубликована в описании приложения в RuStore. Дата выше " +
            "отражает последнее обновление этого документа.",
    ),
)
