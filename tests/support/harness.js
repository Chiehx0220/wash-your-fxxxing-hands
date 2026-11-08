const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');
const { install } = require('./page-shim');

const ROOT = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets');
const INJECT = fs.readFileSync(path.join(ROOT, 'inject.js'), 'utf8');
const FORM = fs.readFileSync(path.join(__dirname, 'form.html'), 'utf8');

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// Boot: injection at document start, then the page populates its options
async function boot({ prefs = {}, roster = {}, themeCss = '' } = {}) {
  const store = { prefs: JSON.stringify(prefs) };
  const calls = { ready: 0, openRepo: 0 };
  const bridge = { messages: [], postMessage(message) { bridge.messages.push(message); } };
  const dom = new JSDOM(FORM, {
    url: 'https://x-script.googleusercontent.com/userCodeAppPanel',
    runScripts: 'outside-only',
    pretendToBeVisual: true,
    beforeParse(window) {
      window.NativeStore = {
        ready() { calls.ready++; },
        openRepo() { calls.openRepo++; },
        themeCss() { return themeCss; },
        roster() { return JSON.stringify(roster); },
        get(key) { return store[key] || ''; },
        set(key, value) { store[key] = value; }
      };
      window.qaBridge = bridge;
      window.eval(INJECT);
    }
  });
  const { window } = dom;
  install(window);
  await wait(150);
  const $ = (id) => window.document.getElementById(id);
  const $$ = (selector, root) => Array.from((root || window.document).querySelectorAll(selector));
  const pick = (id, value) => {
    const node = $(id);
    node.value = value;
    node.dispatchEvent(new window.Event('input', { bubbles: true }));
    node.dispatchEvent(new window.Event('change', { bubbles: true }));
  };
  const back = () => bridge.onmessage({ data: 'back' });
  return { window, document: window.document, store, calls, bridge, back, $, $$, pick, wait, close: () => window.close() };
}

module.exports = { boot, wait, ROOT };
