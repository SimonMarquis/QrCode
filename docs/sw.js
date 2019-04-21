"use strict";

const cacheName = "qrcode-1.0.0";

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(cacheName).then(function(cache) {
      return cache
        .addAll(["./", "./app.css", "./app.js", "./qrcode.js", "./favicon.ico"])
        .then(() => self.skipWaiting());
    })
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener("fetch", event => {
  event.respondWith(
    caches
      .open(cacheName)
      .then(cache => cache.match(event.request, { ignoreSearch: true }))
      .then(response => {
        return response || fetch(event.request);
      })
  );
});
