/* 영업 리드: 연락 문구 복사, 업체로 등록 → 업체 편집 화면 */
(function () {
  document.addEventListener('click', async e => {
    if (e.target.closest('[data-copy-pitch]')) {
      try { await navigator.clipboard.writeText(document.querySelector('[data-pitch]').textContent); App.toast(App.t('admin.leads.copied')); }
      catch { App.toast(App.t('err.general'), true); }
    }
    const b = e.target.closest('[data-to-vendor]');
    if (b) {
      b.disabled = true;
      const res = await App.api('POST', `/admin/api/leads/${b.dataset.id}/vendor`, {});
      if (res.ok) location.href = '/admin/vendors/' + res.json.id;
      else { b.disabled = false; App.toast(App.errText(res), true); }
    }
  });
})();
