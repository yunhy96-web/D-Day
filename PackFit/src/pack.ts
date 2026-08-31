import type { Box, Dim, Item, PackResult, Placed } from './types';

// EP-BFD: Extreme-Point + Best-Fit-Decreasing. 순수 함수·결정적.
// 간격 처리: 상자는 wall 만큼 축소, 물건은 gap 만큼 팽창 → 알고리즘은 간격을 모른다.
type P = { x: number; y: number; z: number };
type Rot = Placed['rotation'];
const ROTS: ((d: Dim) => Dim)[] = [
  d => ({ w: d.w, d: d.d, h: d.h }), d => ({ w: d.d, d: d.w, h: d.h }),            // 0,1: 높이 유지
  d => ({ w: d.w, d: d.h, h: d.d }), d => ({ w: d.h, d: d.w, h: d.d }),
  d => ({ w: d.d, d: d.h, h: d.w }), d => ({ w: d.h, d: d.d, h: d.w }),
];
const SUPPORT = 0.7; // 바닥면의 70% 이상 지지
const vol = (d: Dim) => d.w * d.d * d.h;

// 정렬 순서 후보. fragile 은 항상 뒤로(위에 올라가도록).
export const ORDERS: ((a: Item, b: Item) => number)[] = [
  (a, b) => vol(b) - vol(a),
  (a, b) => Math.max(b.w, b.d, b.h) - Math.max(a.w, a.d, a.h),
  (a, b) => b.h - a.h,
  (a, b) => b.w * b.d - a.w * a.d,
];

export function pack(box: Box, items: Item[]): PackResult {
  const all = items.flatMap(i => Array<Item>(i.qty).fill(i));
  let best: PackResult | undefined;
  for (const order of ORDERS) {
    const q = [...all].sort((a, b) => Number(!!a.fragile) - Number(!!b.fragile) || order(a, b));
    const r = packOnce(box, q);
    if (!best || r.placed.length > best.placed.length || (r.placed.length === best.placed.length && r.fillRate > best.fillRate)) best = r;
    if (r.unplaced.length === 0) break; // 전부 담겼으면 더 볼 필요 없음(정렬 순서대로 우선)
  }
  return best!;
}

// 표준 상자 목록에서 전부 담기는 가장 작은 상자. 없으면 가장 많이 담기는 상자.
export function recommend(boxes: Box[], items: Item[]): { box: Box; result: PackResult } {
  const tried = [...boxes].sort((a, b) => vol(a) - vol(b)).map(box => ({ box, result: pack(box, items) }));
  return tried.find(t => t.result.unplaced.length === 0) ?? tried.reduce((a, b) => (b.result.placed.length > a.result.placed.length ? b : a));
}

export function packOnce(box: Box, queue: Item[]): PackResult {
  const inner = { w: box.w - 2 * box.wall, d: box.d - 2 * box.wall, h: box.h - 2 * box.wall };
  // 팽창된 박스(gap 포함)로 충돌/경계 판정
  const placedG: { pos: P; dim: Dim; fragile: boolean }[] = [];
  const placed: Placed[] = [];
  const unplaced: Item[] = [];
  let eps: P[] = [{ x: 0, y: 0, z: 0 }];
  let weight = 0;

  const fits = (p: P, d: Dim) =>
    p.x + d.w <= inner.w && p.y + d.d <= inner.d && p.z + d.h <= inner.h &&
    !placedG.some(o =>
      p.x < o.pos.x + o.dim.w && o.pos.x < p.x + d.w &&
      p.y < o.pos.y + o.dim.d && o.pos.y < p.y + d.d &&
      p.z < o.pos.z + o.dim.h && o.pos.z < p.z + d.h);

  // 바닥 지지율 검사 + fragile 위 적재 금지
  const supported = (p: P, d: Dim) => {
    if (p.z === 0) return true;
    let area = 0;
    for (const o of placedG) {
      if (o.pos.z + o.dim.h !== p.z) continue;
      const ox = Math.min(p.x + d.w, o.pos.x + o.dim.w) - Math.max(p.x, o.pos.x);
      const oy = Math.min(p.y + d.d, o.pos.y + o.dim.d) - Math.max(p.y, o.pos.y);
      if (ox <= 0 || oy <= 0) continue;
      if (o.fragile) return false;
      area += ox * oy;
    }
    return area >= SUPPORT * d.w * d.d;
  };

  for (const it of queue) {
    if (box.maxWeight != null && weight + (it.weight ?? 0) > box.maxWeight) { unplaced.push(it); continue; }
    const g = it.gap;
    const rots = it.noRotate ? [0, 1] : [0, 1, 2, 3, 4, 5];
    let best: { p: P; d: Dim; r: Rot } | undefined;
    for (const p of eps)
      for (const r of rots as Rot[]) {
        const raw = ROTS[r](it);
        const d = { w: raw.w + 2 * g, d: raw.d + 2 * g, h: raw.h + 2 * g };
        if (!fits(p, d) || !supported(p, d)) continue;
        // 낮은 z → 낮은 y → 낮은 x 우선
        if (!best || p.z < best.p.z || (p.z === best.p.z && (p.y < best.p.y || (p.y === best.p.y && p.x < best.p.x))))
          best = { p, d, r };
      }
    if (!best) { unplaced.push(it); continue; }

    const { p, d, r } = best;
    weight += it.weight ?? 0;
    placedG.push({ pos: p, dim: d, fragile: !!it.fragile });
    placed.push({ item: it, rotation: r, dim: ROTS[r](it), pos: { x: p.x + g, y: p.y + g, z: p.z + g } });
    // ponytail: EP 갱신은 단순 버전 — 새 EP 3개 추가, 기존 박스 내부에 갇힌 EP 제거. n>500이면 EP 투영(projection) 추가
    eps = eps.filter(e => e !== p).concat(
      { x: p.x + d.w, y: p.y, z: p.z }, { x: p.x, y: p.y + d.d, z: p.z }, { x: p.x, y: p.y, z: p.z + d.h })
      .filter(e => !placedG.some(o =>
        e.x >= o.pos.x && e.x < o.pos.x + o.dim.w &&
        e.y >= o.pos.y && e.y < o.pos.y + o.dim.d &&
        e.z >= o.pos.z && e.z < o.pos.z + o.dim.h));
  }

  return {
    placed, unplaced,
    fillRate: placed.reduce((s, p) => s + vol(p.dim), 0) / vol(inner),
    totalWeight: weight,
  };
}
