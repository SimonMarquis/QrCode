"use strict";

const cacheName = "qrcode-1.1.0";

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(cacheName).then(function(cache) {
      return cache
        .addAll(["./", "./index.html", "./app.css", "./app.js", "./qrcode.js", "./favicon.ico", "./192.png", "./512.png"])
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
      .then(cache => cache.match(event.request))
      .then(response => response || fetch(event.request))
  );
});
