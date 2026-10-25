/* 고르는 화면(옵션·제보) + 결과 화면 견적 문의 */
(function () {
  const pick = document.querySelector('[data-pick]');

  /** 고른 옵션·지역. 다 골랐으면 complete */
  function state() {
    const steps = [...pick.querySelectorAll('[data-step]')];
    const options = [...pick.querySelectorAll('[data-opt]:checked')].map(i => i.value);
    const r = pick.querySelector('input[name="r"]:checked');
    return { options, region: r ? r.value : null, complete: options.length === steps.length - 1 && !!r, steps };
  }

  if (pick) {
    const category = pick.dataset.category;
    const countEl = document.querySelector('[data-count]');
    const submit = document.querySelector('[data-submit]');

    function showSubs(sido) {
      pick.querySelectorAll('[data-subs]').forEach(d => {
        const on = d.dataset.sido === sido;
        d.classList.toggle('hidden', !on);
        if (!on) d.querySelectorAll('input').forEach(i => { i.checked = false; });
      });
      const hint = pick.querySelector('[data-sido-hint]');
      if (hint) hint.remove();
    }

    async function refresh() {
      const s = state();
      if (submit) submit.disabled = !s.complete;
      if (!countEl) return;
      const q = new URLSearchParams({ c: category });
      if (s.region) q.set('r', s.region);
      const res = await App.api('GET', '/api/count?' + q);
      if (res.ok) countEl.textContent = App.t('pick.count', res.json.count);
    }

    pick.addEventListener('change', e => {
      if (e.target.name === 'sido') showSubs(e.target.value);
      refresh();
      // 이번 단계를 다 골랐으면 아직 안 고른 다음 단계로
      const step = e.target.closest('[data-step]');
      const next = state().steps.find(st => st !== step && !st.querySelector('input:checked:not([name="sido"])'));
      if (next && e.target.name !== 'sido') next.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    refresh();

    // 비교하기: o=..&o=..&r=.. 로 결과 화면
    const form = document.querySelector('[data-compare]');
    if (form) form.addEventListener('submit', e => {
      e.preventDefault();
      const s = state();
      if (!s.complete) return;
      const q = new URLSearchParams();
      s.options.forEach(o => q.append('o', o));
      q.set('r', s.region);
      location.href = form.getAttribute('action') + '?' + q;
    });

    // 견적 제보
    const report = document.querySelector('[data-report]');
    if (report) {
      const vendorId = report.querySelector('[name="vendorId"]');
      const vendorName = report.querySelector('[name="vendorName"]');
      const toggleName = () => { vendorName.classList.toggle('hidden', !!vendorId.value); vendorName.nextElementSibling.classList.toggle('hidden', !!vendorId.value); };
      vendorId.addEventListener('change', toggleName); toggleName();
      report.addEventListener('submit', async e => {
        e.preventDefault();
        const s = state(), msg = report.querySelector('[data-msg]'), btn = report.querySelector('button:not([type])');
        if (!s.complete) { msg.textContent = App.t('err.options'); return; }
        btn.disabled = true; msg.textContent = '';
        const res = await App.api('POST', '/api/report', {
          category, options: s.options, region: s.region,
          vendorId: vendorId.value ? +vendorId.value : null, vendorName: vendorName.value,
          price: report.elements.price.value ? +report.elements.price.value : null, ym: report.elements.ym.value, memo: report.elements.memo.value,
        });
        btn.disabled = false;
        if (!res.ok) { msg.textContent = App.errText(res); return; }
        report.classList.add('hidden');
        document.querySelector('[data-report-done]').classList.remove('hidden');
        scrollTo({ top: 0, behavior: 'smooth' });
      });
    }
  }

  // 결과 화면: 견적 문의 모달
  const dialog = document.querySelector('[data-inquiry-dialog]');
  if (dialog) {
    const form = dialog.querySelector('form');
    let vendorId;
    document.addEventListener('click', e => {
      const b = e.target.closest('[data-inquiry]');
      if (!b) return;
      vendorId = +b.dataset.vendor;
      dialog.querySelector('[data-vendor-name]').textContent = b.dataset.name;
      form.querySelector('[data-msg]').textContent = '';
      dialog.showModal();
    });
    form.querySelector('[data-close]').addEventListener('click', () => dialog.close());
    form.addEventListener('submit', async e => {
      e.preventDefault();
      const msg = form.querySelector('[data-msg]');
      const res = await App.api('POST', '/api/inquiry', {
        vendorId, category: form.dataset.category, region: form.dataset.region,
        options: form.elements.options.value.split(","), name: form.elements.name.value, phone: form.elements.phone.value, memo: form.elements.memo.value,
      });
      if (!res.ok) { msg.textContent = App.errText(res); return; }
      dialog.close(); form.reset();
      App.toast(App.t('inquiry.sent'));
    });
  }
})();
