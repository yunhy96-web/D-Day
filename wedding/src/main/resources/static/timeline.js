/* 설계 §15-3. 연애 시작 월 ~ 예식 월, 월 단위 눈금. 사진 있는 달에만 멈춘다. 출력은 textContent 만. */
(function () {
  const $ = s => document.querySelector(s);
  const items = JSON.parse(document.getElementById('timeline').textContent);
  const start = new Date(document.body.dataset.datingStart + 'T00:00:00');
  const end = new Date(document.body.dataset.datetime);
  const key = d => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');

  const months = [];
  for (let d = new Date(start.getFullYear(), start.getMonth(), 1); d <= end; d.setMonth(d.getMonth() + 1)) months.push(key(d));
  const byMonth = {};
  items.forEach(it => (byMonth[it.month] ||= []).push(it));
  const stops = months.map((m, i) => byMonth[m] ? i : -1).filter(i => i >= 0);

  const range = $('[data-range]'); range.max = months.length - 1;
  const THUMB = 11;   // app.css .tl-range 썸 반지름. 눈금을 썸 중심에 맞춘다
  const pos = i => `calc(${THUMB}px + (100% - ${THUMB * 2}px) * ${i / Math.max(1, months.length - 1)})`;

  // 눈금 · 연도
  months.forEach((m, i) => {
    const t = document.createElement('span');
    t.className = 'absolute bottom-0 w-px -translate-x-1/2 ' + (byMonth[m] ? 'h-4 bg-sky' : 'h-2 bg-ink/20');
    t.style.left = pos(i); $('[data-ticks]').append(t);
    const last = months.length - 1;   // 1월 라벨이 양 끝 라벨과 겹치지 않게 4칸 이상 떨어진 것만
    if (i === 0 || i === last || (m.endsWith('-01') && i >= 4 && last - i >= 4)) {
      const y = document.createElement('span'); y.className = 'absolute -translate-x-1/2 whitespace-nowrap';
      y.textContent = m.replace('-', '.'); y.style.left = pos(i); $('[data-years]').append(y);
    }
  });

  const days = Math.floor((new Date().setHours(0, 0, 0, 0) - start) / 864e5) + 1;
  const daysEl = $('[data-days]'); daysEl.dataset.i18n = 'timeline.days'; daysEl.dataset.args = days; daysEl.textContent = W.t('timeline.days', days);

  const nearest = i => stops.reduce((a, b) => Math.abs(b - i) < Math.abs(a - i) ? b : a, stops[0]);
  let shown = -1;
  function monthLabel(m) {
    const loc = W.lang === 'th' ? 'th-TH' : W.lang === 'en' ? 'en-US' : 'ko-KR';
    return new Intl.DateTimeFormat(loc, { calendar: 'gregory', year: 'numeric', month: 'long' }).format(new Date(m + '-01T00:00:00'));
  }
  function show(i) {
    $('[data-month]').textContent = monthLabel(months[i]);
    if (i === shown) return; shown = i;
    const box = $('[data-photos]'); box.textContent = ''; box.scrollLeft = 0;
    const list = byMonth[months[i]] || [];
    list.forEach(it => {
      const fig = document.createElement('figure'); fig.className = 'snap-center shrink-0 ' + (list.length > 1 ? 'w-[85%]' : 'w-full');
      const img = document.createElement('img'); img.src = it.url; img.alt = it.caption || ''; img.className = 'w-full aspect-[3/4] object-cover rounded-xl bg-jasmine';
      fig.append(img);
      if (it.caption) { const c = document.createElement('figcaption'); c.className = 'mt-3 text-[14px] text-ink/70'; c.textContent = it.caption; fig.append(c); }
      box.append(fig);
    });
  }

  if (!stops.length) {
    $('[data-empty]').classList.remove('hidden');
    range.value = 0; $('[data-month]').textContent = monthLabel(months[0]);
    return;
  }
  range.addEventListener('input', () => { const i = nearest(+range.value); range.value = i; show(i); });
  document.querySelectorAll('[data-lang]').forEach(b => b.addEventListener('click', () => { shown = -1; show(+range.value); }));
  range.value = stops[0]; show(stops[0]);
})();
