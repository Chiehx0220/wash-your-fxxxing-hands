// Document-start injection: Apps Script sandbox frame
(function () {
  'use strict';
  if (window.__quickAudit) return;
  window.__quickAudit = true;

  // Constants
  var AUDITOR_FIELDS = ['auditorName', 'auditorEmail', 'auditorUnit'];  // unit last: change handler reads name
  var CHIP_FIELDS = ['timing', 'action', 'title', 'step'];
  var CHIP_LIMIT = 12;
  var SEGMENTED_MAX = 3;
  var CHIP_COLUMNS = [{ maxLength: 3, columns: 4 }, { maxLength: 5, columns: 3 }, { maxLength: 8, columns: 2 }];
  var MENU = { maxHeight: 288, gap: 4, margin: 12, minSpace: 200 };  // minSpace: below this, open upward when roomier
  // Page contract: element ids / selectors the injection depends on
  var PAGE = {
    form: 'mainForm', date: 'auditDate', hour: 'auditHour', minute: 'auditMinute', count: 'auditCount',
    modalBody: 'modalMessageBody', timing: 'timing', action: 'action', step: 'step', title: 'title',
    auditorUnit: 'auditorUnit', auditorName: 'auditorName', auditorEmail: 'auditorEmail',
    auditedName: 'auditedName', auditedUnit: 'auditedUnit', unitOther: 'auditedUnitDropdownOther'
  };
  var SEL = {
    headline: '.py-2', headingText: '.py-2 h3', legends: 'fieldset legend', group: '.mb-3', inputGroup: '.input-group'
  };
  var STYLE_ID = 'qa-m3-style';
  var POLL_INTERVAL_MS = 250;
  var POLL_MAX_TRIES = 240;
  var READY_DELAY_MS = 80;
  var TEXT = {
    headlineLead: '大風吹',
    headlineSub: '吹什麼 吹沒洗手的人',
    requiredNotice: '請檢查標示為錯誤的欄位。',
    errorSelect: '請選擇一個選項',
    errorMissing: '請填寫此欄位',
    errorFormat: '格式不正確',
    auditor: '稽核人',
    auditorEmpty: '請設定稽核人',
    auditorHint: '輸入單位 姓名 信箱',
    done: '完成',
    now: '現在',
    repo: '原始碼'
  };
  var CLOCK_ICON =
    '<svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><path fill="currentColor" ' +
    'd="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 ' +
    '0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>';
  var GITHUB_ICON =
    '<svg viewBox="0 0 16 16" aria-hidden="true"><path fill="currentColor" d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 ' +
    '7.59c-.4.08-.55-.17-.55-.38 0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 ' +
    '0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 1.28-.82 2.15 ' +
    '0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.21-1.61.55-2.33-.66-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 ' +
    '2.69.94 0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z"/></svg>';
  var PERSON_ICON =
    '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 ' +
    '1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>';

  // DOM utilities
  function $(key) { return document.getElementById(PAGE[key] || key); }

  function all(selector, root) { return Array.from((root || document).querySelectorAll(selector)); }

  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text) node.textContent = text;
    return node;
  }

  function append(parent) {
    Array.prototype.slice.call(arguments, 1).forEach(function (child) { parent.appendChild(child); });
    return parent;
  }

  function button(className, text) {
    var node = el('button', className, text);
    node.type = 'button';
    return node;
  }

  function pad(n) { return String(n).padStart(2, '0'); }

  function setValue(node, value) {
    node.value = value;
    node.dispatchEvent(new Event('input', { bubbles: true }));
    node.dispatchEvent(new Event('change', { bubbles: true }));
  }

  function onChange(node, handler) {
    ['input', 'change'].forEach(function (type) { node.addEventListener(type, handler); });
  }

  function observeContent(node, handler) {
    new MutationObserver(handler).observe(node, { childList: true, characterData: true, subtree: true });
  }

  // Overlay stack: native back key arrives via qaBridge (WebMessageListener), closes the topmost layer
  var Layers = (function () {
    var stack = [];
    var bound = false;

    function bridge() {
      var port = window.qaBridge;
      if (port && !bound) {
        bound = true;
        port.onmessage = function (e) { if (e.data === 'back') back(); };
      }
      return port;
    }

    function report() {
      var port = bridge();
      if (port) port.postMessage(String(stack.length));
    }

    function back() {
      var hide = stack.pop();
      if (hide) hide();
      report();
    }

    return {
      open: function (hide) { stack.push(hide); report(); },
      close: function (hide) {
        var index = stack.lastIndexOf(hide);
        if (index < 0) return;
        stack.splice(index, 1);
        hide();
        report();
      }
    };
  })();

  // Native bridge: failures degrade to ''
  function callNative(method) {
    var args = Array.prototype.slice.call(arguments, 1);
    try { return window.NativeStore[method].apply(window.NativeStore, args); } catch (e) { return ''; }
  }

  function parseJson(text, fallback) {
    try { return JSON.parse(text); } catch (e) { return fallback; }
  }

  var Prefs = {
    load: function () { return parseJson(callNative('get', 'prefs'), {}); },
    update: function (key, value) {
      var prefs = Prefs.load();
      prefs[key] = value;
      callNative('set', 'prefs', JSON.stringify(prefs));
    }
  };

  // Theme, copy
  function applyTheme() {
    var css = callNative('themeCss');
    if (!css) return;
    var old = document.getElementById(STYLE_ID);
    if (old) old.remove();
    var style = el('style');
    style.id = STYLE_ID;
    style.textContent = css;
    (document.head || document.documentElement).appendChild(style);
  }

  function rewriteHeadline() {
    var heading = document.querySelector(SEL.headingText);
    if (!heading) return;
    heading.textContent = '';
    append(heading, el('span', 'qa-headline-lead', TEXT.headlineLead), el('span', 'qa-headline-sub', TEXT.headlineSub));
  }

  // Section ordinals ("1." / "一、") stale after the account section moves out
  function stripSectionNumbers() {
    all(SEL.legends).forEach(function (legend) {
      var node = legend.firstChild;
      while (node && node.nodeType !== 3) node = node.nextSibling;
      if (node) node.nodeValue = node.nodeValue.replace(/^\s*[0-9０-９一二三四五六七八九十]+\s*[.、．)）]\s*/, '');
    });
  }

  // Chip primitives
  // Equal-width grid: column count from the longest label
  function columnsFor(labels) {
    var longest = Math.max.apply(null, labels.map(function (label) { return Array.from(label).length; }));
    for (var i = 0; i < CHIP_COLUMNS.length; i++) {
      if (longest <= CHIP_COLUMNS[i].maxLength) return CHIP_COLUMNS[i].columns;
    }
    return 1;
  }

  function renderChips(row, labels, onPick) {
    row.textContent = '';
    labels.forEach(function (label) {
      var chip = button('qa-chip', label);
      chip.addEventListener('click', function () { onPick(label); });
      row.appendChild(chip);
    });
    if (labels.length) row.style.setProperty('--qa-cols', columnsFor(labels));
    row.style.display = labels.length ? '' : 'none';
  }

  function markActive(row, value) {
    Array.from(row.children).forEach(function (chip) {
      chip.classList.toggle('on', chip.textContent === value);
    });
  }

  function insertChipRow(before) {
    var row = el('div', 'qa-chips mb-2');
    row.style.display = 'none';
    before.parentNode.insertBefore(row, before);
    return row;
  }

  function chipifySelect(id) {
    var select = $(id);
    if (!select) return;
    var labels = all('option', select)
      .filter(function (o) { return o.value && !o.disabled; })
      .map(function (o) { return o.value; });
    var row = el('div', 'qa-chips' + (labels.length <= SEGMENTED_MAX ? ' qa-segmented' : ''));
    renderChips(row, labels, function (label) { setValue(select, label); });

    // Select retained: validation + page logic
    select.parentNode.style.position = 'relative';
    select.classList.add('qa-hidden-select');
    select.parentNode.insertBefore(row, select);
    select.addEventListener('change', function () { markActive(row, select.value); });
    markActive(row, select.value);
  }

  // Label moves inside the filled container (M3 text field anatomy)
  function floatLabels() {
    all(SEL.group).forEach(function (group) {
      var label = group.querySelector(':scope > .form-label');
      var control = group.querySelector(':scope > .form-control, :scope > .form-select:not(.qa-hidden-select)');
      if (!label || !control) return;
      var field = el('div', 'qa-field');
      control.parentNode.insertBefore(field, control);
      append(field, label, control);
    });
  }

  // Exposed dropdown menu: replaces the native picker
  var openMenuState = null;

  function closeMenu() {
    if (!openMenuState) return;
    openMenuState.host.classList.remove('open');
    openMenuState.nodes.forEach(function (node) { node.remove(); });
    openMenuState = null;
  }

  function placeMenu(menu, rect) {
    var below = window.innerHeight - rect.bottom - MENU.margin;
    var above = rect.top - MENU.margin;
    var up = below < MENU.minSpace && above > below;
    menu.classList.toggle('up', up);
    menu.style.left = rect.left + 'px';
    menu.style.width = rect.width + 'px';
    menu.style.maxHeight = Math.min(MENU.maxHeight, up ? above : below) + 'px';
    if (up) menu.style.bottom = (window.innerHeight - rect.top + MENU.gap) + 'px';
    else menu.style.top = (rect.bottom + MENU.gap) + 'px';
  }

  function renderMenuItems(menu, select) {
    var selected = null;
    all('option', select).forEach(function (option) {
      if (option.disabled || option.value === '') return;
      var item = button('qa-menu-item', option.textContent);
      item.setAttribute('role', 'option');
      if (option.value === select.value) {
        item.classList.add('on');
        selected = item;
      }
      item.addEventListener('click', function () {
        setValue(select, option.value);
        dismissMenu();
      });
      menu.appendChild(item);
    });
    return selected;
  }

  function dismissMenu() { Layers.close(closeMenu); }

  function openMenu(select, host) {
    dismissMenu();
    var menu = el('div', 'qa-menu');
    menu.setAttribute('role', 'listbox');
    var selected = renderMenuItems(menu, select);
    if (!menu.children.length) return;

    placeMenu(menu, host.getBoundingClientRect());
    var scrim = el('div', 'qa-scrim');
    scrim.addEventListener('click', dismissMenu);
    append(document.body, scrim, menu);
    host.classList.add('open');
    openMenuState = { host: host, nodes: [scrim, menu] };
    Layers.open(closeMenu);
    if (selected) menu.scrollTop = selected.offsetTop - (menu.clientHeight - selected.offsetHeight) / 2;
  }

  function enhanceSelects() {
    all('.form-select:not(.qa-hidden-select)').forEach(function (select) {
      var host = select.closest('.qa-field');
      if (!host) {
        host = el('div', 'qa-selectwrap');
        select.parentNode.insertBefore(host, select);
        host.appendChild(select);
      }
      host.classList.add('qa-select');
      host.addEventListener('click', function () {
        if (!select.disabled) openMenu(select, host);
      });
    });
  }

  // Form defaults
  function setNow() {
    var now = new Date();
    setValue($('date'), now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()));
    setValue($('hour'), pad(now.getHours()));
    setValue($('minute'), pad(now.getMinutes()));
  }

  function restoreAuditor() {
    var prefs = Prefs.load();
    AUDITOR_FIELDS.forEach(function (id) {
      var field = $(id);
      if (prefs[id]) setValue(field, prefs[id]);
      field.addEventListener('change', function () { Prefs.update(id, this.value); });
    });
  }

  // Tonal "now" action in the 稽核時間 label row
  function addNowButton() {
    var date = $('date');
    var group = date && date.closest(SEL.inputGroup);
    var label = group && group.previousElementSibling;
    if (!label) return;
    var row = el('div', 'qa-label-row');
    var now = button('qa-now');
    now.innerHTML = CLOCK_ICON + '<span>' + TEXT.now + '</span>';
    now.addEventListener('click', setNow);
    label.parentNode.insertBefore(row, label);
    append(row, label, now);
  }

  // Auditor account: avatar + modal bottom sheet (fields stay in the form: validation + submit)
  var accountSheet = null;

  function buildAvatar(host) {
    var face = el('span');
    var badge = el('span', 'qa-badge');
    var avatar = append(button('qa-avatar qa-avatar-btn'), face, badge);
    avatar.setAttribute('aria-label', TEXT.auditor);
    host.appendChild(avatar);
    return { button: avatar, face: face, badge: badge };
  }

  function buildSheet(section) {
    var face = el('div', 'qa-avatar qa-avatar-lg');
    var title = el('div', 'qa-sheet-title');
    var subtitle = el('div', 'qa-sheet-subtitle');
    var done = button('qa-sheet-done', TEXT.done);
    var header = append(el('div', 'qa-sheet-header'), face, append(el('div'), title, subtitle));
    var repo = button('qa-repo');
    repo.innerHTML = GITHUB_ICON + '<span>' + TEXT.repo + '</span>';
    repo.addEventListener('click', function () { callNative('openRepo'); });
    section.insertBefore(header, section.firstChild);
    append(section, done, repo);
    section.classList.add('qa-sheet');
    return { face: face, title: title, subtitle: subtitle, done: done };
  }

  function setupAccount() {
    var unit = $('auditorUnit');
    var name = $('auditorName');
    var email = $('auditorEmail');
    var section = name && name.closest('fieldset');
    var headline = document.querySelector(SEL.headline);
    if (!unit || !email || !section || !headline) return;
    var controls = [unit, name, email];

    var avatar = buildAvatar(headline);
    var sheet = buildSheet(section);
    var scrim = el('div', 'qa-scrim qa-sheet-scrim');
    document.body.appendChild(scrim);

    function hideSheet() {
      section.classList.remove('open');
      scrim.classList.remove('open');
    }

    function showSheet() {
      if (section.classList.contains('open')) return;
      section.classList.add('open');
      scrim.classList.add('open');
      Layers.open(hideSheet);
    }

    function dismissSheet() { Layers.close(hideSheet); }

    function syncIdentity() {
      var first = Array.from(name.value.trim())[0];
      [avatar.face, sheet.face].forEach(function (face) {
        if (first) face.textContent = first;
        else face.innerHTML = PERSON_ICON;
      });
      sheet.title.textContent = name.value.trim() || TEXT.auditorEmpty;
      sheet.subtitle.textContent = [unit.value, email.value.trim()].filter(Boolean).join(' · ') || TEXT.auditorHint;
    }

    function syncCount() {
      var match = /\d+/.exec(($('count') || {}).textContent || '');
      avatar.badge.textContent = match ? match[0] : '';
    }

    avatar.button.addEventListener('click', showSheet);
    scrim.addEventListener('click', dismissSheet);
    sheet.done.addEventListener('click', function () {
      // Evaluate all: every invalid control gets its error state
      var valid = controls.map(function (control) { return control.checkValidity(); }).every(Boolean);
      if (valid) dismissSheet();
    });
    controls.forEach(function (control) { onChange(control, syncIdentity); });

    var count = $('count');
    if (count) observeContent(count, syncCount);

    syncIdentity();
    syncCount();
    accountSheet = { section: section, open: showSheet };
    if (!controls.every(function (control) { return control.value.trim() !== ''; })) showSheet();
  }

  // Roster picker
  function setupRoster() {
    var roster = parseJson(callNative('roster'), {});
    var nameInput = $('auditedName');
    var title = $('title');
    var unit = $('auditedUnit');
    var other = $('unitOther');
    if (!nameInput || !title || !unit) return;

    var datalist = el('datalist');
    datalist.id = 'qa-names';
    document.body.appendChild(datalist);
    nameInput.setAttribute('list', datalist.id);
    nameInput.setAttribute('autocomplete', 'off');

    var nameRow = insertChipRow(nameInput);
    var deptRow = other ? insertChipRow(other) : null;
    var pickedName = '';  // chip-filled only: auto-cleared on selection change

    function isDoctor() { return title.value === '醫師'; }

    // 其他 + free text -> roster key
    function deptKey() {
      return unit.value === '其他' ? (other ? other.value.trim() : '') : unit.value;
    }

    // Unit options rebuilt per title: evaluate per render
    function rosterOnlyDepts() {
      var known = {};
      all('option', unit).forEach(function (o) { known[o.value] = true; });
      return Object.keys(roster).filter(function (k) { return !known[k] && roster[k].length; });
    }

    function renderDepts() {
      if (!deptRow) return;
      var depts = isDoctor() && unit.value === '其他' ? rosterOnlyDepts() : [];
      renderChips(deptRow, depts, function (dept) {
        setValue(other, other.value.trim() === dept ? '' : dept);
      });
      markActive(deptRow, other.value.trim());
    }

    function renderNames(names) {
      renderChips(nameRow, names.length <= CHIP_LIMIT ? names : [], function (name) {
        pickedName = nameInput.value === name ? '' : name;
        setValue(nameInput, pickedName);
      });
      markActive(nameRow, nameInput.value);
    }

    function refresh() {
      var names = isDoctor() ? roster[deptKey()] || [] : [];
      if (pickedName && nameInput.value === pickedName && names.indexOf(pickedName) < 0) {
        setValue(nameInput, '');
        pickedName = '';
      }
      datalist.textContent = '';
      names.forEach(function (name) {
        var option = el('option');
        option.value = name;
        datalist.appendChild(option);
      });
      renderNames(names);
      renderDepts();
    }

    if (other) other.addEventListener('input', refresh);
    title.addEventListener('change', refresh);
    unit.addEventListener('change', refresh);
    nameInput.addEventListener('input', function () { markActive(nameRow, nameInput.value); });
    refresh();
  }

  // Validation feedback: M3 error state
  function groupOf(control) { return control.closest(SEL.group) || control.parentNode; }

  function errorText(control) {
    if (control.validity.valueMissing) return control.tagName === 'SELECT' ? TEXT.errorSelect : TEXT.errorMissing;
    return control.validationMessage || TEXT.errorFormat;
  }

  function renderErrorGroup(group) {
    var invalid = group.querySelectorAll('[data-qa-invalid]');
    var message = group.querySelector('.qa-error');
    group.classList.toggle('qa-invalid', invalid.length > 0);
    if (!invalid.length) {
      if (message) message.remove();
      return;
    }
    if (!message) message = group.appendChild(el('div', 'qa-error'));
    message.textContent = errorText(invalid[0]);
  }

  function setupValidationFeedback() {
    var form = $('form');

    // Capture: 'invalid' does not bubble
    form.addEventListener('invalid', function (e) {
      if (accountSheet && accountSheet.section.contains(e.target)) accountSheet.open();
      e.target.setAttribute('data-qa-invalid', '');
      renderErrorGroup(groupOf(e.target));
    }, true);

    // Deferred: page handlers toggle required flags after the event
    function sweep() {
      all('[data-qa-invalid]', form).forEach(function (control) {
        if (!control.validity.valid) return;
        control.removeAttribute('data-qa-invalid');
        renderErrorGroup(groupOf(control));
      });
    }
    ['input', 'change'].forEach(function (type) {
      form.addEventListener(type, function () { setTimeout(sweep, 0); }, true);
    });

    // Page notice references removed asterisks
    var body = $('modalBody');
    if (!body) return;
    observeContent(body, function () {
      if (body.textContent.indexOf('紅色星號') >= 0) body.textContent = TEXT.requiredNotice;
    });
  }

  // Bootstrap
  var STEPS = [
    applyTheme, rewriteHeadline, stripSectionNumbers, restoreAuditor, setNow,
    function () { CHIP_FIELDS.forEach(chipifySelect); },
    setupRoster, floatLabels, enhanceSelects, setupAccount, setupValidationFeedback, addNowButton
  ];

  function init() {
    STEPS.forEach(function (step) {
      try { step(); } catch (e) { console.error('[quick-audit]', e); }
    });
    // Splash dismissal: after first styled paint
    setTimeout(function () { callNative('ready'); }, READY_DELAY_MS);
  }

  function formReady() {
    var unit = $('auditorUnit');
    return !!$('form') && !!unit && unit.options.length > 1;
  }

  // MutationObserver: document.write drops DOMContentLoaded listeners
  // Polling fallback: observer loss
  var started = false;
  var tries = 0;
  var observer = new MutationObserver(start);
  var timer = setInterval(poll, POLL_INTERVAL_MS);

  function start() {
    if (started || !formReady()) return;
    started = true;
    stopWatching();
    init();
  }

  function poll() {
    start();
    if (++tries > POLL_MAX_TRIES) stopWatching();
  }

  function stopWatching() {
    observer.disconnect();
    clearInterval(timer);
  }

  observer.observe(document, { childList: true, subtree: true });
})();
