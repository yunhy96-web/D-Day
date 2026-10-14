/* 가족 관리 (설계 §4). 승인·역할·차단은 누르는 즉시 저장 */
document.addEventListener('DOMContentLoaded', () => {
  const { api, t, errText, saved, toast } = window.App;
  const idOf = el => el.closest('[data-member]').dataset.member;

  document.addEventListener('click', async e => {
    const b = e.target.closest('button');
    if (!b) return;
    if (b.matches('[data-approve]')) {
      const r = await api('PUT', `/admin/api/members/${idOf(b)}/status`, { status: 'ACTIVE' });
      r.ok ? location.reload() : toast(errText(r));
    } else if (b.matches('[data-reject]')) {
      if (b.dataset.armed !== '1') { b.dataset.armed = '1'; b.textContent = t('common.tapAgain'); return; }
      const r = await api('DELETE', `/admin/api/members/${idOf(b)}`);
      r.ok ? b.closest('li').remove() : toast(errText(r));
    } else if (b.matches('[data-status]')) {
      if (b.dataset.status === 'BLOCKED' && b.dataset.armed !== '1') { b.dataset.armed = '1'; b.textContent = t('common.tapAgain'); return; }
      const r = await api('PUT', `/admin/api/members/${idOf(b)}/status`, { status: b.dataset.status });
      r.ok ? location.reload() : toast(errText(r));
    } else if (b.matches('[data-reset]')) {
      if (b.dataset.armed !== '1') { b.dataset.armed = '1'; b.textContent = t('common.tapAgain'); return; }
      const r = await api('POST', `/admin/api/members/${idOf(b)}/reset-password`, {});
      if (!r.ok) return toast(errText(r));
      const p = b.closest('li').querySelector('[data-temp]');
      p.textContent = t('admin.tempPw', r.json.tempPassword);
      p.classList.remove('hidden');
      b.textContent = t('admin.resetPw'); b.dataset.armed = '';
    }
  });

  document.querySelectorAll('[data-role]').forEach(c => c.addEventListener('change', async () => {
    const r = await api('PUT', `/admin/api/members/${idOf(c)}/roles`, { role: c.value, granted: c.checked });
    if (r.ok) saved(); else { c.checked = !c.checked; toast(errText(r)); }
  }));
});
