const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');
const { boot, wait, ROOT } = require('./support/harness');
const { NOTICE } = require('./support/page-shim');

const ROSTER = {
  骨科: ['醫師甲', '醫師乙'],
  小兒科: ['醫師丙', '醫師丁'],
  麻醉科: ['醫師戊', '醫師己'],
  大科: Array.from({ length: 13 }, (_, i) => '醫師' + i)
};
const AUDITOR = { auditorName: '測試員', auditorEmail: 'a@b.co', auditorUnit: 'W8' };
const texts = (nodes) => nodes.map((n) => n.textContent);

test('boot: styles, headline, ordinals, ready signal', async () => {
  const t = await boot({ prefs: AUDITOR, themeCss: ':root{--m3-primary:#fff}' });
  assert.ok(t.document.getElementById('qa-m3-style'));
  assert.deepStrictEqual(texts(t.$$('.py-2 h3 span')), ['大風吹', '吹什麼 吹沒洗手的人']);
  assert.deepStrictEqual(texts(t.$$('fieldset legend')).map((s) => s.trim()), ['稽核人資訊', '稽核內容', '受稽核人員資訊']);
  assert.strictEqual(t.calls.ready, 1);
  t.close();
});

test('chips: grid for long lists, segmented for three or fewer, selection syncs', async () => {
  const t = await boot({ prefs: AUDITOR });
  const rows = t.$$('.qa-chips');
  const timing = t.$('timing').parentNode.querySelector('.qa-chips');
  const action = t.$('action').parentNode.querySelector('.qa-chips');
  assert.strictEqual(timing.children.length, 5);
  assert.ok(!timing.classList.contains('qa-segmented'));
  assert.ok(action.classList.contains('qa-segmented'));
  assert.ok(rows.length >= 4);
  timing.children[0].click();
  assert.strictEqual(t.$('timing').value, '接觸病人前');
  assert.ok(timing.children[0].classList.contains('on'));
  t.pick('timing', '接觸病人後');
  assert.ok(timing.children[3].classList.contains('on'));
  assert.ok(!timing.children[0].classList.contains('on'));
  t.close();
});

test('defaults: current time filled, now button resets it', async () => {
  const t = await boot({ prefs: AUDITOR });
  assert.match(t.$('auditDate').value, /^\d{4}-\d{2}-\d{2}$/);
  t.pick('auditHour', '03');
  t.pick('auditMinute', '07');
  const now = t.document.querySelector('.qa-label-row .qa-now');
  assert.ok(now);
  now.click();
  const expected = new Date();
  assert.strictEqual(t.$('auditHour').value, String(expected.getHours()).padStart(2, '0'));
  t.close();
});

test('auditor: restored from prefs, persisted on change, sheet auto-opens when incomplete', async () => {
  const filled = await boot({ prefs: AUDITOR });
  assert.strictEqual(filled.$('auditorName').value, '測試員');
  assert.ok(!filled.document.querySelector('.qa-sheet').classList.contains('open'));
  filled.pick('auditorName', '新名字');
  assert.strictEqual(JSON.parse(filled.store.prefs).auditorName, '新名字');
  assert.strictEqual(filled.document.querySelector('.qa-avatar-btn span').textContent, '新');
  filled.close();

  const empty = await boot();
  assert.ok(empty.document.querySelector('.qa-sheet').classList.contains('open'));
  empty.close();
});

test('account sheet: done keeps it open while invalid, closes when valid', async () => {
  const t = await boot();
  const sheet = t.document.querySelector('.qa-sheet');
  t.document.querySelector('.qa-sheet-done').click();
  assert.ok(sheet.classList.contains('open'));
  assert.ok(t.$('auditorName').hasAttribute('data-qa-invalid'));
  t.pick('auditorUnit', 'W8');
  t.pick('auditorName', '測試員');
  t.pick('auditorEmail', 'a@b.co');
  t.document.querySelector('.qa-sheet-done').click();
  assert.ok(!sheet.classList.contains('open'));
  t.close();
});

test('account sheet: count badge follows the page counter', async () => {
  const t = await boot({ prefs: AUDITOR });
  t.$('auditCount').textContent = '您本月已稽核 7 筆資料。';
  await wait(20);
  assert.strictEqual(t.document.querySelector('.qa-badge').textContent, '7');
  t.close();
});

test('source link: calls the native bridge', async () => {
  const t = await boot({ prefs: AUDITOR });
  t.document.querySelector('.qa-repo').click();
  assert.strictEqual(t.calls.openRepo, 1);
  t.close();
});

