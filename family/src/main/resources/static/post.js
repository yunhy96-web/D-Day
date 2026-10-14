/* 글 상세: 하트 · 댓글 · 지우기 (설계 §5). 지우기는 두 번 눌러야 함(팝업 대신) */
document.addEventListener('DOMContentLoaded', () => {
  const { api, t, errText, toast } = window.App;
  const id = document.querySelector('[data-post]').dataset.post;

  const like = document.querySelector('[data-like]');
  like.addEventListener('click', async () => {
    const r = await api('PUT', `/api/posts/${id}/reaction`, {});
    if (!r.ok) return toast(errText(r));
    like.setAttribute('aria-pressed', r.json.liked);
    like.classList.toggle('text-red-500', r.json.liked);
    like.classList.toggle('border-red-300', r.json.liked);
    like.querySelector('svg').setAttribute('fill', r.json.liked ? 'currentColor' : 'none');
    like.querySelector('[data-like-count]').textContent = r.json.count;
    document.querySelector('[data-likers]').textContent = r.json.names.join(', ');
  });

  const form = document.querySelector('[data-comment]');
  if (form) {
    const ta = form.body;
    ta.addEventListener('input', () => { ta.style.height = 'auto'; ta.style.height = Math.min(ta.scrollHeight, 200) + 'px'; });
    form.addEventListener('submit', async e => {
      e.preventDefault();
      if (!ta.value.trim()) return;
      const btn = form.querySelector('button');
      btn.disabled = true;
      const r = await api('POST', `/api/posts/${id}/comments`, { body: ta.value });
      btn.disabled = false;
      if (r.ok) location.reload(); else toast(errText(r));
    });
  }

  /** 첫 탭: "한 번 더 누르면 지워져요", 3초 안에 두 번째 탭 */
  function twoStep(btn, run) {
    let armed = false, timer, label = btn.textContent;
    btn.addEventListener('click', () => {
      if (!armed) {
        armed = true; btn.textContent = t('common.tapAgain');
        timer = setTimeout(() => { armed = false; btn.textContent = label; }, 3000);
        return;
      }
      clearTimeout(timer);
      run();
    });
  }

  document.querySelectorAll('[data-del-comment]').forEach(b => twoStep(b, async () => {
    const r = await api('DELETE', `/api/comments/${b.dataset.delComment}`);
    if (r.ok) b.closest('li').remove(); else toast(errText(r));
  }));
  const del = document.querySelector('[data-del-post]');
  if (del) twoStep(del, async () => {
    const r = await api('DELETE', `/api/posts/${id}`);
    if (r.ok) location.href = '/'; else toast(errText(r));
  });
});
