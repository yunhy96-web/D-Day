/* 홈 검색: 리프 카테고리 이름 경로 + 검색어(kw.{코드}) 를 브라우저에서 거른다 (설계 §3-1) */
(function () {
  const box = document.querySelector('[data-search]');
  if (!box) return;
  const input = box.querySelector('[data-search-input]');
  const list = box.querySelector('[data-search-results]');
  const empty = box.querySelector('[data-search-empty]');
  const norm = s => s.toLowerCase().replace(/\s+/g, '');
  const index = JSON.parse(box.querySelector('[data-search-index]').textContent).map(c => {
    const words = (c.path + ',' + c.kw).split(/[,›]/).map(norm).filter(Boolean);
    return { ...c, hay: words.join('|'), words };
  });

  /** 단어마다: 이름·검색어에 들어 있거나, 단어가 검색어를 품으면('변기막혔어요' ⊃ '막혔') 맞음. 맞은 단어 수로 정렬 */
  function search(q) {
    const tokens = q.split(/\s+/).map(norm).filter(Boolean);
    if (!tokens.length) return [];
    return index.map(c => ({ c, score: tokens.filter(t => c.hay.includes(t) || c.words.some(w => w.length >= 2 && t.includes(w))).length }))
      .filter(r => r.score > 0).sort((a, b) => b.score - a.score).slice(0, 6).map(r => r.c);
  }

  function render() {
    const q = input.value.trim();
    const hits = search(q);
    list.innerHTML = '';
    for (const c of hits) {
      const li = document.createElement('li');
      const a = document.createElement('a');
      a.href = '/c/' + encodeURIComponent(c.code);
      a.className = 'flex items-center gap-3 px-3 py-3 rounded-2xl hover:bg-soft focus:bg-soft outline-none';
      const icon = document.createElement('span'); icon.className = 'text-2xl'; icon.textContent = c.icon;
      const name = document.createElement('span'); name.className = 'font-semibold'; name.textContent = c.path;
      a.append(icon, name); li.append(a); list.append(li);
    }
    list.classList.toggle('hidden', !hits.length);
    empty.classList.toggle('hidden', !q || hits.length > 0);
  }

  input.addEventListener('input', render);
  // 엔터 = 첫 결과로
  input.addEventListener('keydown', e => {
    if (e.key === 'Enter') { const first = list.querySelector('a'); if (first) location.href = first.href; }
    if (e.key === 'ArrowDown') { const first = list.querySelector('a'); if (first) { e.preventDefault(); first.focus(); } }
  });
})();
