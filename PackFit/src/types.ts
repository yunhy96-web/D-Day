// 단위: mm, g (정수). 좌표 원점 = 상자 내벽 좌-앞-바닥 모서리.
export type Dim = { w: number; d: number; h: number };

export type Box = Dim & {
  wall: number;        // 내벽 여유(포장재). 6면 적용
  maxWeight?: number;
};

export type Item = Dim & {
  id: string;
  name: string;
  qty: number;
  weight?: number;
  gap: number;         // 물건 사이 최소 간격. 각 면 적용
  noRotate?: boolean;  // 높이 축 고정
  fragile?: boolean;
};

export type Placed = {
  item: Item;
  pos: { x: number; y: number; z: number };
  dim: Dim;            // 회전 적용, gap 미포함
  rotation: 0 | 1 | 2 | 3 | 4 | 5;
};

export type PackResult = {
  placed: Placed[];
  unplaced: Item[];
  fillRate: number;
  totalWeight: number;
};
