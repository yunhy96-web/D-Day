/* 설계 §6-1. 출력은 textContent 만. */
(function () {
  const $ = s => document.querySelector(s);
  const form = $('[data-form]'), list = $('[data-list]'), more = $('[data-more]'), msg = $('[data-msg]'), dlg = $('[data-del-dialog]');
  let page = 0;

  function row(g) {
    const li = document.createElement('li'); li.dataset.id = g.id; li.className = 'py-5 border-t border-ink/10 text-center';
    const head = document.createElement('div'); head.className = 'flex justify-center gap-2 items-baseline text-[13px]';
    const name = document.createElement('span'); name.className = 'text-ink/85'; name.textContent = g.name;
    const date = document.createElement('span'); date.className = 'text-ink/35 tabular'; date.textContent = (g.createdAt || '').slice(0, 10);
    head.append(name, date);
    const p = document.createElement('p'); p.className = 'mt-2 text-[15px] leading-7 whitespace-pre-line break-words'; p.textContent = g.message;
    const del = document.createElement('button'); del.type = 'button'; del.dataset.del = ''; del.dataset.i18n = 'guestbook.delete';
    del.className = 'mt-2 text-[12px] text-ink/35'; del.textContent = W.t('guestbook.delete');
    li.append(head, p);
    if (g.photos && g.photos.length) {
      const box = document.createElement('div'); box.className = 'mt-3 flex justify-center gap-1.5';
      g.photos.forEach(ph => {
        const b = document.createElement('button'); b.type = 'button'; b.dataset.view = ph.view; b.className = 'w-24 h-24 rounded-lg overflow-hidden bg-jasmine';
        const im = document.createElement('img'); im.src = ph.thumb; im.alt = ''; im.loading = 'lazy'; im.className = 'w-full h-full object-cover';
        b.append(im); box.append(b);
      });
      li.append(box);
    }
    li.append(del); return li;
  }

  // ---- 사진 첨부 (설계 §15-7): 고르면 바로 2048px 로 줄여 올리고 토큰을 모은다
  const MAX = 3, pInput = $('[data-photo-input]'), pList = $('[data-photo-list]'), pMsg = $('[data-photo-msg]');
  let shots = [];   // {token|null, failed, li}
  const busy = () => shots.some(s => !s.token && !s.failed);
  function progressText() {
    const done = shots.filter(s => s.token).length;
    pMsg.textContent = busy() ? W.t('guestbook.photo_progress', done + 1, shots.length) : shots.some(s => s.failed) ? W.t('guestbook.photo_failed') : '';
  }
  pInput.addEventListener('change', async () => {
    const files = [...pInput.files].slice(0, MAX - shots.length); pInput.value = '';
    const added = files.map(f => {
      const li = document.createElement('li'); li.className = 'relative w-20';
      const im = document.createElement('img'); im.alt = ''; im.className = 'w-20 h-20 rounded-lg object-cover bg-jasmine opacity-60'; im.src = URL.createObjectURL(f);
      const bar = document.createElement('div'); bar.className = 'up-bar mt-1'; bar.append(document.createElement('i'));
      const x = document.createElement('button'); x.type = 'button'; x.className = 'absolute -top-2 -right-2 w-6 h-6 rounded-full bg-night/70 text-white text-[12px]';
      x.textContent = '×'; x.setAttribute('aria-label', W.t('guestbook.photo_remove'));
      li.append(im, bar, x); pList.append(li);
      const shot = { token: null, failed: false, li, file: f, im, bar };
      x.addEventListener('click', () => { shots = shots.filter(s => s !== shot); li.remove(); progressText(); });
      return shot;
    });
    shots.push(...added); progressText();
    await Up.pool(added.length, 2, async i => {
      const s = added[i];
      try {
        const blob = await Up.resize(s.file, 2048, .88);
        const r = await Up.send('/api/guestbook/photo', blob, 'image/jpeg', p => s.bar.firstChild.style.width = (p * 100) + '%');
        if (r.ok) { s.token = r.json.token; s.im.classList.remove('opacity-60'); s.bar.remove(); } else s.failed = true;
      } catch { s.failed = true; }
      if (s.failed) s.im.classList.add('grayscale');
      progressText();
    });
  });
  function resetShots() { shots = []; pList.textContent = ''; pMsg.textContent = ''; }

  // 사진 크게 보기
  const viewer = $('[data-photo-viewer]');
  list.addEventListener('click', e => {
    const b = e.target.closest('[data-view]'); if (!b) return;
    viewer.querySelector('img').src = b.dataset.view; viewer.showModal();
  });
  viewer.addEventListener('click', () => viewer.close());

  form.addEventListener('submit', async e => {
    e.preventDefault();
    const fd = new FormData(form), btn = form.querySelector('button[type=submit]');
    if (busy()) { msg.textContent = W.t('guestbook.photo_wait'); return; }
    const tokens = shots.filter(s => s.token).map(s => s.token), views = shots.filter(s => s.token);
    btn.disabled = true; msg.textContent = '';
    try {
      const r = await W.api('/api/guestbook', 'POST', { sid: W.sid, name: fd.get('name'), message: fd.get('message'), password: fd.get('password'), turnstile: fd.get('cf-turnstile-response'), photos: tokens });
      if (r.status === 429) { msg.textContent = W.t('guestbook.rate'); return; }
      if (!r.ok) { msg.textContent = W.t('guestbook.error'); return; }
      const { id } = await r.json();
      list.prepend(row({ id, name: fd.get('name'), message: fd.get('message'), createdAt: new Date().toISOString(),
        photos: views.map(s => ({ thumb: '/guestbook-photos/' + s.token + '_t.jpg', view: '/guestbook-photos/' + s.token + '.jpg' })) }));
      $('[data-empty]').classList.add('hidden');
      form.reset(); resetShots(); msg.textContent = W.t('guestbook.success');
      if (window.turnstile) try { turnstile.reset(); } catch {}
    } catch { msg.textContent = W.t('guestbook.error'); }
    finally { btn.disabled = false; }
  });

  more.addEventListener('click', async () => {
    page++;
    const r = await (await fetch('/api/guestbook?page=' + page)).json();
    r.items.forEach(g => list.append(row(g)));
    more.classList.toggle('hidden', !r.hasNext);
  });

  list.addEventListener('click', e => {
    const b = e.target.closest('[data-del]'); if (!b) return;
    const li = b.closest('li'); dlg.dataset.target = li.dataset.id;
    dlg.querySelector('[name=pw]').value = ''; dlg.querySelector('[data-del-msg]').textContent = '';
    dlg.showModal();
  });
  dlg.addEventListener('close', async () => {
    if (dlg.returnValue !== 'ok') return;
    const pw = dlg.querySelector('[name=pw]').value, id = dlg.dataset.target;
    const r = await fetch('/api/guestbook/' + id, { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ password: pw }) });
    if (r.ok || r.status === 404) { list.querySelector(`li[data-id="${id}"]`)?.remove(); if (!list.children.length) $('[data-empty]').classList.remove('hidden'); }
    else { msg.textContent = W.t('guestbook.wrong_pw'); setTimeout(() => msg.textContent = '', 2000); }
  });
})();
