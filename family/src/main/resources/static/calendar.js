/* 근무 캘린더 (설계 §5-1)
 * - 처음: 근무 방식(2·3·4교대·직접) → 교대 확인 → 순환 패턴으로 채우기
 * - 입력: 칩(붓)을 고르고 날짜를 탭하면 칠해짐. 칩 없이 탭하면 바텀시트
 * - 탭마다 바로 저장 + 되돌리기 토스트. 연결이 끊기면 모아 뒀다가 다시 보냄
 */
document.addEventListener('DOMContentLoaded', () => {
  const { api, t, errText, saved, failed, toast, fmtTime, fmtRange } = window.App;
  const main = document.querySelector('[data-cal]');
  const body = main.querySelector('[data-body]');
  const TODAY = main.dataset.today;
  const TARGET = main.dataset.target ? +main.dataset.target : null;
  const COLORS = ['blue', 'green', 'orange', 'purple', 'red', 'sky', 'amber', 'gray'];
  const font = () => document.documentElement.dataset.font;
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  const st = {
    month: TODAY.slice(0, 7),
    view: new URLSearchParams(location.search).get('view') === 'family' ? 'family' : 'me',
    people: [],
    brush: null,          // typeId | 'erase' | null
    pending: new Map(),   // day → payload (오프라인 대기)
  };
  const person = () => st.people[0];
  const liveTypes = () => person().types.filter(x => !x.deleted);
  const typeOf = (p, id) => p.types.find(x => x.id === id);
  const dayOf = (p, d) => p.days.find(x => x.day === d);
  const withTarget = o => (TARGET ? { ...o, memberId: TARGET } : o);
  const tq = TARGET ? `?memberId=${TARGET}` : '';

  // ---- 날짜 계산 (문자열 'yyyy-MM-dd' 만, 시간대 영향 없게 UTC 로)
  const toDate = s => new Date(s + 'T00:00:00Z');
  const ymd = d => d.toISOString().slice(0, 10);
  const addDays = (s, n) => { const d = toDate(s); d.setUTCDate(d.getUTCDate() + n); return ymd(d); };
  const addMonths = (ym, n) => { const d = toDate(ym + '-01'); d.setUTCMonth(d.getUTCMonth() + n); return ymd(d).slice(0, 7); };
  const lang = document.documentElement.lang;
  const monthTitle = ym => new Intl.DateTimeFormat(lang === 'th' ? 'th-TH-u-ca-gregory' : 'ko-KR', { year: 'numeric', month: 'long', timeZone: 'UTC' }).format(toDate(ym + '-01'));
  const dayTitle = s => new Intl.DateTimeFormat(lang === 'th' ? 'th-TH-u-ca-gregory' : 'ko-KR', { month: 'long', day: 'numeric', weekday: font() === 'lg' ? 'long' : 'short', timeZone: 'UTC' }).format(toDate(s));
  const weekdays = [...Array(7)].map((_, i) => new Intl.DateTimeFormat(lang === 'th' ? 'th-TH' : 'ko-KR', { weekday: 'narrow', timeZone: 'UTC' }).format(toDate('2023-01-0' + (i + 1))));

  async function load() {
    const scope = st.view === 'family' ? 'family' : 'me';
    const r = await api('GET', `/api/shifts?month=${st.month}&scope=${scope}` + (TARGET && scope === 'me' ? `&memberId=${TARGET}` : ''));
    if (!r.ok) { body.innerHTML = `<p class="py-16 text-center text-muted">${esc(errText(r))}</p>`; return; }
    st.people = r.json.people;
    render();
  }

  function render() {
    if (st.view === 'me' && person() && !person().setting && person().days.length === 0) return person().editable ? renderSetup() : renderEmpty();
    body.innerHTML = `
      ${st.view === 'me' && person()?.editable ? brushBar() : ''}
      <div class="flex items-center justify-between">
        <button type="button" data-nav="-1" class="w-12 h-12 -ml-2 rounded-full text-2xl" aria-label="prev">‹</button>
        <div class="text-center"><p class="text-lg font-bold">${esc(monthTitle(st.month))}</p></div>
        <button type="button" data-nav="1" class="w-12 h-12 -mr-2 rounded-full text-2xl" aria-label="next">›</button>
      </div>
      <div class="grid grid-cols-7 text-center text-sm text-muted mt-1">${weekdays.map((w, i) => `<span class="py-1 ${i === 0 ? 'text-red-500' : ''}">${w}</span>`).join('')}</div>
      <div data-grid class="grid grid-cols-7 gap-1"></div>
      <div class="mt-3 flex justify-between items-center">
        <button type="button" data-today class="btn-soft px-4 text-sm">${esc(t('cal.today'))}</button>
        ${st.view === 'me' && person()?.editable ? `<button type="button" data-settings class="btn-soft px-4 text-sm">${esc(t('cal.settings'))}</button>` : ''}
      </div>
      ${st.view === 'family' ? legend() : ''}`;
    renderGrid();
  }

  function cells() {
    const first = toDate(st.month + '-01');
    const start = addDays(st.month + '-01', -first.getUTCDay());
    return [...Array(42)].map((_, i) => addDays(start, i));
  }

  function renderGrid() {
    const grid = body.querySelector('[data-grid]');
    grid.innerHTML = cells().map(d => st.view === 'family' ? familyCell(d) : meCell(d)).join('');
  }

  function meCell(d) {
    const p = person(), sd = dayOf(p, d), ty = sd && sd.typeId ? typeOf(p, sd.typeId) : null;
    const out = d.slice(0, 7) !== st.month, today = d === TODAY, num = +d.slice(8);
    const big = font() === 'lg';
    return `<button type="button" data-day="${d}" class="relative ${big ? 'h-[3.6rem]' : 'h-[4.2rem]'} font-sm:h-16 rounded-xl flex flex-col items-center pt-1 ${ty ? 'tint c-' + ty.color : 'bg-card'} ${out ? 'opacity-35' : ''} ${today ? 'ring-2 ring-fg' : ''} ${st.pending.has(d) ? 'outline-dashed outline-1 outline-accent' : ''}">
      <span class="text-xs ${today ? 'font-bold' : ''} ${new Date(d + 'T00:00:00Z').getUTCDay() === 0 ? 'text-red-500' : ''}">${num}</span>
      ${ty ? `<span class="font-bold leading-none mt-0.5 ${big ? 'text-2xl' : 'text-lg'}">${esc(ty.shortName)}</span>` : ''}
      ${ty && ty.startTime ? `<span class="hidden font-sm:block text-[10px] leading-tight mt-0.5 opacity-80">${esc(ty.startTime)}</span>` : ''}
      ${sd && sd.note ? `<span class="absolute bottom-1 right-1.5 w-1.5 h-1.5 rounded-full bg-accent"></span>` : ''}
    </button>`;
  }

  function familyCell(d) {
    const out = d.slice(0, 7) !== st.month, today = d === TODAY;
    const marks = st.people.map(p => {
      const sd = dayOf(p, d), ty = sd && sd.typeId ? typeOf(p, sd.typeId) : null;
      if (!ty && !(sd && sd.note)) return '';
      return `<span class="c-${p.color} solid w-full rounded text-[10px] font-lg:text-xs leading-tight font-bold truncate px-0.5">${esc(ty ? ty.shortName : '•')}</span>`;
    }).join('');
    return `<button type="button" data-day="${d}" class="h-[4.6rem] rounded-xl bg-card flex flex-col items-center pt-1 px-0.5 gap-0.5 overflow-hidden ${out ? 'opacity-35' : ''} ${today ? 'ring-2 ring-fg' : ''}">
      <span class="text-xs ${today ? 'font-bold' : ''}">${+d.slice(8)}</span>${marks}</button>`;
  }

  function legend() {
    if (!st.people.length) return `<p class="mt-6 text-center text-muted">${esc(t('cal.familyEmpty'))}</p>`;
    return `<div class="mt-4 flex flex-wrap gap-3">${st.people.map(p => `<span class="inline-flex items-center gap-1.5 text-sm"><span class="c-${p.color} solid w-3 h-3 rounded-full"></span>${esc(p.name)}</span>`).join('')}</div>`;
  }

  // ---- 붓(칩)
  function brushBar() {
    const chips = liveTypes().map(x => `<button type="button" data-brush="${x.id}" aria-pressed="${st.brush === x.id}"
        class="shrink-0 min-w-[3.2rem] font-lg:min-w-[4rem] h-tap px-3 rounded-2xl font-bold c-${x.color} ${st.brush === x.id ? 'solid ring-c' : 'tint'}">${esc(x.shortName)}<span class="font-normal ml-1 hidden sm:inline font-lg:hidden">${esc(x.name)}</span></button>`).join('');
    // 칩은 달력 위에 붙여 둔다(스크롤해도 보이고, 아래 토스트·탭과 안 겹침)
    return `<div class="sticky top-14 font-lg:top-16 z-10 -mx-4 px-4 py-2.5 mb-2 bg-bg/95 backdrop-blur border-b border-line/50">
      <p class="text-sm text-muted mb-2">${esc(st.brush ? t('cal.brushOn') : t('cal.brushHint'))}</p>
      <div class="flex gap-2 overflow-x-auto no-scrollbar p-1 -m-1">${chips}
        <button type="button" data-brush="erase" aria-pressed="${st.brush === 'erase'}" class="shrink-0 h-tap px-4 rounded-2xl border-2 ${st.brush === 'erase' ? 'border-fg' : 'border-line'} text-muted">${esc(t('cal.erase'))}</button>
      </div></div>`;
  }

  // ---- 저장: 낙관적 갱신 → PUT → 실패 처리
  async function setDay(d, typeId, note, { silent } = {}) {
    const p = person(), prev = dayOf(p, d) || { day: d, typeId: null, note: null };
    applyLocal(d, typeId, note);
    const payload = withTarget({ typeId, note });
    const r = await api('PUT', `/api/shifts/${d}`, payload);
    if (r.ok) {
      st.pending.delete(d); saved();
      if (!silent) {
        const ty = typeId ? typeOf(p, typeId) : null;
        toast(ty ? t('cal.changed', dayTitle(d), ty.name) : t('cal.cleared', dayTitle(d)), () => setDay(d, prev.typeId, prev.note, { silent: true }));
      }
    } else if (r.status === 0) {
      st.pending.set(d, payload); toast(t('cal.offline'));
    } else {
      applyLocal(d, prev.typeId, prev.note); failed();
    }
    renderGrid();
  }

  function applyLocal(d, typeId, note) {
    const p = person();
    p.days = p.days.filter(x => x.day !== d);
    if (typeId || note) p.days.push({ day: d, typeId, note: note || null });
  }

  async function flush() {
    for (const [d, payload] of [...st.pending]) {
      const r = await api('PUT', `/api/shifts/${d}`, payload);
      if (r.ok || (r.status >= 400 && r.status < 500)) st.pending.delete(d);
    }
    if (!st.pending.size) { saved(); renderGrid(); }
  }
  window.addEventListener('online', flush);
  setInterval(() => st.pending.size && flush(), 15000);

  // ---- 클릭
  body.addEventListener('click', e => {
    const el = e.target.closest('button');
    if (!el) return;
    if (el.dataset.nav) { st.month = addMonths(st.month, +el.dataset.nav); load(); return; }
    if (el.matches('[data-today]')) { st.month = TODAY.slice(0, 7); load(); return; }
    if (el.matches('[data-settings]')) { openSettings(); return; }
    if (el.dataset.brush) {
      const v = el.dataset.brush === 'erase' ? 'erase' : +el.dataset.brush;
      st.brush = st.brush === v ? null : v;
      render(); return;
    }
    if (el.dataset.system) { pickSystem(el.dataset.system); return; }
    if (el.dataset.day) {
      const d = el.dataset.day;
      if (st.view === 'family') return openFamilyDay(d);
      if (!person().editable) return;
      if (st.brush === 'erase') { const sd = dayOf(person(), d); if (sd) setDay(d, null, sd.note); return; }
      if (st.brush) { const sd = dayOf(person(), d); setDay(d, st.brush, sd ? sd.note : null); return; }
      openDay(d);
    }
  });
  // 좌우 스와이프로 달 이동
  let sx = null;
  body.addEventListener('touchstart', e => { sx = e.touches[0].clientX; }, { passive: true });
  body.addEventListener('touchend', e => {
    if (sx === null || !e.target.closest('[data-grid]')) return;
    const dx = e.changedTouches[0].clientX - sx; sx = null;
    if (Math.abs(dx) > 60) { st.month = addMonths(st.month, dx < 0 ? 1 : -1); load(); }
  }, { passive: true });

  document.querySelectorAll('[data-view]').forEach(b => b.addEventListener('click', () => {
    st.view = b.dataset.view; st.brush = null;
    document.querySelectorAll('[data-view]').forEach(x => x.setAttribute('aria-pressed', String(x === b)));
    load();
  }));
  if (st.view === 'family') document.querySelectorAll('[data-view]').forEach(x => x.setAttribute('aria-pressed', String(x.dataset.view === 'family')));
  const sel = document.querySelector('[data-member]');
  if (sel) sel.addEventListener('change', () => { location.href = '/calendar' + (sel.value ? `?member=${sel.value}` : ''); });

  // ---- 바텀시트
  function sheet(html, onClick) {
    const wrap = document.createElement('div');
    wrap.className = 'fixed inset-0 z-40 flex items-end justify-center';
    wrap.innerHTML = `<div data-close class="absolute inset-0 bg-black/40"></div>
      <div class="relative w-full max-w-xl max-h-[88dvh] overflow-y-auto bg-card rounded-t-3xl px-5 pt-3 pb-[calc(1.5rem+env(safe-area-inset-bottom))]">
        <div class="mx-auto mb-4 w-10 h-1.5 rounded-full bg-line"></div>${html}</div>`;
    document.body.appendChild(wrap);
    document.body.classList.add('overflow-hidden');
    const close = () => { wrap.remove(); document.body.classList.remove('overflow-hidden'); };
    wrap.addEventListener('click', e => { if (e.target.closest('[data-close]')) return close(); onClick && onClick(e, close, wrap); });
    return { wrap, close };
  }

  function openDay(d) {
    const sd = dayOf(person(), d) || {};
    const types = liveTypes();
    const { wrap } = sheet(`
      <h2 class="text-xl font-bold">${esc(dayTitle(d))}</h2>
      <div class="mt-4 grid grid-cols-2 gap-2">${types.map(x => `
        <button type="button" data-pick="${x.id}" class="min-h-[3.6rem] rounded-2xl px-4 text-left c-${x.color} ${sd.typeId === x.id ? 'solid' : 'tint'}">
          <span class="block font-bold">${esc(x.shortName)} · ${esc(x.name)}</span>
          ${x.startTime ? `<span class="block text-sm opacity-80">${esc(fmtRange(x.startTime, x.endTime))}</span>` : ''}
        </button>`).join('')}</div>
      <label class="label mt-5" for="note">${esc(t('cal.note'))}</label>
      <input id="note" data-note class="field" maxlength="30" value="${esc(sd.note || '')}" placeholder="${esc(t('cal.notePh'))}">
      <div class="mt-5 flex gap-2">
        <button type="button" data-pick="" class="btn-line flex-1">${esc(t('cal.erase'))}</button>
        <button type="button" data-close class="btn-soft flex-1">${esc(t('common.close'))}</button>
      </div>`, (e, close2) => {
      const b = e.target.closest('[data-pick]');
      if (!b) return;
      const note = wrap.querySelector('[data-note]').value.trim() || null;
      setDay(d, b.dataset.pick ? +b.dataset.pick : null, b.dataset.pick ? note : null);
      close2();
    });
    // 메모만 바꾸고 닫아도 저장
    const noteEl = wrap.querySelector('[data-note]');
    noteEl.addEventListener('change', () => {
      const cur = dayOf(person(), d) || {};
      const v = noteEl.value.trim() || null;
      if (v !== (cur.note || null)) setDay(d, cur.typeId || null, v, { silent: true });
    });
  }

  function openFamilyDay(d) {
    const rows = st.people.map(p => {
      const sd = dayOf(p, d), ty = sd && sd.typeId ? typeOf(p, sd.typeId) : null;
      if (!ty && !(sd && sd.note)) return '';
      return `<li class="flex items-center gap-3 py-3">
        <span class="avatar solid c-${p.color} w-10 h-10">${esc([...p.name][0])}</span>
        <div class="flex-1"><p class="font-semibold">${esc(p.name)}</p>
          <p>${ty ? `<span class="font-bold">${esc(ty.name)}</span> <span class="text-muted">${esc(fmtRange(ty.startTime, ty.endTime))}</span>` : ''}
          ${sd.note ? `<span class="block text-sm text-muted">${esc(sd.note)}</span>` : ''}</p></div></li>`;
    }).join('');
    sheet(`<h2 class="text-xl font-bold">${esc(dayTitle(d))}</h2>
      <ul class="mt-2 divide-y divide-line">${rows || `<li class="py-6 text-muted">${esc(t('cal.nobody'))}</li>`}</ul>
      <button type="button" data-close class="btn-soft w-full mt-4">${esc(t('common.close'))}</button>`);
  }

  // ---- 처음 설정
  function systemCards() {
    return ['2', '3', '4', 'custom'].map(s => `
      <button type="button" data-system="${s}" class="card p-5 text-left active:scale-[.98] transition">
        <span class="block text-xl font-bold">${esc(t('cal.sys.' + s))}</span>
        <span class="block mt-1 text-sm text-muted">${esc(t('cal.sysDesc.' + s))}</span>
      </button>`).join('');
  }

  function renderEmpty() {
    body.innerHTML = `<p class="py-16 text-center text-muted">${esc(t('cal.noneYet'))}</p>`;
  }

  function renderSetup() {
    body.innerHTML = `
      <div class="py-4">
        <p class="text-sm font-semibold text-accent">1 / 3</p>
        <h2 class="mt-1 text-2xl font-bold">${esc(t('cal.setupTitle'))}</h2>
        <p class="mt-2 text-muted">${esc(t('cal.setupDesc'))}</p>
        <div class="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-3">${systemCards()}</div>
      </div>`;
  }

  async function pickSystem(system) {
    const r = await api('POST', '/api/shift-setting/preset', withTarget({ system }));
    if (!r.ok) return toast(errText(r));
    await load();
    openSettings(true);
  }

  // ---- 근무 설정 시트: 교대 목록 · 순환 패턴 · 채우기 · 방식 바꾸기
  function openSettings(wizard) {
    let stage = wizard ? 2 : 0;
    const { wrap } = sheet('<div data-settings-body></div>', onClick);
    const box = wrap.querySelector('[data-settings-body]');

    function draw() {
      const p = person(), types = liveTypes(), pattern = (p.setting?.pattern || []).map(id => typeOf(p, id)).filter(Boolean);
      const startDefault = p.setting?.patternStart || TODAY;
      box.innerHTML = `
        ${stage ? `<p class="text-sm font-semibold text-accent">${stage} / 3</p>` : ''}
        <section ${stage === 3 ? 'hidden' : ''}>
          <h2 class="text-xl font-bold">${esc(t('cal.types'))}</h2>
          <p class="mt-1 text-sm text-muted">${esc(t('cal.typesDesc'))}</p>
          <ul class="mt-4 space-y-3">${types.map(typeRow).join('')}</ul>
          <button type="button" data-add-type class="btn-line w-full mt-3">＋ ${esc(t('cal.addType'))}</button>
          ${stage === 2 ? `<button type="button" data-next class="btn-primary w-full mt-6">${esc(t('common.next'))}</button>` : ''}
        </section>
        <section class="${stage === 2 ? 'hidden' : stage === 3 ? '' : 'mt-8 pt-6 border-t border-line'}">
          <h2 class="text-xl font-bold">${esc(t('cal.pattern'))}</h2>
          <p class="mt-1 text-sm text-muted">${esc(t('cal.patternDesc'))}</p>
          <div class="mt-3 min-h-[3.4rem] p-2 rounded-2xl bg-soft flex flex-wrap gap-1.5">${pattern.length ? pattern.map((x, i) =>
            `<button type="button" data-pat-remove="${i}" class="w-10 h-10 font-lg:w-12 font-lg:h-12 rounded-xl font-bold solid c-${x.color}">${esc(x.shortName)}</button>`).join('')
            : `<span class="p-2 text-muted text-sm">${esc(t('cal.patternEmpty'))}</span>`}</div>
          <p class="mt-3 text-sm text-muted">${esc(t('cal.patternAdd'))}</p>
          <div class="mt-2 flex flex-wrap gap-2">${types.map(x =>
            `<button type="button" data-pat-add="${x.id}" class="h-tap px-4 rounded-2xl font-bold tint c-${x.color}">${esc(x.shortName)}</button>`).join('')}
            <button type="button" data-pat-clear class="h-tap px-4 rounded-2xl border border-line text-muted">${esc(t('cal.patternClear'))}</button></div>
          <div class="mt-5 grid grid-cols-2 gap-3">
            <div><label class="label" for="pstart">${esc(t('cal.patternStart'))}</label><input id="pstart" data-pstart type="date" class="field" value="${startDefault}"></div>
            <div><label class="label" for="pmonths">${esc(t('cal.patternMonths'))}</label>
              <select id="pmonths" data-pmonths class="field">${[1, 3, 6, 12].map(m => `<option value="${m}" ${m === 3 ? 'selected' : ''}>${esc(t('cal.months', m))}</option>`).join('')}</select></div>
          </div>
          <p class="mt-2 text-sm text-muted">${esc(t('cal.patternStartHint'))}</p>
          <button type="button" data-fill class="btn-primary w-full mt-4" ${pattern.length ? '' : 'disabled'}>${esc(t('cal.fill'))}</button>
          ${stage === 3 ? `<button type="button" data-close class="btn w-full mt-2 text-muted">${esc(t('cal.skip'))}</button>` : ''}
        </section>
        ${stage ? '' : `<section class="mt-8 pt-6 border-t border-line">
          <h2 class="text-xl font-bold">${esc(t('cal.changeSystem'))}</h2>
          <p class="mt-1 text-sm text-muted">${esc(t('cal.changeSystemDesc'))}</p>
          <div class="mt-4 grid grid-cols-2 gap-2">${['2', '3', '4', 'custom'].map(s =>
            `<button type="button" data-resystem="${s}" class="btn-line">${esc(t('cal.sys.' + s))}</button>`).join('')}</div>
        </section>
        <button type="button" data-close class="btn-soft w-full mt-8">${esc(t('common.close'))}</button>`}`;
    }

    function typeRow(x) {
      const noTime = !x.startTime;
      return `<li data-type="${x.id}" class="p-3 rounded-2xl bg-soft">
        <div class="flex items-center gap-2">
          <button type="button" data-color class="shrink-0 w-11 h-11 rounded-full solid c-${x.color}" aria-label="color"></button>
          <input data-f="name" class="field flex-1 min-w-0" maxlength="10" value="${esc(x.name)}" aria-label="${esc(t('cal.typeName'))}">
          <input data-f="shortName" class="field w-16 text-center font-bold" maxlength="2" value="${esc(x.shortName)}" aria-label="${esc(t('cal.typeShort'))}">
        </div>
        <div class="mt-2 flex items-center gap-2 flex-wrap">
          <input data-f="startTime" type="time" class="field flex-1 min-w-[7rem]" value="${x.startTime || ''}" ${noTime ? 'disabled' : ''} aria-label="start">
          <span class="text-muted">–</span>
          <input data-f="endTime" type="time" class="field flex-1 min-w-[7rem]" value="${x.endTime || ''}" ${noTime ? 'disabled' : ''} aria-label="end">
        </div>
        <div class="mt-2 flex items-center justify-between">
          <label class="inline-flex items-center gap-2 text-sm min-h-tap"><input type="checkbox" data-notime class="w-5 h-5" ${noTime ? 'checked' : ''}>${esc(t('cal.noTime'))}</label>
          <button type="button" data-del-type class="text-sm text-red-600 px-2 min-h-tap">${esc(t('common.delete'))}</button>
        </div>
      </li>`;
    }

    async function saveType(li) {
      const id = +li.dataset.type, x = typeOf(person(), id);
      const noTime = li.querySelector('[data-notime]').checked;
      const b = {
        name: li.querySelector('[data-f=name]').value, shortName: li.querySelector('[data-f=shortName]').value, color: x.color,
        startTime: noTime ? null : li.querySelector('[data-f=startTime]').value || null,
        endTime: noTime ? null : li.querySelector('[data-f=endTime]').value || null,
      };
      if (!b.name.trim() || !b.shortName.trim()) return;
      if (!noTime && (!b.startTime || !b.endTime)) return;   // 둘 다 채워지면 저장
      const r = await api('PUT', `/api/shift-types/${id}`, b);
      if (r.ok) { Object.assign(x, b); saved(); renderGrid(); } else failed();
    }

    box.addEventListener('change', e => {
      const li = e.target.closest('[data-type]');
      if (!li) return;
      if (e.target.matches('[data-notime]')) {
        li.querySelectorAll('[type=time]').forEach(i => { i.disabled = e.target.checked; if (!e.target.checked && !i.value) i.value = i.dataset.f === 'startTime' ? '09:00' : '18:00'; });
      }
      saveType(li);
    });

    async function savePattern(pattern) {
      const r = await api('PUT', '/api/shift-setting/pattern', withTarget({ pattern }));
      if (r.ok) { person().setting.pattern = pattern; saved(); draw(); } else failed();
    }

    async function onClick(e, closeSheet) {
      const b = e.target.closest('button');
      if (!b) return;
      const p = person();
      if (b.matches('[data-next]')) { stage = 3; draw(); return; }
      if (b.matches('[data-color]')) {
        const li = b.closest('[data-type]'), x = typeOf(p, +li.dataset.type);
        x.color = COLORS[(COLORS.indexOf(x.color) + 1) % COLORS.length];
        b.className = b.className.replace(/\bc-\w+/, 'c-' + x.color);
        saveType(li); return;
      }
      if (b.matches('[data-add-type]')) {
        const used = new Set(liveTypes().map(x => x.color));
        const color = COLORS.find(c => !used.has(c)) || 'gray';
        const nb = { name: t('cal.newType'), shortName: t('cal.newTypeShort'), color, startTime: '09:00', endTime: '18:00' };
        const r = await api('POST', '/api/shift-types' + tq, nb);
        if (!r.ok) return failed();
        p.types.push({ id: r.json.id, ...nb, sort: 99, deleted: false });
        saved(); draw(); return;
      }
      if (b.matches('[data-del-type]')) {
        if (b.dataset.armed !== '1') { b.dataset.armed = '1'; b.textContent = t('common.tapAgain'); return; }
        const id = +b.closest('[data-type]').dataset.type;
        const r = await api('DELETE', `/api/shift-types/${id}`);
        if (!r.ok) return failed();
        typeOf(p, id).deleted = true;
        p.setting.pattern = p.setting.pattern.filter(x => x !== id);
        if (st.brush === id) st.brush = null;
        saved(); draw(); render(); return;
      }
      if (b.dataset.patAdd) return savePattern([...(p.setting.pattern || []), +b.dataset.patAdd]);
      if (b.dataset.patRemove) return savePattern(p.setting.pattern.filter((_, i) => i !== +b.dataset.patRemove));
      if (b.matches('[data-pat-clear]')) return savePattern([]);
      if (b.matches('[data-fill]')) {
        const start = box.querySelector('[data-pstart]').value, months = +box.querySelector('[data-pmonths]').value;
        b.disabled = true;
        const r = await api('POST', '/api/shifts/fill', withTarget({ start, months }));
        b.disabled = false;
        if (!r.ok) return toast(errText(r));
        const prev = r.json.previous;
        closeSheet();
        st.month = start.slice(0, 7);
        await load();
        toast(t('cal.filled', months), async () => {
          const u = await api('PUT', '/api/shifts', withTarget({ days: prev }));
          u.ok ? load() : failed();
        });
        return;
      }
      if (b.dataset.resystem) {
        if (b.dataset.armed !== '1') {
          box.querySelectorAll('[data-resystem]').forEach(x => { x.dataset.armed = ''; x.textContent = t('cal.sys.' + x.dataset.resystem); });
          b.dataset.armed = '1'; b.textContent = t('common.tapAgain'); return;
        }
        const r = await api('POST', '/api/shift-setting/preset', withTarget({ system: b.dataset.resystem }));
        if (!r.ok) return toast(errText(r));
        await load(); stage = 2; draw(); return;
      }
    }

    draw();
  }

  document.addEventListener('pref', load);   // 글씨 크기 등 바뀌면 다시 그림
  load();
});
