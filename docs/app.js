"use strict";

App.QRCODE_GENERATORS = ["local", "zxing", "google"];
App.DEFAULT_QRCODE_GENERATOR = "local";

App.ERROR_CORRECTION_LEVELS = ["L", "M", "Q", "H"];
App.DEFAULT_ERROR_CORRECTION_LEVEL = "M";

App.LOCAL_STORAGE_FAVORITES = "favorites";
App.LOCAL_STORAGE_GENERATOR = "generator";
App.LOCAL_STORAGE_CORRECTION_LEVEL = "correction-level";

window.onload = function() {
  window.app = new App();
};

function App() {
  this.initElements();
  this.initServiceWorker();
  this.initPWA();
  this.initGenerators();
  this.initCorrectionLevels();
  this.initFavorites();
  this.initInput();
  window.onhashchange();
}

App.prototype.initElements = function() {
  this.input = document.getElementById("input");
  this.container = document.getElementById("container");
  this.progress = document.getElementById("progress");
  this.favoriteToggle = document.getElementById("favorite-toggle");
  this.favoritesToggle = document.getElementById("favorites-toggle");
  this.favoritesContainer = document.getElementById("favorites-container");
  this.favoriteTemplate = document.getElementById("template-favorite-item");
};

App.prototype.initServiceWorker = function() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker
      .register("sw.js", { scope: "./" })
      .then(function(registration) {
        console.log("Service Worker Registered");
      })
      .catch(function(err) {
        console.log("Service Worker Registration Failed: ", err);
      });
    navigator.serviceWorker.ready.then(function(registration) {
      console.log("Service Worker Ready");
    });
  }

  window.addEventListener("online", this.renderQrCode.bind(this));
};

App.prototype.initPWA = function() {
  window.addEventListener("beforeinstallprompt", this.beforeInstallPrompt.bind(this));
  document.getElementById("pwa-install").addEventListener("click", this.promptPWA.bind(this), false);
};

App.prototype.beforeInstallPrompt = function(event) {
  event.preventDefault();
  this.PWA = event;
  Utils.show(document.getElementById("pwa"));
};

App.prototype.promptPWA = function(event) {
  this.PWA.prompt();
  this.PWA.userChoice.then(function(result) {
    console.log("PWA result:", result);
    Utils.hide(document.getElementById("pwa"));
  });
  event.preventDefault();
  event.stopPropagation();
};

App.prototype.initFavorites = function() {
  this.favorites = this.getFavorites();
  for (var i = 0; i < this.favorites.length; i++) {
    this.addFavoriteNode(this.favorites[i]);
  }
  this.favoriteToggle.addEventListener("click", this.toggleFavorite.bind(this), false);
};

App.prototype.toggleFavorite = function(event) {
  const value = this.input.value;
  if (this.favorites.indexOf(value) !== -1) {
    this.removeFavorite(value);
  } else {
    this.addFavorite(value);
  }
};

App.prototype.isFavorite = function(favorite) {
  return this.favorites.indexOf(favorite) !== -1;
};

App.prototype.addFavorite = function(favorite) {
  this.favorites.unshift(favorite);
  this.addFavoriteNode(favorite);
  this.setFavorites(this.favorites);
  this.renderInput();
};

App.prototype.addFavoriteNode = function(favorite) {
  const item = Utils.inflate(this.favoriteTemplate);
  const span = item.querySelector("[favorite-item]");
  span.textContent = favorite;
  span.setAttribute("favorite-item", favorite);
  item.href = "#" + encodeURIComponent(favorite);
  const remove = item.querySelector("button.close");
  remove.addEventListener(
    "click",
    function f(event) {
      event.preventDefault();
      this.removeFavorite(favorite);
      // This will close the dropdown when empty
      if (this.favorites.length > 0) {
        event.stopPropagation();
      }
    }.bind(this)
  );
  this.favoritesContainer.insertBefore(item, this.favoritesContainer.children[1]);
  this.favoritesToggle.removeAttribute("disabled");
};

App.prototype.removeFavorite = function(favorite) {
  this.favorites.splice(this.favorites.indexOf(favorite), 1);
  this.removeFavoriteNode(favorite);
  this.setFavorites(this.favorites);
  this.renderInput();
};