test('roster: chips per department, stale pick cleared, other-department flow, long lists skipped', async () => {
  const t = await boot({ prefs: AUDITOR, roster: ROSTER });
  const nameRow = () => t.$('auditedName').closest('.qa-field').previousElementSibling;
  const deptRow = () => t.$('auditedUnitDropdownOther').closest('.qa-field').previousElementSibling;

  t.pick('title', '醫師');
  t.pick('auditedUnit', '小兒科');
  assert.deepStrictEqual(texts(Array.from(nameRow().children)), ['醫師丙', '醫師丁']);
  nameRow().children[0].click();
  assert.strictEqual(t.$('auditedName').value, '醫師丙');
  assert.ok(nameRow().children[0].classList.contains('on'));

  t.pick('auditedUnit', '骨科');
  assert.strictEqual(t.$('auditedName').value, '');
  assert.deepStrictEqual(texts(Array.from(nameRow().children)), ['醫師甲', '醫師乙']);

  t.pick('auditedUnit', '其他');
  assert.deepStrictEqual(texts(Array.from(deptRow().children)).sort(), ['大科', '麻醉科']);
  Array.from(deptRow().children).find((b) => b.textContent === '麻醉科').click();
  assert.strictEqual(t.$('auditedUnitDropdownOther').value, '麻醉科');
  assert.deepStrictEqual(texts(Array.from(nameRow().children)), ['醫師戊', '醫師己']);

  Array.from(deptRow().children).find((b) => b.textContent === '大科').click();
  assert.strictEqual(nameRow().children.length, 0);

  t.pick('title', '護理師');
  assert.strictEqual(nameRow().children.length, 0);
  t.close();
});

test('validation: M3 errors per group, cleared on fix, notice rewritten', async () => {
  const t = await boot({ prefs: AUDITOR });
  t.$('submitButton').click();
  await wait(20);
  const errors = () => texts(t.$$('.qa-error'));
  assert.ok(errors().includes('請選擇一個選項'));
  assert.ok(t.$('timing').closest('.mb-3').classList.contains('qa-invalid'));
  assert.ok(!t.document.getElementById('modalMessageBody').textContent.includes('紅色星號'));
  assert.strictEqual(t.document.getElementById('modalMessageBody').textContent, '請檢查標示為錯誤的欄位。');
  t.pick('timing', '接觸病人前');
  await wait(20);
  assert.ok(!t.$('timing').closest('.mb-3').classList.contains('qa-invalid'));
  assert.ok(NOTICE.includes('紅色星號'));
  t.close();
});

test('dropdown menu: opens from the field, selects a value, closes', async () => {
  const t = await boot({ prefs: AUDITOR });
  const host = t.$('auditHour').closest('.qa-select');
  host.click();
  const items = t.$$('.qa-menu .qa-menu-item');
  assert.strictEqual(items.length, 24);
  items[5].click();
  assert.strictEqual(t.$('auditHour').value, '05');
  assert.strictEqual(t.$$('.qa-menu').length, 0);
  await wait(30);
  t.close();
});

test('back key: closes the menu, then the sheet, one layer at a time', async () => {
  const t = await boot();
  const sheet = t.document.querySelector('.qa-sheet');
  assert.ok(sheet.classList.contains('open'));
  assert.strictEqual(t.bridge.messages.at(-1), '1');

  t.$('auditorUnit').closest('.qa-select').click();
  assert.strictEqual(t.$$('.qa-menu').length, 1);
  assert.strictEqual(t.bridge.messages.at(-1), '2');

  t.back();
  assert.strictEqual(t.$$('.qa-menu').length, 0);
  assert.ok(sheet.classList.contains('open'));
  assert.strictEqual(t.bridge.messages.at(-1), '1');

  t.back();
  assert.ok(!sheet.classList.contains('open'));
  assert.strictEqual(t.bridge.messages.at(-1), '0');
  t.close();
});

test('back key: closing through the UI keeps the reported depth in step', async () => {
  const t = await boot();
  const sheet = t.document.querySelector('.qa-sheet');
  t.$('auditorUnit').closest('.qa-select').click();
  t.$$('.qa-menu .qa-menu-item')[0].click();
  assert.ok(sheet.classList.contains('open'));
  assert.strictEqual(t.bridge.messages.at(-1), '1');
  t.pick('auditorName', '測試員');
  t.pick('auditorEmail', 'a@b.co');
  t.document.querySelector('.qa-sheet-done').click();
  assert.ok(!sheet.classList.contains('open'));
  assert.strictEqual(t.bridge.messages.at(-1), '0');
  t.back();
  assert.strictEqual(t.bridge.messages.at(-1), '0');
  t.close();
});

const ROSTER_FILE = path.join(ROOT, 'roster.json');  // untracked, local only

test('roster.json: valid shape, no duplicate names per department', { skip: !fs.existsSync(ROSTER_FILE) }, () => {
  const roster = JSON.parse(fs.readFileSync(ROSTER_FILE, 'utf8'));
  Object.entries(roster).forEach(([dept, names]) => {
    assert.ok(Array.isArray(names), dept);
    assert.strictEqual(new Set(names).size, names.length, dept);
    names.forEach((name) => assert.ok(typeof name === 'string' && name.trim() === name && name, dept));
  });
});
