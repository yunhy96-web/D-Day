import { pack, recommend } from './pack';
import { createRenderer } from './render';
import type { Box, Item } from './types';

const $ = <T extends HTMLElement>(s: string) => document.querySelector<T>(s)!;
const form = $<HTMLFormElement>('#form');
const tbody = $<HTMLTableSectionElement>('#items');
const tpl = $<HTMLTemplateElement>('#row');
const { draw, setStep } = createRenderer($('#view'));
const stepEl = $<HTMLInputElement>('#step');

const COLORS = ['#60a5fa', '#f87171', '#34d399', '#fbbf24', '#a78bfa', '#fb923c', '#22d3ee', '#f472b6', '#a3e635', '#94a3b8'];
const colorOf = (id: string) => COLORS[Number(id) % COLORS.length];

// 우체국 규격 상자 (mm)
const PRESETS: [string, number, number, number][] = [
  ['우체국 1호', 220, 190, 90], ['우체국 2호', 270, 180, 150], ['우체국 3호', 350, 250, 100],
  ['우체국 4호', 410, 310, 280], ['우체국 5호', 480, 380, 340], ['우체국 6호', 520, 480, 400],
];
const presetEl = $<HTMLSelectElement>('#preset');
PRESETS.forEach(([n, w, d, h], i) => presetEl.add(new Option(`${n} ${w}×${d}×${h}`, String(i))));
const setBox = (w: number, d: number, h: number) =>
  (['bw', 'bd', 'bh'] as const).forEach((n, i) => ((form.elements.namedItem(n) as HTMLInputElement).value = String([w, d, h][i])));
presetEl.addEventListener('change', () => { const p = PRESETS[+presetEl.value]; if (p) setBox(p[1], p[2], p[3]); });

function readBox(): Box {
  const fd = new FormData(form);
  const mw = fd.get('maxWeight');
  return { w: +fd.get('bw')!, d: +fd.get('bd')!, h: +fd.get('bh')!, wall: +fd.get('wall')!, ...(mw ? { maxWeight: +mw } : {}) };
}

$('#recommend').addEventListener('click', () => {
  if (!form.reportValidity()) return;
  const base = readBox();
  const { box, result } = recommend(PRESETS.map(([, w, d, h]) => ({ ...base, w, d, h })), readItems());
  setBox(box.w, box.d, box.h);
  presetEl.value = String(PRESETS.findIndex(p => p[1] === box.w && p[2] === box.d && p[3] === box.h));
  form.requestSubmit();
  if (result.unplaced.length) $('#result').insertAdjacentHTML('afterbegin', '<div class="bad">전부 담기는 표준 상자 없음 — 가장 많이 담기는 상자 선택</div>');
});

let seq = 0;
function addRow(v: Partial<Item> = {}) {
  const row = tpl.content.firstElementChild!.cloneNode(true) as HTMLTableRowElement;
  row.dataset.id = String(seq++);
  for (const [k, val] of Object.entries(v)) {
    const input = row.querySelector<HTMLInputElement>(`[name=${k}]`);
    if (!input) continue;
    if (input.type === 'checkbox') input.checked = !!val; else input.value = String(val);
  }
  row.querySelector('.del')!.addEventListener('click', () => row.remove());
  tbody.appendChild(row);
}
$('#add').addEventListener('click', () => addRow());

function readItems(): Item[] {
  return [...tbody.rows].map(r => {
    const f = (n: string) => r.querySelector<HTMLInputElement>(`[name=${n}]`)!;
    return {
      id: r.dataset.id!, name: f('name').value,
      w: +f('w').value, d: +f('d').value, h: +f('h').value,
      qty: +f('qty').value, gap: +f('gap').value, noRotate: f('noRotate').checked, fragile: f('fragile').checked,
      ...(f('weight').value ? { weight: +f('weight').value } : {}),
    };
  });
}

form.addEventListener('submit', e => {
  e.preventDefault();
  const box = readBox();
  const items = readItems();
  localStorage.setItem('packfit', JSON.stringify({ box, items }));
  const r = pack(box, items);
  draw(box, r, colorOf);
  $('#player').hidden = false;
  stepEl.max = String(r.placed.length);
  showStep(r.placed.length);

  const total = items.reduce((s, i) => s + i.qty, 0);
  const missed = Object.entries(r.unplaced.reduce<Record<string, number>>((m, i) => (m[i.name] = (m[i.name] ?? 0) + 1, m), {}));
  $('#result').innerHTML = `
    <b>채움률 ${(r.fillRate * 100).toFixed(1)}%</b> · ${r.placed.length}/${total} 담김${r.totalWeight ? ` · ${(r.totalWeight / 1000).toFixed(2)}kg` : ''}
    ${missed.length ? `<div class="bad">못 담은 것: ${missed.map(([n, c]) => `${n} ×${c}`).join(', ')}</div>` : ''}
    <div id="legend">${items.map(i => `<span><i style="background:${colorOf(i.id)}"></i>${i.name} ${i.w}×${i.d}×${i.h}</span>`).join('')}</div>`;
});

// 단계별 재생
let timer = 0;
function showStep(n: number) {
  stepEl.value = String(n);
  setStep(n);
  $('#stepLabel').textContent = `${n} / ${stepEl.max}`;
}
stepEl.addEventListener('input', () => { clearInterval(timer); showStep(+stepEl.value); });
$('#play').addEventListener('click', () => {
  clearInterval(timer);
  let n = +stepEl.value >= +stepEl.max ? 0 : +stepEl.value;
  showStep(n);
  timer = window.setInterval(() => (n < +stepEl.max ? showStep(++n) : clearInterval(timer)), 600);
});

// 복원 or 샘플
const saved = (() => { try { return JSON.parse(localStorage.getItem('packfit') ?? ''); } catch { return null; } })();
if (saved) {
  for (const [k, v] of Object.entries(saved.box)) { const i = form.elements.namedItem(k.length === 1 ? 'b' + k : k) as HTMLInputElement | null; if (i) i.value = String(v); }
  (saved.items as Item[]).forEach(addRow);
} else {
  addRow({ name: '책', w: 220, d: 150, h: 30, qty: 4 });
  addRow({ name: '머그컵', w: 120, d: 90, h: 100, qty: 2, noRotate: true, fragile: true, weight: 350 });
  addRow({ name: '노트북', w: 320, d: 220, h: 20, qty: 1 });
}
form.requestSubmit();
