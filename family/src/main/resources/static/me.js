/* 내 정보: 이름·색·동의는 /api/me, 언어·테마·글씨·시간대는 app.js(data-pref). 전부 자동 저장 */
document.addEventListener('DOMContentLoaded', () => {
  const { api, t, errText, saved, failed } = window.App;
  const put = async body => { const r = await api('PUT', '/api/me', body); r.ok ? saved() : failed(); return r.ok; };

  const name = document.querySelector('[data-profile=name]');
  let last = name.value;
  name.addEventListener('change', async () => {
    if (!name.value.trim()) { name.value = last; return; }
    if (await put({ name: name.value })) { last = name.value; document.querySelector('[data-avatar]').textContent = [...name.value.trim()][0]; }
  });

  document.querySelectorAll('button[data-profile=color]').forEach(b => b.addEventListener('click', async () => {
    document.querySelectorAll('button[data-profile=color]').forEach(x => x.setAttribute('aria-pressed', String(x === b)));
    const av = document.querySelector('[data-avatar]');
    av.className = av.className.replace(/\bc-\w+/, 'c-' + b.value);
    put({ color: b.value });
  }));

  const consent = document.querySelector('[data-profile=legacyConsent]');
  consent.addEventListener('change', () => put({ legacyConsent: consent.checked }));

  document.querySelector('[data-pw]').addEventListener('submit', async e => {
    e.preventDefault();
    const f = e.target, msg = f.querySelector('[data-msg]');
    const r = await api('PUT', '/api/me/password', { current: f.current.value, next: f.next.value });
    if (r.ok) { f.reset(); msg.textContent = ''; saved(); App.toast(t('password.changed')); } else msg.textContent = errText(r);
  });
});
