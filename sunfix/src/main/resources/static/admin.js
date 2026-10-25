/* 관리자: 칸 단위 자동 저장(data-put) · 체크 묶음(data-set) · 상태 버튼(data-status) · 폼(data-form) */
(function () {
  const saved = () => App.toast(App.t('common.saved'));
  const failed = res => App.toast(App.errText(res), true);

  function value(el) {
    if (el.type === 'checkbox') return el.checked ? 'Y' : 'N';
    const v = el.value.trim();
    if (el.dataset.type === 'int') return v === '' ? null : +v;
    return v;
  }

  document.addEventListener('change', async e => {
    const el = e.target;
    if (el.dataset.put) {
      // data-arg-category-code="X" → { categoryCode: "X" }
      const body = { [el.dataset.field]: value(el) };
      for (const [k, v] of Object.entries(el.dataset)) if (k.startsWith('arg')) body[k[3].toLowerCase() + k.slice(4)] = v;
      const res = await App.api('PUT', el.dataset.put, body);
      res.ok ? saved() : failed(res);
      return;
    }
    const set = el.closest('[data-set]');
    if (set) {
      const codes = [...set.querySelectorAll('input[type=checkbox]:checked')].map(i => i.value);
      const res = await App.api('PUT', set.dataset.set, { codes });
      if (!res.ok) return failed(res);
      set.dataset.reload !== undefined ? location.reload() : saved();
    }
  });

  document.addEventListener('click', async e => {
    const b = e.target.closest('[data-status]');
    if (b) {
      const res = await App.api('PUT', b.dataset.status, { status: b.value });
      res.ok ? location.reload() : failed(res);
    }
    if (e.target.closest('[data-logout]')) {
      await App.api('POST', '/admin/api/logout', {});
      location.href = '/admin/login';
    }
  });

  document.addEventListener('submit', async e => {
    const f = e.target.closest('[data-form]');
    if (!f) return;
    e.preventDefault();
    const body = {};
    for (const el of f.elements) if (el.name) { const v = value(el); body[el.name] = v === '' ? null : v; }
    const res = await App.api(f.dataset.method || 'POST', f.dataset.form, body);
    if (!res.ok) return failed(res);
    if (f.dataset.next) location.href = f.dataset.next.replace('{id}', res.json.id);
    else location.reload();
  });
})();
