/* 공통: 세션ID · 다국어 · heartbeat · 이벤트 · 복사/공유 · D-day · 스크롤 reveal */
(function () {
  const L = JSON.parse(document.getElementById('labels').textContent);
  const LANGS = ['ko', 'en', 'th'];
  const sid = (() => { try { return localStorage.sid || (localStorage.sid = crypto.randomUUID()); } catch { return crypto.randomUUID(); } })();
  let lang = document.documentElement.lang || 'ko';
  const when = document.body.dataset.datetime ? new Date(document.body.dataset.datetime) : null;
  const startOfDay = d => new Date(d.getFullYear(), d.getMonth(), d.getDate());

  const fmt = (s, args) => (s || '').replace(/\{(\d)\}/g, (_, i) => args[i] ?? '');
  const t = (key, ...args) => fmt((L[lang] && L[lang][key]) ?? (L.ko && L.ko[key]) ?? key, args);

  function applyLang() {
    document.documentElement.lang = lang;
    document.querySelectorAll('[data-i18n]').forEach(el => {
      const args = el.dataset.args ? el.dataset.args.split('|') : [];
      el.textContent = t(el.dataset.i18n, ...args);
    });
    document.querySelectorAll('[data-i18n-ph]').forEach(el => el.placeholder = t(el.dataset.i18nPh));
    document.querySelectorAll('[data-i18n-aria]').forEach(el => el.setAttribute('aria-label', t(el.dataset.i18nAria)));
    document.querySelectorAll('[data-lang]').forEach(b => b.setAttribute('aria-pressed', b.dataset.lang === lang));
    document.title = t('og.title');
    renderDates();
  }
  function setLang(l, user) {
    if (!LANGS.includes(l) || l === lang) return;
    lang = l;
    try { localStorage.lang = l; } catch {}
    document.cookie = 'lang=' + l + ';path=/;max-age=31536000;SameSite=Lax' + (location.protocol === 'https:' ? ';Secure' : '');
    applyLang();
    if (user) track('lang.switch', l);
  }
  document.querySelectorAll('[data-lang]').forEach(b => b.addEventListener('click', () => setLang(b.dataset.lang, true)));

  // 초기 언어: localStorage → 브라우저 → 서버 렌더링 값
  (function initLang() {
    let saved = null; try { saved = localStorage.lang; } catch {}
    const nav = (navigator.language || '').slice(0, 2);
    const want = saved || (LANGS.includes(nav) ? nav : lang);
    if (want !== lang) setLang(want); else applyLang();
  })();

  // 다크모드 토글 (초기 적용은 head 인라인 스크립트)
  document.querySelectorAll('[data-theme-toggle]').forEach(b => b.addEventListener('click', () => {
    const t = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = t; try { localStorage.theme = t; } catch {}
  }));

  // API
  const api = (path, method, body) => fetch(path, { method, headers: { 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined });
  const track = (name, meta) => { api('/api/event', 'POST', { sid, name, meta: meta == null ? undefined : String(meta) }).catch(() => {}); };

  // heartbeat (설계 §6)
  async function heartbeat() {
    try {
      const r = await api('/api/heartbeat', 'POST', { sid });
      if (!r.ok) return;
      const { online, total } = await r.json();
      document.querySelectorAll('[data-online]').forEach(el => { el.dataset.args = online; el.textContent = t('footer.online', online); });
      document.querySelectorAll('[data-total]').forEach(el => { el.dataset.args = total; el.textContent = t('footer.total', total); });
    } catch {}
  }
  heartbeat(); setInterval(heartbeat, 15000);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) heartbeat(); });

  // 유입 추적
  const from = new URLSearchParams(location.search).get('from');
  if (from) track('share.in', from);

  // 복사 · 공유
  async function copy(text, el) {
    try { await navigator.clipboard.writeText(text); } catch {
      const ta = document.createElement('textarea'); ta.value = text; document.body.appendChild(ta); ta.select(); document.execCommand('copy'); ta.remove();
    }
    if (el) { const o = el.textContent; el.textContent = t('btn.copied'); setTimeout(() => el.textContent = o, 1500); }
  }
  async function share(url, title) {
    url = url || location.origin + '/?from=share';
    if (navigator.share) { try { await navigator.share({ title: title || t('og.title'), url }); return; } catch {} }
    await copy(url); alert(t('share.copied'));
  }
  document.querySelectorAll('[data-share]').forEach(b => b.addEventListener('click', () => share()));
  document.querySelectorAll('[data-copy-account]').forEach(b => b.addEventListener('click', async () => {
    const r = await fetch('/api/account?side=' + b.dataset.copyAccount); if (!r.ok) return;
    const a = await r.json(); copy(a.bank + ' ' + a.number + ' ' + a.holder, b); track('account.copy', b.dataset.copyAccount);
  }));
  document.querySelectorAll('[data-copy-address]').forEach(b => b.addEventListener('click', async () => {
    const r = await fetch('/api/address?lang=' + lang); if (!r.ok) return;
    copy((await r.json()).address, b);
  }));

  // 날짜 · D-day · 카운트다운
  function renderDates() {
    if (!when) return;
    const loc = lang === 'th' ? 'th-TH' : lang === 'en' ? 'en-US' : 'ko-KR';
    const cal = { calendar: 'gregory' };   // 태국어 불기 대신 서기 고정. 불기 원하면 이 줄 제거
    document.querySelectorAll('[data-date-long]').forEach(el => el.textContent =
      new Intl.DateTimeFormat(loc, { ...cal, year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }).format(when));
    document.querySelectorAll('[data-date-time]').forEach(el => el.textContent =
      new Intl.DateTimeFormat(loc, { hour: 'numeric', minute: '2-digit' }).format(when));
    document.querySelectorAll('[data-date-short]').forEach(el => el.textContent =
      new Intl.DateTimeFormat('en-US', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(when).replace(/(\d+)\/(\d+)\/(\d+)/, '$3. $1. $2'));
    const days = Math.ceil((startOfDay(when) - startOfDay(new Date())) / 864e5);
    document.querySelectorAll('[data-dday]').forEach(el => el.textContent = days > 0 ? t('dday.before', days) : days === 0 ? t('dday.today') : t('dday.after', -days));
    tick();
  }
  function tick() {
    const el = document.querySelector('[data-countdown]'); if (!el || !when) return;
    let s = Math.floor((when - Date.now()) / 1000);
    if (s <= 0) { el.textContent = t('date.passed'); return; }
    const d = Math.floor(s / 86400); s %= 86400; const h = Math.floor(s / 3600); s %= 3600; const m = Math.floor(s / 60); s %= 60;
    el.textContent = t('date.countdown', d, h, m, s);
  }
  setInterval(tick, 1000);

  // 달력
  (function calendar() {
    const el = document.querySelector('[data-calendar]'); if (!el || !when) return;
    const y = when.getFullYear(), mo = when.getMonth();
    const first = new Date(y, mo, 1).getDay(), days = new Date(y, mo + 1, 0).getDate();
    let html = '';
    for (let i = 0; i < first; i++) html += '<span></span>';
    for (let d = 1; d <= days; d++) html += `<span class="${d === when.getDate() ? 'rounded-full bg-yeonji text-white font-semibold' : ''}">${d}</span>`;
    el.innerHTML = html;   // 숫자만 생성. 외부 입력 없음
  })();

  // 스크롤 reveal
  const io = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); } }), { threshold: .15 });
  document.querySelectorAll('.reveal').forEach(el => io.observe(el));

  // 컨텐츠 진입 추적
  document.querySelectorAll('[data-content-link]').forEach(a => a.addEventListener('click', () => track('content.open', a.dataset.contentLink)));

  window.W = { t, get lang() { return lang; }, sid, api, track, copy, share };
})();
