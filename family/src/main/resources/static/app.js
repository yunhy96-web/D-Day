/* 공통: 라벨 · API · 자동 저장 표시 · 되돌리기 토스트 · 화면 설정 (설계 §7-2) */
(function () {
  const L = JSON.parse(document.getElementById('L').textContent);
  const root = document.documentElement;

  /** 라벨 + {0} 치환 */
  function t(key, ...args) {
    let s = L[key] ?? key;
    args.forEach((a, i) => { s = s.replace('{' + i + '}', a); });
    return s;
  }

  async function api(method, url, body) {
    try {
      const r = await fetch(url, {
        method,
        headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
      let json = {};
      try { json = await r.json(); } catch {}
      if (r.status === 401) location.href = '/login';
      return { ok: r.ok, status: r.status, json };
    } catch {
      return { ok: false, status: 0, json: { code: 'network' } };
    }
  }

  function errText(res) { return L['err.' + (res.json && res.json.code)] || L['err.general']; }

  /** 위 줄의 "저장됨 ✓" 를 잠깐 */
  let savedTimer;
  function saved() {
    const el = document.querySelector('[data-saved]');
    if (!el) return;
    el.textContent = t('common.saved');
    el.classList.remove('text-red-600');
    el.classList.add('opacity-100');
    clearTimeout(savedTimer);
    savedTimer = setTimeout(() => el.classList.remove('opacity-100'), 1600);
  }
  function failed() {
    const el = document.querySelector('[data-saved]');
    if (el) { el.textContent = t('common.saveFailed'); el.classList.add('opacity-100', 'text-red-600'); }
    toast(t('common.saveFailed'));
  }

  /** 아래 토스트. undo 를 주면 "되돌리기" 버튼 (4초) */
  let toastEl, toastTimer;
  function toast(msg, undo) {
    if (!toastEl) {
      toastEl = document.createElement('div');
      toastEl.className = 'fixed inset-x-0 z-50 flex justify-center px-4 pointer-events-none bottom-[calc(5.5rem+env(safe-area-inset-bottom))]';
      document.body.appendChild(toastEl);
    }
    toastEl.innerHTML = '';
    const box = document.createElement('div');
    box.className = 'pointer-events-auto flex items-center gap-4 max-w-md w-full px-5 py-3 rounded-2xl bg-fg text-bg shadow-lg';
    const span = document.createElement('span');
    span.className = 'flex-1';
    span.textContent = msg;
    box.appendChild(span);
    if (undo) {
      const b = document.createElement('button');
      b.className = 'font-bold text-accent shrink-0 min-h-tap';
      b.textContent = t('common.undo');
      b.onclick = () => { box.remove(); undo(); };
      box.appendChild(b);
    }
    toastEl.appendChild(box);
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => box.remove(), undo ? 4000 : 2500);
  }

  /** 크게 보기면 '저녁 8시' 처럼 (Fmt.time 과 같은 규칙) */
  function fmtTime(hhmm) {
    if (!hhmm) return '';
    if (root.dataset.font !== 'lg') return hhmm;
    const h = +hhmm.slice(0, 2), m = +hhmm.slice(3);
    if (root.lang === 'th') return `${String(h).padStart(2, '0')}.${String(m).padStart(2, '0')} น.`;
    const part = h < 5 ? '새벽' : h < 12 ? '오전' : h < 18 ? '오후' : h < 21 ? '저녁' : '밤';
    const h12 = h === 0 ? 12 : h > 12 ? h - 12 : h;
    return `${part} ${h12}시` + (m ? ` ${m}분` : '');
  }
  function fmtRange(s, e) { return s && e ? fmtTime(s) + '–' + fmtTime(e) + (e <= s ? '(+1)' : '') : ''; }

  // ---- 화면 설정: 바꾸는 즉시 적용 + 저장
  async function setPref(field, value) {
    if (field === 'theme') { root.dataset.theme = value; window.__applyTheme(); }
    if (field === 'fontScale') root.dataset.font = value;
    const res = await api('PUT', '/api/prefs', { [field]: value });
    if (!res.ok) return failed();
    if (field === 'lang') location.reload(); else saved();
  }
  function markPressed(group, value) {
    document.querySelectorAll(`[data-pref="${group}"]`).forEach(b => b.setAttribute('aria-pressed', String(b.value === value)));
  }

  document.addEventListener('click', e => {
    const p = e.target.closest('button[data-pref]');
    if (p) { markPressed(p.dataset.pref, p.value); setPref(p.dataset.pref, p.value); document.dispatchEvent(new CustomEvent('pref', { detail: p.dataset.pref })); return; }
    if (e.target.closest('[data-font-cycle]')) {
      const order = ['sm', 'md', 'lg'];
      setPref('fontScale', order[(order.indexOf(root.dataset.font) + 1) % 3]);
      return;
    }
    if (e.target.closest('[data-theme-cycle]')) {
      const dark = root.classList.contains('dark');
      setPref('theme', dark ? 'light' : 'dark');
      return;
    }
    if (e.target.closest('[data-logout]')) {
      api('POST', '/api/logout', {}).then(() => { location.href = '/login'; });
    }
  });
  document.addEventListener('change', e => {
    const s = e.target.closest('select[data-pref]');
    if (s) setPref(s.dataset.pref, s.value);
  });

  window.App = { L, t, api, errText, saved, failed, toast, fmtTime, fmtRange };
})();
