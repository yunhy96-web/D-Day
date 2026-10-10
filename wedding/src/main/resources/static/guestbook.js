/* 설계 §6-1. 출력은 textContent 만. */
(function () {
  const $ = s => document.querySelector(s);
  const form = $('[data-form]'), list = $('[data-list]'), more = $('[data-more]'), msg = $('[data-msg]'), dlg = $('[data-del-dialog]');
  let page = 0;

  function row(g) {
    const li = document.createElement('li'); li.dataset.id = g.id; li.className = 'memo p-4 rounded-lg bg-jasmine border border-gold/40 shadow-sm';
    const head = document.createElement('div'); head.className = 'flex justify-between items-baseline text-xs';
    const name = document.createElement('span'); name.className = 'font-medium'; name.textContent = g.name;
    const date = document.createElement('span'); date.className = 'text-ink/40 tabular'; date.textContent = (g.createdAt || '').slice(0, 10);
    head.append(name, date);
    const p = document.createElement('p'); p.className = 'mt-2 text-sm leading-6 whitespace-pre-line break-words'; p.textContent = g.message;
    const del = document.createElement('button'); del.type = 'button'; del.dataset.del = ''; del.dataset.i18n = 'guestbook.delete';
    del.className = 'mt-2 text-[11px] text-ink/40 underline'; del.textContent = W.t('guestbook.delete');
    li.append(head, p, del); return li;
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();
    const fd = new FormData(form), btn = form.querySelector('button[type=submit]');
    btn.disabled = true; msg.textContent = '';
    try {
      const r = await W.api('/api/guestbook', 'POST', { sid: W.sid, name: fd.get('name'), message: fd.get('message'), password: fd.get('password'), turnstile: fd.get('cf-turnstile-response') });
      if (r.status === 429) { msg.textContent = W.t('guestbook.rate'); return; }
      if (!r.ok) { msg.textContent = W.t('guestbook.error'); return; }
      const { id } = await r.json();
      list.prepend(row({ id, name: fd.get('name'), message: fd.get('message'), createdAt: new Date().toISOString() }));
      $('[data-empty]').classList.add('hidden');
      form.reset(); msg.textContent = W.t('guestbook.success');
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
