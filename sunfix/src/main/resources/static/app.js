/* 공통: 라벨 · API · 토스트 · 언어/테마 전환 */
(function () {
  const L = JSON.parse(document.getElementById('L').textContent);
  const root = document.documentElement;

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
      return { ok: r.ok, status: r.status, json };
    } catch {
      return { ok: false, status: 0, json: { code: 'network' } };
    }
  }

  function errText(res) { return L['err.' + (res.json && res.json.code)] || L['err.general']; }

  let toastTimer;
  function toast(msg, bad) {
    let el = document.querySelector('[data-toast]');
    if (!el) {
      el = document.createElement('div');
      el.dataset.toast = '';
      el.setAttribute('role', 'status');
      el.className = 'fixed left-1/2 -translate-x-1/2 bottom-24 z-50 px-5 py-3 rounded-2xl shadow-lg font-medium transition-opacity';
      document.body.appendChild(el);
    }
    el.textContent = msg;
    el.classList.toggle('bg-red-600', !!bad); el.classList.toggle('text-white', !!bad);
    el.classList.toggle('bg-fg', !bad); el.classList.toggle('text-bg', !bad);
    el.classList.remove('opacity-0');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.add('opacity-0'), 2200);
  }

  function setCookie(k, v) { document.cookie = `${k}=${v}; path=/; max-age=31536000; samesite=lax`; }

  document.addEventListener('click', e => {
    const lang = e.target.closest('[data-lang]');
    if (lang) {
      setCookie('lang', lang.value);
      const u = new URL(location.href); u.searchParams.delete('lang');   // ?lang= 이 쿠키를 덮지 않게
      location.href = u.toString();
    }
    if (e.target.closest('[data-theme-cycle]')) {
      const order = ['system', 'light', 'dark'];
      const next = order[(order.indexOf(root.dataset.theme) + 1) % 3];
      root.dataset.theme = next;
      setCookie('theme', next);
      root.classList.toggle('dark', next === 'dark' || (next === 'system' && matchMedia('(prefers-color-scheme: dark)').matches));
      toast(t('theme.' + next));
    }
  });

  window.App = { t, api, errText, toast };
})();
