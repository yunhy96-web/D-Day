export interface Product {
  name: string;
  url: string;
}

export interface Site {
  id: string;
  name: string;
  url: string;
  baseUrl: string;
  selector: string;
  countSelector?: string;
  refreshInterval: number;
  isActive: boolean;
  createdAt: string;
}

export interface Snapshot {
  id: string;
  siteId: string;
  productCount: number;
  products: Product[];
  checkedAt: string;
}

export interface Change {
  id: string;
  siteId: string;
  oldCount: number;
  newCount: number;
  addedProducts: Product[];
  removedProducts: Product[];
  detectedAt: string;
}
