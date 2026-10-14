/* 글쓰기 (설계 §5·§8): 사진은 브라우저에서 줄여(보기용 2560·썸네일 600) 원본과 함께 바로 올린다. 글은 localStorage 임시 저장. */
document.addEventListener('DOMContentLoaded', () => {
  const { api, t, errText, toast } = window.App;
  const main = document.querySelector('[data-write]');
  const editId = main.dataset.postId || null;
  const DRAFT = 'draft:new';
  const MAX = 20, MAX_ORIGINAL = 40 * 1024 * 1024;
  const $ = s => document.querySelector(s);
  const title = $('[name=title]'), body = $('[name=body]'), date = $('[name=postDate]');
  const grid = $('[data-photos]'), addTile = grid.lastElementChild, status = $('[data-status]'), submit = $('[data-submit]');
  let kind = document.querySelector('[data-kind][aria-pressed=true]').dataset.kind;
  let vis = (document.querySelector('[data-vis][aria-pressed=true]') || { dataset: { vis: 'family' } }).dataset.vis;
  let dateTouched = !!editId, uploading = 0;

  // ---- 종류·공개 범위
  function setKind(k) {
    kind = k;
    document.querySelectorAll('[data-kind]').forEach(b => b.setAttribute('aria-pressed', String(b.dataset.kind === k)));
    $('[data-diary-only]').hidden = k !== 'diary';
    $('[data-kind-hint]').textContent = t(k === 'diary' ? 'write.diaryHint' : 'write.momentHint');
    body.rows = k === 'diary' ? 12 : 5;
    saveDraft();
  }
  document.querySelectorAll('[data-kind]').forEach(b => b.addEventListener('click', () => setKind(b.dataset.kind)));
  document.querySelectorAll('[data-vis]').forEach(b => b.addEventListener('click', () => {
    vis = b.dataset.vis;
    document.querySelectorAll('[data-vis]').forEach(x => x.setAttribute('aria-pressed', String(x === b)));
    saveDraft();
  }));

  // ---- 임시 저장 (새 글만)
  function saveDraft() {
    if (editId) return;
    try { localStorage.setItem(DRAFT, JSON.stringify({ kind, vis, title: title.value, body: body.value, date: dateTouched ? date.value : null })); } catch {}
  }
  if (!editId) {
    try {
      const d = JSON.parse(localStorage.getItem(DRAFT) || 'null');
      if (d && (d.title || d.body)) {
        title.value = d.title || ''; body.value = d.body || ''; vis = d.vis || 'family';
        if (d.date) { date.value = d.date; dateTouched = true; }
        document.querySelectorAll('[data-vis]').forEach(x => x.setAttribute('aria-pressed', String(x.dataset.vis === vis)));
        kind = d.kind || kind;
        toast(t('write.draftRestored'));
      }
    } catch {}
    const q = new URLSearchParams(location.search).get('kind');
    if (q === 'diary' || q === 'moment') kind = q;
  }
  setKind(kind);
  [title, body].forEach(el => el.addEventListener('input', saveDraft));
  date.addEventListener('change', () => { dateTouched = true; saveDraft(); });

  // ---- 사진
  grid.addEventListener('click', e => {
    const x = e.target.closest('[data-remove]');
    if (x) { x.closest('[data-media-id],[data-uploading]').remove(); refresh(); }
  });
  $('[data-file]').addEventListener('change', async e => {
    const files = [...e.target.files];
    e.target.value = '';
    const room = MAX - grid.querySelectorAll('[data-media-id],[data-uploading]').length;
    if (files.length > room) toast(t('write.tooMany', MAX));
    const tiles = files.slice(0, Math.max(0, room)).map(f => ({ f, el: tile(f) }));
    await pool(tiles.length, 2, i => upload(tiles[i].f, tiles[i].el));
  });

  function tile(file) {
    const el = document.createElement('div');
    el.className = 'relative aspect-square rounded-2xl overflow-hidden bg-soft';
    el.dataset.uploading = '1';
    el.innerHTML = '<img alt="" class="w-full h-full object-cover opacity-50"><div class="absolute inset-x-2 bottom-2 h-1.5 rounded-full bg-black/20 overflow-hidden"><div data-bar class="h-full w-0 bg-accent transition-all"></div></div>'
      + '<button type="button" data-remove class="absolute top-1.5 right-1.5 w-8 h-8 rounded-full bg-black/60 text-white text-lg leading-none">×</button>';
    el.querySelector('img').src = URL.createObjectURL(file);
    grid.insertBefore(el, addTile);
    uploading++; refresh();
    return el;
  }

  async function upload(file, el) {
    try {
      if (file.size > MAX_ORIGINAL) throw new Error('size');
      const [view, thumb, taken] = await Promise.all([resize(file, 2560, 0.9), resize(file, 600, 0.85), takenAt(file)]);
      if (!dateTouched && taken) { date.value = taken.slice(0, 10); dateTouched = true; }
      const q = `view=${view.size}&thumb=${thumb.size}` + (taken ? `&takenAt=${encodeURIComponent(taken)}` : '');
      const r = await send('/api/media?' + q, new Blob([view, thumb, file]), p => { el.querySelector('[data-bar]').style.width = (p * 100) + '%'; });
      if (!r.ok) throw new Error(r.json.code || 'upload');
      if (!el.isConnected) return;   // 올리는 중에 지움
      delete el.dataset.uploading;
      el.dataset.mediaId = r.json.id;
      el.querySelector('img').classList.remove('opacity-50');
      el.querySelector('[data-bar]').parentElement.remove();
    } catch (err) {
      el.remove();
      toast(t(err.message === 'size' ? 'write.photoTooBig' : 'write.photoFailed'));
    } finally {
      uploading--; refresh();
    }
  }

  function refresh() {
    submit.disabled = uploading > 0;
    status.textContent = uploading > 0 ? t('write.uploading', uploading) : '';
  }

  /** 긴 변 maxSide 이하 JPEG. EXIF 회전 반영, 메타데이터는 남지 않는다 */
  async function resize(file, maxSide, quality) {
    const bmp = await createImageBitmap(file, { imageOrientation: 'from-image' });
    const k = Math.min(1, maxSide / Math.max(bmp.width, bmp.height));
    const c = document.createElement('canvas');
    c.width = Math.round(bmp.width * k); c.height = Math.round(bmp.height * k);
    c.getContext('2d').drawImage(bmp, 0, 0, c.width, c.height);
    bmp.close?.();
    return new Promise((res, rej) => c.toBlob(b => b ? res(b) : rej(new Error('encode')), 'image/jpeg', quality));
  }

  /** JPEG EXIF DateTimeOriginal('2026:10:14 13:05:00') → '2026-10-14T13:05:00'. 없으면 파일 수정 시각(현지) */
  async function takenAt(file) {
    try {
      const v = new DataView(await file.slice(0, 256 * 1024).arrayBuffer());
      if (v.getUint16(0) === 0xFFD8) {
        for (let o = 2; o + 4 < v.byteLength;) {
          const marker = v.getUint16(o), len = v.getUint16(o + 2);
          if (marker === 0xFFE1 && v.getUint32(o + 4) === 0x45786966) {          // 'Exif'
            const tiff = o + 10, le = v.getUint16(tiff) === 0x4949;
            const u16 = p => v.getUint16(p, le), u32 = p => v.getUint32(p, le);
            const find = (ifd, tag) => { const n = u16(ifd); for (let i = 0; i < n; i++) { const e = ifd + 2 + i * 12; if (u16(e) === tag) return e; } return 0; };
            const exifPtr = find(tiff + u32(tiff + 4), 0x8769);
            const e = exifPtr && find(tiff + u32(exifPtr + 8), 0x9003);
            if (e) {
              let s = ''; const at = tiff + u32(e + 8);
              for (let i = 0; i < 19; i++) s += String.fromCharCode(v.getUint8(at + i));
              const m = s.match(/^(\d{4}):(\d{2}):(\d{2}) (\d{2}:\d{2}:\d{2})$/);
              if (m && m[1] !== '0000') return `${m[1]}-${m[2]}-${m[3]}T${m[4]}`;
            }
            break;
          }
          if ((marker & 0xFF00) !== 0xFF00) break;
          o += 2 + len;
        }
      }
    } catch {}
    if (!file.lastModified) return null;
    const d = new Date(file.lastModified);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 19);
  }

  /** fetch 는 업로드 진행률을 못 줘서 XHR */
  function send(url, blob, onProgress) {
    return new Promise(res => {
      const x = new XMLHttpRequest();
      x.open('POST', url);
      x.setRequestHeader('Content-Type', 'application/octet-stream');
      x.upload.onprogress = e => { if (e.lengthComputable) onProgress(e.loaded / e.total); };
      x.onload = () => { let json = {}; try { json = JSON.parse(x.responseText); } catch {} res({ ok: x.status >= 200 && x.status < 300, json }); };
      x.onerror = () => res({ ok: false, json: {} });
      x.send(blob);
    });
  }

  async function pool(count, n, task) {
    let next = 0;
    await Promise.all(Array.from({ length: Math.min(n, count) }, async () => { while (next < count) await task(next++); }));
  }

  // ---- 올리기
  submit.addEventListener('click', async () => {
    const mediaIds = [...grid.querySelectorAll('[data-media-id]')].map(el => +el.dataset.mediaId);
    if (!body.value.trim() && !mediaIds.length) return toast(t('err.empty'));
    submit.disabled = true;
    const payload = { kind, title: title.value, body: body.value, visibility: vis, postDate: date.value, mediaIds };
    const r = editId ? await api('PUT', `/api/posts/${editId}`, payload) : await api('POST', '/api/posts', payload);
    submit.disabled = false;
    if (!r.ok) return toast(errText(r));
    if (!editId) try { localStorage.removeItem(DRAFT); } catch {}
    location.href = `/p/${editId || r.json.id}`;
  });
});
