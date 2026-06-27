'use strict';

var CACHE_NAME = 'vocab-app-v2';
var STATIC_ASSETS = [
  '/index.html',
  '/style.css',
  '/app.js',
  '/manifest.json'
];

self.addEventListener('install', function(event) {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then(function(cache) {
      return cache.addAll(STATIC_ASSETS);
    })
  );
});

self.addEventListener('activate', function(event) {
  event.waitUntil(
    caches.keys().then(function(cacheNames) {
      return Promise.all(
        cacheNames.map(function(name) {
          if (name !== CACHE_NAME) {
            return caches.delete(name);
          }
        })
      );
    }).then(function() {
      return self.clients.claim();
    })
  );
});

self.addEventListener('fetch', function(event) {
  // Network-first for HTML, cache-first for static assets
  var requestUrl = new URL(event.request.url);
  var isHtml = requestUrl.pathname === '/' || requestUrl.pathname.endsWith('.html');

  if (isHtml) {
    // Network first: always get latest HTML, fallback to cache
    event.respondWith(
      fetch(event.request).then(function(networkResponse) {
        return caches.open(CACHE_NAME).then(function(cache) {
          cache.put(event.request, networkResponse.clone());
          return networkResponse;
        });
      }).catch(function() {
        return caches.match(event.request);
      })
    );
  } else {
    // Cache-first for static assets (CSS, JS)
    event.respondWith(
      caches.match(event.request).then(function(cachedResponse) {
        if (cachedResponse) {
          return cachedResponse;
        }
        return fetch(event.request).then(function(networkResponse) {
          if (networkResponse && networkResponse.status === 200 && STATIC_ASSETS.indexOf(requestUrl.pathname) !== -1) {
            var responseToCache = networkResponse.clone();
            caches.open(CACHE_NAME).then(function(cache) {
              cache.put(event.request, responseToCache);
            });
          }
          return networkResponse;
        });
      })
    );
  }
});

self.addEventListener('message', function(event) {
  if (event.data && event.data.action === 'skipWaiting') {
    self.skipWaiting();
  }
});
