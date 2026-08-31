import { describe, it, expect } from 'vitest';
import { pack, packOnce, recommend, ORDERS } from '../src/pack';
import type { Box, Item, PackResult } from '../src/types';

const box = (w: number, d: number, h: number, wall = 0): Box => ({ w, d, h, wall });
const item = (w: number, d: number, h: number, qty = 1, extra: Partial<Item> = {}): Item =>
  ({ id: `${w}x${d}x${h}`, name: `${w}x${d}x${h}`, w, d, h, qty, gap: 0, ...extra });

// 공통 불변식: 유효 공간 안에 있고, 서로 겹치지 않는다 (gap 포함).
function assertValid(b: Box, r: PackResult) {
  const inner = { w: b.w - 2 * b.wall, d: b.d - 2 * b.wall, h: b.h - 2 * b.wall };
  for (const p of r.placed) {
    const g = p.item.gap;
    expect(p.pos.x - g).toBeGreaterThanOrEqual(0);
    expect(p.pos.y - g).toBeGreaterThanOrEqual(0);
    expect(p.pos.z - g).toBeGreaterThanOrEqual(0);
    expect(p.pos.x + p.dim.w + g).toBeLessThanOrEqual(inner.w);
    expect(p.pos.y + p.dim.d + g).toBeLessThanOrEqual(inner.d);
    expect(p.pos.z + p.dim.h + g).toBeLessThanOrEqual(inner.h);
  }
  for (let i = 0; i < r.placed.length; i++)
    for (let j = i + 1; j < r.placed.length; j++) {
      const a = r.placed[i], c = r.placed[j];
      const gap = Math.max(a.item.gap, c.item.gap);
      const overlap =
        a.pos.x < c.pos.x + c.dim.w + gap && c.pos.x < a.pos.x + a.dim.w + gap &&
        a.pos.y < c.pos.y + c.dim.d + gap && c.pos.y < a.pos.y + a.dim.d + gap &&
        a.pos.z < c.pos.z + c.dim.h + gap && c.pos.z < a.pos.z + a.dim.h + gap;
      expect(overlap, `${i} overlaps ${j}`).toBe(false);
    }
}

describe('pack', () => {
  it('1. 50³ × 8 → 100³ 상자에 전부, fillRate 1', () => {
    const b = box(100, 100, 100);
    const r = pack(b, [item(50, 50, 50, 8)]);
    assertValid(b, r);
    expect(r.placed).toHaveLength(8);
    expect(r.unplaced).toHaveLength(0);
    expect(r.fillRate).toBeCloseTo(1);
  });

  it('2. gap=5 이면 8개 못 들어감', () => {
    const b = box(100, 100, 100);
    const r = pack(b, [item(50, 50, 50, 8, { gap: 5 })]);
    assertValid(b, r);
    expect(r.placed).toHaveLength(1);
    expect(r.unplaced).toHaveLength(7);
  });

  it('3. wall=10 → 유효 80³ → 50³ 1개만', () => {
    const b = box(100, 100, 100, 10);
    const r = pack(b, [item(50, 50, 50, 8)]);
    assertValid(b, r);
    expect(r.placed).toHaveLength(1);
  });

  it('4. 상자보다 큰 물건 → unplaced', () => {
    const b = box(100, 100, 100);
    const r = pack(b, [item(120, 10, 10)]);
    expect(r.placed).toHaveLength(0);
    expect(r.unplaced).toHaveLength(1);
  });

  it('5. 회전 필요 케이스 / noRotate 면 실패', () => {
    const b = box(100, 50, 50);
    assertValid(b, pack(b, [item(50, 100, 50)]));
    expect(pack(b, [item(50, 100, 50)]).placed).toHaveLength(1);
    expect(pack(b, [item(50, 100, 50, 1, { noRotate: true })]).placed).toHaveLength(1); // 높이축 고정, 수평 회전은 허용
    expect(pack(b, [item(50, 50, 100, 1, { noRotate: true })]).placed).toHaveLength(0); // 눕혀야만 들어감 → 실패
  });
});

describe('M4', () => {
  it('6. fragile 위에는 아무것도 못 올림 → fragile 이 위로', () => {
    const b = box(100, 100, 100);
    const r = pack(b, [item(100, 100, 50, 1, { id: 'f', fragile: true }), item(100, 100, 50, 1, { id: 'n' })]);
    assertValid(b, r);
    expect(r.placed).toHaveLength(2);
    expect(r.placed.find(p => p.item.id === 'f')!.pos.z).toBe(50);
  });

  it('7. maxWeight 초과분은 unplaced', () => {
    const b = { ...box(100, 100, 100), maxWeight: 100 };
    const r = pack(b, [item(50, 50, 50, 8, { weight: 30 })]);
    expect(r.placed).toHaveLength(3);
    expect(r.unplaced).toHaveLength(5);
    expect(r.totalWeight).toBe(90);
  });

  it('8. 다중 정렬: 부피순 단일 시도로는 실패하는 케이스도 담김', () => {
    const b = box(100, 100, 100);
    const items = [item(80, 20, 90, 2), item(90, 20, 50, 1), item(10, 100, 90, 1)];
    const all = items.flatMap(i => Array<Item>(i.qty).fill(i));
    expect(packOnce(b, [...all].sort(ORDERS[0])).unplaced.length).toBeGreaterThan(0);
    const r = pack(b, items);
    assertValid(b, r);
    expect(r.unplaced).toHaveLength(0);
  });

  it('9. recommend: 전부 담기는 가장 작은 상자', () => {
    const boxes = [box(60, 60, 60), box(100, 100, 100), box(200, 200, 200)];
    const { box: b } = recommend(boxes, [item(50, 50, 50, 8)]);
    expect(b.w).toBe(100);
  });
});