App.prototype.removeFavoriteNode = function(favorite) {
  const items = this.favoritesContainer.querySelectorAll("[favorite-item]");
  for (var i = 0; i < items.length; i++) {
    const item = items[i];
    if (item.getAttribute("favorite-item") == favorite) {
      this.favoritesContainer.removeChild(item.parentNode);
    }
  }
  if (this.favorites.length == 0) {
    this.favoritesToggle.setAttribute("disabled", "true");
  }
};

App.prototype.setFavorites = function(favorites) {
  localStorage.setItem(App.LOCAL_STORAGE_FAVORITES, JSON.stringify(favorites));
};

App.prototype.getFavorites = function() {
  const favorites = localStorage.getItem(App.LOCAL_STORAGE_FAVORITES);
  return (favorites && JSON.parse(favorites)) || [];
};

App.prototype.initGenerators = function() {
  const generators = document.querySelectorAll("[data-generator]");
  for (let i = 0; i < generators.length; i++) {
    const generator = generators[i];
    const value = generator.getAttribute("data-generator");
    generator.addEventListener("click", this.selectGenerator.bind(this, value), false);
  }

  const docs = document.querySelectorAll("[data-generator-url]");
  for (let i = 0; i < docs.length; i++) {
    const doc = docs[i];
    const url = doc.getAttribute("data-generator-url");
    doc.addEventListener(
      "click",
      function(event) {
        window.open(url, "_blank");
        event.preventDefault();
        event.stopPropagation();
      },
      false
    );
  }

  this.renderGenerators();
};

App.prototype.selectGenerator = function(generator, event) {
  this.setGenerator(generator);
  this.renderGenerators();
  this.renewQrCode();
  event.preventDefault();
  event.stopPropagation();
};

App.prototype.renderGenerators = function() {
  const generator = this.getGenerator();
  const entries = document.querySelectorAll("[data-generator]");
  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];
    if (generator == entry.getAttribute("data-generator")) {
      entry.classList.add("active");
    } else {
      entry.classList.remove("active");
    }
  }
};

App.prototype.setGenerator = function(generator) {
  localStorage.setItem(App.LOCAL_STORAGE_GENERATOR, generator);
};

App.prototype.getGenerator = function() {
  const generator = localStorage.getItem(App.LOCAL_STORAGE_GENERATOR);
  return App.QRCODE_GENERATORS.indexOf(generator) !== -1 ? generator : App.DEFAULT_QRCODE_GENERATOR;
};

App.prototype.renderQrCode = function() {
  const data = this.input.value;
  const encodedData = encodeURIComponent(this.input.value);
  const correction = this.getCorrectionLevel();
  const containerSize = this.container.clientWidth;
  this.container.setAttribute("title", data);
  switch (this.getGenerator()) {
    case "local":
      this.qrcode =
        this.qrcode ||
        new QRCode(this.container, {
          width: 1024,
          height: 1024,
          correctLevel: this.parseCorrectionLevel(correction)
        });
      try {
        this.qrcode.makeCode(data);
        this.renderState(true);
      } catch (error) {
        this.renderState(false);
      }
      break;
    case "google":
      const size = Math.min(512, containerSize); // Max 300000px² ~ 512
      const google = this.createSafeImage();
      google.src = "https://chart.apis.google.com/chart?cht=qr&chs=" + size + "x" + size + "&chld=" + correction + "|0&choe=UTF-8&chl=" + encodedData;
      break;
    case "zxing":
      const zxing = this.createSafeImage();
      zxing.src = "https://zxing.org/w/chart?cht=qr&chs=" + containerSize + "x" + containerSize + "&chld=" + correction + "&choe=UTF-8&chl=" + encodedData;
      break;
  }
};

App.prototype.createSafeImage = function() {
  const img = this.container.querySelector("img");
  if (img) {
    return img;
  }
  const image = this.container.appendChild(document.createElement("img"));
  image.style.visibility = "hidden";
  image.onload = function() {
    image.style.visibility = "visible";
    this.renderState(true);
  }.bind(this);
  image.onerror = function() {
    image.style.visibility = "hidden";
    this.renderState(false);
  }.bind(this);
  return image;
};

