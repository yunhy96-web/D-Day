/* 설계 §15-1. 어드민. 출력은 textContent 만(퀴즈·캡션도 결국 하객 화면에 나가는 값). */
(function () {
  const $ = s => document.querySelector(s);
  const json = (path, method, body) => fetch(path, { method, headers: { 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined });
  const el = (tag, cls, text) => { const e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; };

  // ---- 로그인
  const login = $('[data-login]');
  if (login) {
    login.addEventListener('submit', async e => {
      e.preventDefault();
      const r = await json('/admin/api/login', 'POST', { username: login.username.value, password: login.password.value });
      if (r.ok) location.reload();
      else $('[data-login-msg]').textContent = r.status === 429 ? '잠시 후 다시 (10분에 5회)' : '아이디·비밀번호가 틀렸거나 ADMIN_PW_HASH 가 비어 있다';
    });
    return;
  }
  $('[data-logout]').addEventListener('click', async () => { await json('/admin/api/logout', 'POST', {}); location.reload(); });
  const guard = r => { if (r.status === 401) location.reload(); return r; };

  // ---- 탭
  const tabs = [...document.querySelectorAll('[data-tab]')];
  function showTab(name) {
    tabs.forEach(t => { const on = t.dataset.tab === name; t.classList.toggle('border-sky', on); t.classList.toggle('border-transparent', !on); t.classList.toggle('text-ink/50', !on); });
    document.querySelectorAll('[data-panel]').forEach(p => p.classList.toggle('hidden', p.dataset.panel !== name));
    try { sessionStorage.adminTab = name; } catch {}
  }
  tabs.forEach(t => t.addEventListener('click', () => showTab(t.dataset.tab)));
  let firstTab = 'photos'; try { firstTab = sessionStorage.adminTab || firstTab; } catch {}
  showTab(firstTab);

  // ---- 업로드 진행 (설계 §15-6): 장마다 진행률 + 전체 n / N, 동시 2장, 실패분만 재시도
  let uploading = false;
  window.addEventListener('beforeunload', e => { if (uploading) { e.preventDefault(); e.returnValue = ''; } });
  async function runUploads(jobs, after) {
    const box = $('[data-up]'), list = $('[data-up-list]'), retry = $('[data-up-retry]');
    uploading = true; box.classList.remove('hidden'); retry.classList.add('hidden'); list.textContent = ''; $('[data-up-summary]').textContent = '';
    let finished = 0, ok = 0; const failed = [];
    const update = () => { $('[data-up-count]').textContent = `${finished} / ${jobs.length}`; $('[data-up-total]').style.width = (finished / jobs.length * 100) + '%'; };
    const rows = jobs.map(j => {
      const li = el('li', 'grid grid-cols-[1fr_120px_48px] gap-3 items-center');
      const bar = el('div', 'up-bar'); bar.append(el('i'));
      const st = el('span', 'text-ink/50 text-right', '대기');
      li.append(el('span', 'truncate', j.name), bar, st); list.append(li);
      return { bar: bar.firstChild, st };
    });
    update();
    await Up.pool(jobs.length, 2, async i => {
      const r = rows[i]; r.st.textContent = '…';
      let res; try { res = await jobs[i].run(p => r.bar.style.width = (p * 100) + '%'); } catch (e) { res = { ok: false, code: e.message }; }
      if (res.status === 401) location.reload();
      finished++;
      if (res.ok) { ok++; r.st.textContent = '✓'; r.st.className = 'text-sky text-right'; r.bar.style.width = '100%'; }
      else { failed.push(jobs[i]); r.st.textContent = '✗ ' + (res.code || res.status || ''); r.st.className = 'text-red-500 text-right truncate'; }
      update();
    });
    uploading = false;
    $('[data-up-summary]').textContent = `${ok}장 성공` + (failed.length ? ` · ${failed.length}장 실패` : '');
    if (failed.length) { retry.classList.remove('hidden'); retry.onclick = () => runUploads(failed, after); }
    after();
  }
  const sendJob = (url, body, type, onP) => Up.send(url, body, type, onP).then(r => ({ ok: r.ok, status: r.status, code: r.json.code }));

  // ---- 갤러리·월드컵 사진
  const pf = $('[data-photo-form]'), grid = $('[data-photo-grid]');
  pf.addEventListener('submit', e => {
    e.preventDefault();
    const wc = pf.worldcup.checked ? 'Y' : 'N';
    const jobs = [...pf.files.files].map(f => ({ name: f.name, run: async onP => {
      const view = await Up.resize(f, 2560, .92), thumb = await Up.resize(f, 600, .8);
      return sendJob(`/admin/api/photos?view=${view.size}&thumb=${thumb.size}&worldcup=${wc}`, new Blob([view, thumb, f]), 'application/octet-stream', onP);
    } }));
    pf.files.value = '';
    runUploads(jobs, loadPhotos);
  });
  async function loadPhotos() {
    const list = await (await guard(await fetch('/admin/api/photos'))).json();
    const wc = list.filter(p => p.worldcup).length;
    $('[data-photo-stat]').textContent = `총 ${list.length}장 · 월드컵 후보 ${wc}장` + (wc < 32 ? ` (32강은 32장 필요, 지금은 ${2 ** Math.floor(Math.log2(Math.max(1, wc)))}강)` : ' (랜덤 32장)');
    grid.textContent = '';
    list.forEach((ph, i) => {
      const li = el('li', 'space-y-1 text-[12px]'); li.dataset.id = ph.id;
      const fig = el('div', 'relative cursor-grab');
      const img = el('img', 'w-full aspect-square object-cover rounded-lg'); img.src = ph.thumb; img.alt = '';
      fig.append(img, el('span', 'absolute top-1 left-1 px-1.5 rounded bg-night/60 text-white tabular', i === 0 ? '1 · 메인' : String(i + 1)));
      const wcl = el('label', 'flex gap-1 items-center'); const cb = el('input'); cb.type = 'checkbox'; cb.checked = ph.worldcup;
      cb.addEventListener('change', async () => { await guard(await json('/admin/api/photos/' + ph.id, 'PUT', { worldcup: cb.checked })); loadPhotos(); });
      wcl.append(cb, document.createTextNode('월드컵'));
      const act = el('p', 'space-x-2');
      const top = el('button', 'text-sky', '맨 앞으로'); top.type = 'button'; top.addEventListener('click', () => { grid.prepend(li); saveOrder(); });
      const del = el('button', 'text-ink/45', '삭제'); del.type = 'button';
      del.addEventListener('click', async () => { if (confirm('원본까지 삭제할까?')) { await guard(await json('/admin/api/photos/' + ph.id, 'DELETE')); loadPhotos(); } });
      if (i > 0) act.append(top); act.append(del);
      li.append(fig, wcl, act); grid.append(li);
    });
  }
  async function saveOrder() {
    const ids = [...grid.children].map(li => +li.dataset.id);
    await guard(await json('/admin/api/photos/order', 'PUT', { ids }));
    loadPhotos();
  }
  if (window.Sortable) Sortable.create(grid, { animation: 150, delay: 180, delayOnTouchOnly: true, filter: 'input,button', preventOnFilter: false, onEnd: saveOrder });

  // ---- 방명록 관리
  let gbPage = 0;
  async function loadGuestbook(more) {
    gbPage = more ? gbPage + 1 : 0;
    const r = await (await guard(await fetch('/admin/api/guestbook?page=' + gbPage))).json();
    const ol = $('[data-gb-list]'); if (!more) ol.textContent = '';
    if (!more && !r.items.length) ol.append(el('li', 'text-ink/45', '아직 글이 없다'));
    r.items.forEach(g => {
      const li = el('li', 'py-3 border-b border-ink/10' + (g.hidden ? ' opacity-40' : ''));
      li.append(el('p', 'font-medium', `${g.name} · ${g.createdAt}` + (g.hidden ? ' (숨김)' : '')));
      li.append(el('p', 'mt-1 whitespace-pre-line break-words', g.message));
      if (g.photos.length) {
        const box = el('div', 'mt-2 flex gap-2');
        g.photos.forEach(ph => {
          const w = el('div', 'relative');
          const im = el('img', 'w-20 h-20 object-cover rounded'); im.src = ph.thumb; im.alt = '';
          const x = el('button', 'absolute -top-2 -right-2 w-6 h-6 rounded-full bg-night/70 text-white', '×'); x.type = 'button';
          x.addEventListener('click', async () => { if (confirm('이 사진을 삭제할까?')) { await guard(await json('/admin/api/guestbook/photo/' + ph.id, 'DELETE')); loadGuestbook(); } });
          w.append(im, x); box.append(w);
        });
        li.append(box);
      }
      const t = el('button', 'mt-2 text-sky', g.hidden ? '되돌리기' : '숨기기'); t.type = 'button';
      t.addEventListener('click', async () => { await guard(await json('/admin/api/guestbook/' + g.id, 'PUT', { hidden: !g.hidden })); loadGuestbook(); });
      li.append(t); ol.append(li);
    });
    $('[data-gb-more]').classList.toggle('hidden', !r.hasNext);
  }
  $('[data-gb-more]').addEventListener('click', () => loadGuestbook(true));

  // ---- 퀴즈
  const qf = $('[data-quiz-form]');
  const choiceBox = $('[data-choice-inputs]');
  for (let i = 0; i < 4; i++) {
    const row = el('label', 'flex gap-2 items-center');
    const radio = el('input'); radio.type = 'radio'; radio.name = 'answer'; radio.value = i; radio.required = true;
    const input = el('input', 'flex-1 p-2 rounded border border-ink/15 bg-transparent'); input.name = 'choice'; input.maxLength = 80;
    input.placeholder = '보기 ' + (i + 1) + (i < 2 ? '' : ' (선택)');
    row.append(radio, input); choiceBox.append(row);
  }
  const LEVELS = [['덜친함', 1, 2], ['중간친함', 3, 4], ['매우친함', 4, 5]];
  let quizzes = [];

  async function loadQuiz() {
    quizzes = await (await guard(await fetch('/admin/api/quiz'))).json();
    const per = p => quizzes.filter(q => q.points === p).length;
    $('[data-quiz-stat]').textContent = `총 ${quizzes.length}문제 · ` + [1, 2, 3, 4, 5].map(p => `${p}점 ${per(p)}`).join(' · ')
      + '  |  ' + LEVELS.map(([n, a, b]) => `${n} 풀 ${quizzes.filter(q => q.points >= a && q.points <= b).length}/10`).join(' · ');
    const ol = $('[data-quiz-list]'); ol.textContent = '';
    quizzes.forEach(q => {
      const li = el('li', 'py-3 border-b border-ink/10');
      li.append(el('p', 'font-medium', `[${q.points}점] #${q.id} ${q.question}`));
      const ul = el('ul', 'mt-1 pl-4 text-ink/70');
      q.choices.forEach((c, i) => ul.append(el('li', i === q.answerNo ? 'text-sky font-semibold' : '', (i === q.answerNo ? '✓ ' : '· ') + c)));
      li.append(ul);
      if (q.hint) li.append(el('p', 'mt-1 text-ink/45', '힌트: ' + q.hint));
      const act = el('p', 'mt-1 space-x-4');
      const edit = el('button', 'text-sky', '수정'); edit.type = 'button'; edit.addEventListener('click', () => fill(q));
      const del = el('button', 'text-ink/45', '삭제'); del.type = 'button';
      del.addEventListener('click', async () => { if (confirm('삭제할까?')) { await guard(await json('/admin/api/quiz/' + q.id, 'DELETE')); loadQuiz(); } });
      act.append(edit, del); li.append(act); ol.append(li);
    });
  }
  function fill(q) {
    qf.quizId.value = q ? q.id : ''; qf.question.value = q ? q.question : ''; qf.points.value = q ? q.points : 3; qf.hint.value = q?.hint || '';
    qf.querySelectorAll('[name=choice]').forEach((inp, i) => inp.value = q?.choices[i] || '');
    qf.querySelectorAll('[name=answer]').forEach((r, i) => r.checked = q ? i === q.answerNo : false);
    $('[data-quiz-submit]').textContent = q ? '수정 저장' : '추가';
    $('[data-quiz-cancel]').classList.toggle('hidden', !q);
    if (q) qf.scrollIntoView({ behavior: 'smooth' });
  }
  $('[data-quiz-cancel]').addEventListener('click', () => fill(null));
  qf.addEventListener('submit', async e => {
    e.preventDefault();
    const inputs = [...qf.querySelectorAll('[name=choice]')];
    const answer = +qf.querySelector('[name=answer]:checked').value;
    if (!inputs[answer].value.trim()) { $('[data-quiz-msg]').textContent = '정답 보기가 비어 있다'; return; }
    // 빈 보기는 빼고, 정답 인덱스를 그만큼 당긴다
    const kept = inputs.map((inp, i) => ({ i, v: inp.value.trim() })).filter(c => c.v);
    const body = { question: qf.question.value, choices: kept.map(c => c.v), answerNo: kept.findIndex(c => c.i === answer), points: +qf.points.value, hint: qf.hint.value };
    const id = qf.quizId.value;
    const r = await guard(await json(id ? '/admin/api/quiz/' + id : '/admin/api/quiz', id ? 'PUT' : 'POST', body));
    if (!r.ok) { $('[data-quiz-msg]').textContent = '저장 실패: ' + ((await r.json().catch(() => ({}))).code || r.status); return; }
    $('[data-quiz-msg]').textContent = id ? '수정했다' : '추가했다';
    fill(null); loadQuiz();
  });

  // ---- 타임라인
  const tf = $('[data-tl-form]');
  tf.addEventListener('submit', e => {
    e.preventDefault();
    const month = tf.month.value, caption = tf.caption.value;
    const jobs = [...tf.files.files].map(f => ({ name: f.name, run: async onP => {
      const blob = await Up.resize(f, 2560, .92);
      return sendJob('/admin/api/timeline?' + new URLSearchParams({ month, caption }), blob, 'image/jpeg', onP);
    } }));
    tf.files.value = ''; tf.caption.value = '';
    runUploads(jobs, loadTimeline);
  });

  async function loadTimeline() {
    const items = await (await guard(await fetch('/admin/api/timeline'))).json();
    const box = $('[data-tl-list]'); box.textContent = '';
    if (!items.length) { box.append(el('p', 'text-ink/45', '아직 사진이 없다')); return; }
    let month = null, grid = null;
    items.forEach(it => {
      if (it.month !== month) {
        month = it.month; box.append(el('h3', 'mt-6 font-semibold', month));
        grid = el('div', 'mt-2 grid grid-cols-2 sm:grid-cols-3 gap-3'); box.append(grid);
      }
      const card = el('div', 'space-y-1');
      const img = el('img', 'w-full aspect-[3/4] object-cover rounded-lg'); img.src = it.url; img.alt = '';
      const m = el('input', 'w-full p-1 rounded border border-ink/15 bg-transparent text-[13px]'); m.type = 'month'; m.value = it.month;
      m.min = document.body.dataset.monthMin; m.max = document.body.dataset.monthMax;
      const cap = el('input', 'w-full p-1 rounded border border-ink/15 bg-transparent text-[13px]'); cap.value = it.caption || ''; cap.maxLength = 60; cap.placeholder = '설명';
      const act = el('p', 'space-x-3 text-[13px]');
      const save = el('button', 'text-sky', '저장'); save.type = 'button';
      save.addEventListener('click', async () => {
        const r = await guard(await json('/admin/api/timeline/' + it.id, 'PUT', { month: m.value, caption: cap.value }));
        save.textContent = r.ok ? '저장됨' : '실패'; setTimeout(loadTimeline, 600);
      });
      const del = el('button', 'text-ink/45', '삭제'); del.type = 'button';
      del.addEventListener('click', async () => { if (confirm('삭제할까?')) { await guard(await json('/admin/api/timeline/' + it.id, 'DELETE')); loadTimeline(); } });
      act.append(save, del); card.append(img, m, cap, act); grid.append(card);
    });
  }

  loadPhotos(); loadTimeline(); loadQuiz(); loadGuestbook();
})();
