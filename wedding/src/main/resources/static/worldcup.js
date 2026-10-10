/* 설계 §5. 대진은 전부 클라이언트. 서버엔 pick 만. */
(function () {
  const all = JSON.parse(document.getElementById('photos').textContent);
  const $ = s => document.querySelector(s);
  const steps = { intro: $('[data-step=intro]'), match: $('[data-step=match]'), result: $('[data-step=result]') };
  const show = k => Object.entries(steps).forEach(([n, el]) => el.classList.toggle('hidden', n !== k));
  const byId = Object.fromEntries(all.map(p => [p.id, p]));

  // 32 → 16 → 8 → 4 → 2. 사진이 32장 안 되면 가능한 최대 2^n (개발용)
  let size = 32; while (size > all.length) size >>= 1;
  if (size < 2) { $('[data-start]').classList.add('hidden'); $('[data-not-enough]').classList.remove('hidden'); }
  const roundNo = n => 6 - Math.log2(n);            // 남은 인원 → 서버 round (32→1 … 2→5)
  const roundLabel = n => n === 2 ? W.t('worldcup.final') : n === 4 ? W.t('worldcup.semi') : W.t('worldcup.round', n);

  let pool = [], next = [], idx = 0, total = 0, done = 0;
  function start() {
    pool = all.map(p => p.id).sort(() => Math.random() - .5).slice(0, size);
    next = []; idx = 0; done = 0; total = size - 1;
    W.track('worldcup.start'); show('match'); render();
  }
  function render() {
    const [a, b] = [pool[idx], pool[idx + 1]];
    $('[data-side="0"] img').src = byId[a].url; $('[data-side="1"] img').src = byId[b].url;
    $('[data-progress]').textContent = W.t('worldcup.progress', roundLabel(pool.length), idx / 2 + 1, pool.length / 2);
    $('[data-bar]').style.width = (done / total * 100) + '%';
    // 다음 매치 프리로드
    [pool[idx + 2], pool[idx + 3]].filter(Boolean).forEach(id => { new Image().src = byId[id].url; });
  }
  function pick(side) {
    const w = pool[idx + side], l = pool[idx + 1 - side];
    W.api('/api/pick', 'POST', { sid: W.sid, round: roundNo(pool.length), winnerId: w, loserId: l }).catch(() => {});
    next.push(w); idx += 2; done++;
    const card = document.querySelector(`[data-side="${side}"]`); card.classList.add('win');
    setTimeout(() => {
      card.classList.remove('win');
      if (idx >= pool.length) { pool = next; next = []; idx = 0; }
      if (pool.length === 1) finish(pool[0]); else render();
    }, 180);
  }
  async function finish(champ) {
    $('[data-champion]').src = byId[champ].url;
    W.track('worldcup.finish', champ); show('result'); window.scrollTo(0, 0);
    loadRanking();
  }
  async function loadRanking() {
    try {
      const r = await (await fetch('/api/ranking')).json();
      document.querySelectorAll('[data-participants]').forEach(el => { el.dataset.i18n = 'worldcup.participants'; el.dataset.args = r.participants; el.textContent = W.t('worldcup.participants', r.participants); });
      const ol = $('[data-ranking]'); ol.textContent = '';
      r.top.forEach((p, i) => {
        const li = document.createElement('li'); li.className = 'rounded-xl overflow-hidden border border-gold bg-jasmine';
        const img = document.createElement('img'); img.src = p.url; img.alt = ''; img.className = 'w-full aspect-[3/4] object-cover';
        const cap = document.createElement('p'); cap.className = 'text-[11px] py-1 tabular ' + (i === 0 ? 'text-marigold' : 'text-ink/60');
        const rank = document.createElement('span'); rank.textContent = (i + 1) + ' · ';
        const wins = document.createElement('span'); wins.dataset.i18n = 'worldcup.wins'; wins.dataset.args = p.wins; wins.textContent = W.t('worldcup.wins', p.wins);
        cap.append(rank, wins); li.append(img, cap); ol.append(li);
      });
    } catch {}
  }
  document.querySelectorAll('[data-side]').forEach(b => b.addEventListener('click', () => pick(+b.dataset.side)));
  $('[data-start]').addEventListener('click', start);
  $('[data-retry]').addEventListener('click', start);
  $('[data-share-result]').addEventListener('click', () => { W.track('worldcup.share'); W.share(location.origin + '/c/worldcup?from=share'); });
  loadRanking();
})();
