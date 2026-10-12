/* 사진 업로드 공통 (설계 §15-6·§15-7): 브라우저에서 줄이기 + 진행률 있는 전송. 어드민·방명록이 같이 쓴다. */
(function () {
  /** 긴 변 maxSide 이하 JPEG. EXIF 회전 반영, 메타데이터는 남지 않는다 */
  async function resize(file, maxSide, quality) {
    const bmp = await createImageBitmap(file, { imageOrientation: 'from-image' });
    const k = Math.min(1, maxSide / Math.max(bmp.width, bmp.height));
    const c = document.createElement('canvas'); c.width = Math.round(bmp.width * k); c.height = Math.round(bmp.height * k);
    c.getContext('2d').drawImage(bmp, 0, 0, c.width, c.height); bmp.close?.();
    return new Promise((res, rej) => c.toBlob(b => b ? res(b) : rej(new Error('encode')), 'image/jpeg', quality));
  }

  /** fetch 는 업로드 진행률을 못 줘서 XHR. onProgress(0~1) */
  function send(url, body, contentType, onProgress) {
    return new Promise(res => {
      const x = new XMLHttpRequest();
      x.open('POST', url); x.setRequestHeader('Content-Type', contentType);
      x.upload.onprogress = e => { if (e.lengthComputable && onProgress) onProgress(e.loaded / e.total); };
      x.onload = () => { let json = {}; try { json = JSON.parse(x.responseText); } catch {} res({ ok: x.status >= 200 && x.status < 300, status: x.status, json }); };
      x.onerror = x.ontimeout = () => res({ ok: false, status: 0, json: {} });
      x.send(body);
    });
  }

  /** 작업 목록을 동시 n 개씩. task(i) 는 Promise<boolean> */
  async function pool(count, n, task) {
    let next = 0;
    const worker = async () => { while (next < count) { const i = next++; await task(i); } };
    await Promise.all(Array.from({ length: Math.min(n, count) }, worker));
  }

  window.Up = { resize, send, pool };
})();
