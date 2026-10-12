/* 설계 §15-2. 정답은 서버에만 있다. hint 는 응답에만 있고 화면엔 그리지 않는다. 출력은 textContent 만. */
(function () {
  const $ = s => document.querySelector(s);
  const steps = { intro: $('[data-step=intro]'), question: $('[data-step=question]'), result: $('[data-step=result]') };
  const show = k => { Object.entries(steps).forEach(([n, el]) => el.classList.toggle('hidden', n !== k)); window.scrollTo(0, 0); };
  let quiz = null, idx = 0, answers = [], level = null;

  console.log('%cHello, fellow developer 👀', 'font-size:14px;color:#4FA3E0',
    '\nNo answers in the response. Check "hint" in /api/quiz/start instead.');

  document.querySelectorAll('[data-level]').forEach(b => b.addEventListener('click', async () => {
    level = b.dataset.level; $('[data-msg]').textContent = '';
    try {
      const r = await W.api('/api/quiz/start', 'POST', { sid: W.sid, level });
      if (!r.ok) { $('[data-msg]').textContent = W.t('common.error'); return; }
      quiz = await r.json();
      if (!quiz.items.length) { $('[data-msg]').textContent = W.t('quiz.empty'); return; }
      idx = 0; answers = []; show('question'); render();
    } catch { $('[data-msg]').textContent = W.t('common.error'); }
  }));

  function render() {
    const q = quiz.items[idx], n = quiz.items.length;
    $('[data-progress]').textContent = W.t('quiz.progress', idx + 1, n, q.points);
    $('[data-question]').textContent = q.question;
    $('[data-bar]').style.width = (idx / n * 100) + '%';
    const ol = $('[data-choices]'); ol.textContent = '';
    q.choices.forEach(c => {
      const li = document.createElement('li'); li.className = 'border-b border-ink/10 last:border-0';
      const b = document.createElement('button'); b.type = 'button'; b.className = 'w-full py-4 text-[16px] transition-colors';
      b.textContent = c.text;
      b.addEventListener('click', () => choose(q.id, c.no, b), { once: true });
      li.append(b); ol.append(li);
    });
  }

  function choose(id, no, btn) {
    answers.push({ id, no });
    btn.classList.add('text-sky', 'font-semibold');
    $('[data-choices]').querySelectorAll('button').forEach(b => b.disabled = true);
    setTimeout(() => { idx++; idx < quiz.items.length ? render() : submit(); }, 220);
  }

  async function submit() {
    try {
      const r = await W.api(`/api/quiz/${quiz.attemptId}/submit`, 'POST', { answers });
      if (!r.ok) { show('intro'); $('[data-msg]').textContent = W.t('common.error'); return; }
      result(await r.json());
    } catch { show('intro'); $('[data-msg]').textContent = W.t('common.error'); }
  }

  function result(res) {
    const ratio = res.max ? res.score / res.max : 0;
    const comment = ratio >= .8 ? 'quiz.comment.high' : ratio >= .5 ? 'quiz.comment.mid' : 'quiz.comment.low';
    const set = (el, key, ...args) => { el.dataset.i18n = key; el.dataset.args = args.join('|'); el.textContent = W.t(key, ...args); };
    set($('[data-level-name]'), 'quiz.level.' + level);
    set($('[data-score]'), 'quiz.score', res.score, res.max);
    set($('[data-comment]'), comment);
    // 문제별 맞음/틀림만. 정답 보기는 알려주지 않는다
    const byId = Object.fromEntries(quiz.items.map(q => [q.id, q]));
    const ol = $('[data-results]'); ol.textContent = '';
    res.results.forEach((r, i) => {
      const li = document.createElement('li'); li.className = 'flex gap-3 py-2 text-[14px]';
      const mark = document.createElement('span'); mark.className = 'w-4 shrink-0 ' + (r.correct ? 'text-sky' : 'text-ink/35');
      mark.textContent = r.correct ? '○' : '×';
      const q = document.createElement('span'); q.className = r.correct ? '' : 'text-ink/50';
      q.textContent = (i + 1) + '. ' + (byId[r.id]?.question ?? '');
      li.append(mark, q); ol.append(li);
    });
    show('result');
  }

  $('[data-retry]').addEventListener('click', () => show('intro'));
})();
