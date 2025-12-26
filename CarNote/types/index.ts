export interface Car {
  id: string;
  name: string;
  mileage: number;
  createdAt: Date;
}

export interface MaintenanceRecord {
  id: string;
  carId: string;
  type: MaintenanceType;
  date: Date;
  mileage: number;
  cost: number;
  description?: string;
  location?: string;
  createdAt: Date;
}

export type MaintenanceType =
  | 'oil_change'
  | 'tire_rotation'
  | 'brake_service'
  | 'battery'
  | 'air_filter'
  | 'transmission'
  | 'coolant'
  | 'inspection'
  | 'other';

export const MaintenanceTypeLabels: Record<MaintenanceType, string> = {
  oil_change: '엔진오일 교환',
  tire_rotation: '타이어 교체',
  brake_service: '브레이크 정비',
  battery: '배터리 교체',
  air_filter: '에어필터 교환',
  transmission: '미션오일 교환',
  coolant: '냉각수 교환',
  inspection: '정기 점검',
  other: '기타',
};
