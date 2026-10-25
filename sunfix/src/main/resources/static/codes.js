/* 관리자 코드 화면: '+ 하위' 버튼 → 아래 추가 폼에 부모·종류 채우기 */
(function () {
  const form = document.querySelector('[data-add-form]');
  const kind = form.querySelector('[data-add-kind]');
  const set = (parent, child, label) => {
    form.elements.parentCode.value = parent;
    form.elements.childGroup.value = child;
    form.elements.code.value = parent ? parent + '_' : '';
    kind.textContent = label;
  };
  document.addEventListener('click', e => {
    const b = e.target.closest('[data-add-child]');
    if (b) {
      set(b.dataset.parent, b.dataset.child, `${b.dataset.parent} → ${b.textContent.trim()}`);
      form.scrollIntoView({ behavior: 'smooth' });
      form.elements.code.focus();
    }
    if (e.target.closest('[data-add-reset]')) { set('', '', App.t('admin.codes.addRoot')); form.scrollIntoView({ behavior: 'smooth' }); form.elements.code.focus(); }
  });
})();
