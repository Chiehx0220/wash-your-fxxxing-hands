// Stand-in for the original page script: option population, conditional required flags, submit validation
const OPTIONS = {
  units: ['W8', 'W9', '10B'],
  titles: ['醫師', '護理師', '專科護理師', '其他'],
  timings: ['接觸病人前', '執行清潔/無菌操作技術前', '暴觸病人體液風險後', '接觸病人後', '接觸病人周遭環境後'],
  actions: ['乾洗手', '濕洗手', '沒有洗手'],
  steps: ['確實', '未確實'],
  internalMedDepts: ['心臟內科', '腎臟內科'],
  surgeryDepts: ['骨科', '一般外科'],
  otherSpecDepts: ['小兒科', '其他']
};

const NOTICE = '請檢查所有紅色星號 (*) 的必填欄位。';

function install(window) {
  const document = window.document;
  const $ = (id) => document.getElementById(id);

  function fill(select, items, groupLabel) {
    let target = select;
    if (groupLabel) {
      target = document.createElement('optgroup');
      target.label = groupLabel;
      select.appendChild(target);
    }
    items.forEach((item) => target.appendChild(new window.Option(item, item)));
  }

  function resetSelect(select, placeholder) {
    select.innerHTML = '';
    const option = new window.Option(placeholder, '');
    option.disabled = true;
    option.selected = true;
    select.appendChild(option);
  }

  function updateAuditedUnit(title) {
    const select = $('auditedUnit');
    resetSelect(select, '-- 請選擇 --');
    if (title === '醫師') {
      fill(select, OPTIONS.internalMedDepts, '內科科別');
      fill(select, OPTIONS.surgeryDepts, '外科科別');
      fill(select, OPTIONS.otherSpecDepts, '其他專科科別');
      select.disabled = false;
    } else if (title === '護理師') {
      fill(select, OPTIONS.units);
      select.disabled = false;
    } else {
      resetSelect(select, '-- 請先選擇職稱 --');
      select.disabled = true;
    }
  }

  function conditional() {
    const action = $('action').value;
    const showStep = action === '乾洗手' || action === '濕洗手';
    $('stepWrapper').style.display = showStep ? 'block' : 'none';
    updateAuditedUnit($('title').value);
    $('auditedUnitDropdownOtherWrapper').style.display = $('auditedUnit').value === '其他' ? 'block' : 'none';
    $('step').required = showStep;
    $('auditedUnitDropdownOther').required = $('auditedUnit').value === '其他';
    const failed = (showStep && $('step').value === '未確實') || action === '沒有洗手';
    $('auditedName').required = failed && $('title').value === '醫師';
  }

  fill($('auditorUnit'), OPTIONS.units);
  fill($('timing'), OPTIONS.timings);
  fill($('action'), OPTIONS.actions);
  fill($('step'), OPTIONS.steps);
  fill($('title'), OPTIONS.titles);
  for (let i = 0; i <= 23; i++) fill($('auditHour'), [String(i).padStart(2, '0')]);
  for (let i = 0; i <= 59; i++) fill($('auditMinute'), [String(i).padStart(2, '0')]);

  ['title', 'action', 'step'].forEach((id) => $(id).addEventListener('change', conditional));
  $('auditedUnit').addEventListener('change', () => {
    $('auditedUnitDropdownOtherWrapper').style.display = $('auditedUnit').value === '其他' ? 'block' : 'none';
    $('auditedUnitDropdownOther').required = $('auditedUnit').value === '其他';
  });

  $('mainForm').addEventListener('submit', (e) => {
    e.preventDefault();
    if (!$('mainForm').reportValidity()) $('modalMessageBody').textContent = NOTICE;
  });
}

module.exports = { install, OPTIONS, NOTICE };