App.prototype.renderState = function(success) {
  Utils.hide(this.progress);
  if (success) {
    this.container.classList.remove("border");
    this.container.classList.remove("border-danger");
  } else {
    this.container.classList.add("border");
    this.container.classList.add("border-danger");
  }
};

App.prototype.renewQrCode = function() {
  Utils.show(this.progress);

  // Clear local QrCode
  if (this.qrcode) {
    this.qrcode.clear();
    this.qrcode = null;
  }

  // Remove previous canvas/images
  const nodes = this.container.childNodes;
  for (let i = nodes.length - 1; i >= 0; i--) {
    const node = nodes[i];
    if (node != this.progress) {
      Utils.remove(node);
    }
  }

  this.renderQrCode();
};

App.prototype.initInput = function() {
  this.input.addEventListener("input", this.onInputChange.bind(this));
  this.input.value = decodeURIComponent(window.location.hash.substr(1));

  window.onhashchange = function() {
    this.input.value = decodeURIComponent(window.location.hash.substr(1));
    this.renderInput();
    this.renderQrCode();
  }.bind(this);
};

App.prototype.onInputChange = function(event) {
  document.location.hash = encodeURIComponent(this.input.value);
  this.renderInput();
};

App.prototype.renderInput = function() {
  const data = this.input.value;
  this.input.setAttribute("rows", (data.match(/\n/g) || []).length + 1);
  if (this.isFavorite(data)) {
    this.input.classList.add("text-primary");
  } else {
    this.input.classList.remove("text-primary");
  }
  if (data.length == 0) {
    this.favoriteToggle.setAttribute("disabled", "true");
  } else {
    this.favoriteToggle.removeAttribute("disabled");
  }
};

App.prototype.initCorrectionLevels = function() {
  const levels = document.querySelectorAll("[data-correction-level]");
  for (let i = 0; i < levels.length; i++) {
    const level = levels[i];
    const value = level.getAttribute("data-correction-level");
    level.addEventListener("click", this.selectCorrectionLevel.bind(this, value), false);
  }
  this.renderCorrectionLevels();
};

App.prototype.selectCorrectionLevel = function(level, event) {
  this.setCorrectionLevel(level);
  this.renderCorrectionLevels();
  this.renewQrCode();
  event.preventDefault();
  event.stopPropagation();
};

App.prototype.renderCorrectionLevels = function() {
  const level = this.getCorrectionLevel();
  const entries = document.querySelectorAll("[data-correction-level]");
  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];
    if (level == entry.getAttribute("data-correction-level")) {
      entry.classList.add("active");
    } else {
      entry.classList.remove("active");
    }
  }
};

App.prototype.setCorrectionLevel = function(level) {
  localStorage.setItem(App.LOCAL_STORAGE_CORRECTION_LEVEL, level);
};

App.prototype.getCorrectionLevel = function() {
  const level = localStorage.getItem(App.LOCAL_STORAGE_CORRECTION_LEVEL);
  return App.ERROR_CORRECTION_LEVELS.indexOf(level) !== -1 ? level : App.DEFAULT_ERROR_CORRECTION_LEVEL;
};

App.prototype.parseCorrectionLevel = function(raw) {
  switch (raw) {
    case "L":
      return QRCode.CorrectLevel.L;
    case "M":
      return QRCode.CorrectLevel.M;
    case "Q":
      return QRCode.CorrectLevel.Q;
    case "H":
      return QRCode.CorrectLevel.H;
    default:
      return QRCode.CorrectLevel.M;
  }
};

function Utils() {}

Utils.inflate = function(template) {
  if (template.content) {
    return document.importNode(template.content, true).firstElementChild;
  } else {
    return document.importNode(template.firstElementChild, true);
  }
};

Utils.show = function(element) {
  element && element.removeAttribute("hidden");
};

Utils.hide = function(element) {
  element && element.setAttribute("hidden", "true");
};

Utils.isHidden = function(element) {
  return element && element.hasAttribute("hidden");
};

Utils.remove = function(element) {
  element && element.parentNode && element.parentNode.removeChild(element);
};
