// Офлайн: при установке кладём в кэш все файлы приложения и дальше отдаём их только оттуда.
// Две строки ниже после сборки дистрибутива переписывает Gradle (shared/build.gradle.kts): имена wasm-файлов
// меняются с каждой сборкой. Без подстановки (dev-сервер) список пуст и всё идёт из сети.
const VERSION = "dev";
const FILES = [];

const CACHE = "app-" + VERSION;

// Новая версия не ждёт, пока закроются все вкладки со старой: скачалась — и со следующего открытия страницы
// работает она. Уже открытая страница остаётся на своей сборке: её файлы загружены.
self.addEventListener("install", event => {
    event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(FILES)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", event => {
    event.waitUntil(
        caches.keys()
            .then(names => Promise.all(names.filter(name => name !== CACHE).map(name => caches.delete(name))))
            .then(() => self.clients.claim())
    );
});

// Чужие адреса (курсы) не трогаем: их кэширует само приложение.
self.addEventListener("fetch", event => {
    const request = event.request;
    if (request.method !== "GET" || new URL(request.url).origin !== self.location.origin) return;
    const cached = request.mode === "navigate" ? "index.html" : request;
    event.respondWith(caches.open(CACHE).then(cache => cache.match(cached)).then(hit => hit || fetch(request)));
});
