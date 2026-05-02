import * as SQLite from 'expo-sqlite';
import type { Site, Product, Snapshot, Change } from '@/types';

const DATABASE_NAME = 'checkstock.db';

let db: SQLite.SQLiteDatabase | null = null;

export async function getDatabase(): Promise<SQLite.SQLiteDatabase> {
  if (!db) {
    db = await SQLite.openDatabaseAsync(DATABASE_NAME);
    await initializeTables();
  }
  return db;
}

async function initializeTables(): Promise<void> {
  if (!db) return;

  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS sites (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      url TEXT NOT NULL,
      baseUrl TEXT NOT NULL,
      selector TEXT NOT NULL,
      countSelector TEXT,
      refreshInterval INTEGER DEFAULT 30,
      isActive INTEGER DEFAULT 1,
      createdAt TEXT NOT NULL
    );
  `);

  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS snapshots (
      id TEXT PRIMARY KEY,
      siteId TEXT NOT NULL,
      productCount INTEGER NOT NULL,
      products TEXT NOT NULL,
      checkedAt TEXT NOT NULL
    );
  `);

  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS changes (
      id TEXT PRIMARY KEY,
      siteId TEXT NOT NULL,
      oldCount INTEGER NOT NULL,
      newCount INTEGER NOT NULL,
      addedProducts TEXT,
      removedProducts TEXT,
      detectedAt TEXT NOT NULL
    );
  `);

  await db.execAsync(`
    CREATE INDEX IF NOT EXISTS idx_snapshots_site ON snapshots(siteId, checkedAt DESC);
  `);

  await db.execAsync(`
    CREATE INDEX IF NOT EXISTS idx_changes_site ON changes(siteId, detectedAt DESC);
  `);

  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS keyword_cache (
      siteId TEXT PRIMARY KEY,
      backendId TEXT NOT NULL,
      includeKeywords TEXT NOT NULL,
      excludeKeywords TEXT NOT NULL,
      cachedAt TEXT NOT NULL
    );
  `);

  // sites 테이블에 키워드 컬럼 추가 (idempotent)
  for (const sql of [
    "ALTER TABLE sites ADD COLUMN includeKeywords TEXT NOT NULL DEFAULT '[]'",
    "ALTER TABLE sites ADD COLUMN excludeKeywords TEXT NOT NULL DEFAULT '[]'",
  ]) {
    try {
      await db.execAsync(sql);
    } catch {
      // 컬럼이 이미 존재
    }
  }

  // keyword_cache → sites 1회 마이그레이션 (sites.includeKeywords가 비어있고 캐시가 있으면 복사)
  try {
    const cached = await db.getAllAsync<{
      siteId: string;
      includeKeywords: string;
      excludeKeywords: string;
    }>('SELECT siteId, includeKeywords, excludeKeywords FROM keyword_cache');
    for (const c of cached) {
      const site = await db.getFirstAsync<{ includeKeywords: string }>(
        'SELECT includeKeywords FROM sites WHERE id = ?',
        [c.siteId]
      );
      if (site && site.includeKeywords === '[]') {
        await db.runAsync(
          'UPDATE sites SET includeKeywords = ?, excludeKeywords = ? WHERE id = ?',
          [c.includeKeywords, c.excludeKeywords, c.siteId]
        );
      }
    }
  } catch {
    // 캐시 테이블 없거나 마이그레이션 불필요
  }
}

// ========== Sites ==========

interface SiteRow {
  id: string;
  name: string;
  url: string;
  baseUrl: string;
  selector: string;
  countSelector: string | null;
  refreshInterval: number;
  isActive: number;
  createdAt: string;
  includeKeywords: string | null;
  excludeKeywords: string | null;
}

function rowToSite(row: SiteRow): Site {
  const parseList = (raw: string | null): string[] => {
    if (!raw) return [];
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed.filter((s) => typeof s === 'string') : [];
    } catch {
      return [];
    }
  };
  return {
    id: row.id,
    name: row.name,
    url: row.url,
    baseUrl: row.baseUrl,
    selector: row.selector,
    countSelector: row.countSelector ?? undefined,
    refreshInterval: row.refreshInterval,
    isActive: row.isActive === 1,
    createdAt: row.createdAt,
    includeKeywords: parseList(row.includeKeywords),
    excludeKeywords: parseList(row.excludeKeywords),
  };
}

export async function getAllSites(): Promise<Site[]> {
  const database = await getDatabase();
  const rows = await database.getAllAsync<SiteRow>(
    'SELECT * FROM sites ORDER BY createdAt ASC'
  );
  return rows.map(rowToSite);
}

export async function getSiteById(id: string): Promise<Site | null> {
  const database = await getDatabase();
  const row = await database.getFirstAsync<SiteRow>('SELECT * FROM sites WHERE id = ?', [id]);
  return row ? rowToSite(row) : null;
}

export async function insertSite(site: Site) {
  const database = await getDatabase();
  await database.runAsync(
    'INSERT INTO sites (id, name, url, baseUrl, selector, countSelector, refreshInterval, isActive, createdAt, includeKeywords, excludeKeywords) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
    [
      site.id,
      site.name,
      site.url,
      site.baseUrl,
      site.selector,
      site.countSelector ?? null,
      site.refreshInterval,
      site.isActive ? 1 : 0,
      site.createdAt,
      JSON.stringify(site.includeKeywords ?? []),
      JSON.stringify(site.excludeKeywords ?? []),
    ]
  );
}

export async function updateSiteById(id: string, data: Partial<Site>) {
  const database = await getDatabase();
  const updates: string[] = [];
  const values: (string | number | null)[] = [];

  if (data.name !== undefined) { updates.push('name = ?'); values.push(data.name); }
  if (data.url !== undefined) { updates.push('url = ?'); values.push(data.url); }
  if (data.baseUrl !== undefined) { updates.push('baseUrl = ?'); values.push(data.baseUrl); }
  if (data.selector !== undefined) { updates.push('selector = ?'); values.push(data.selector); }
  if (data.countSelector !== undefined) { updates.push('countSelector = ?'); values.push(data.countSelector ?? null); }
  if (data.refreshInterval !== undefined) { updates.push('refreshInterval = ?'); values.push(data.refreshInterval); }
  if (data.isActive !== undefined) { updates.push('isActive = ?'); values.push(data.isActive ? 1 : 0); }
  if (data.includeKeywords !== undefined) { updates.push('includeKeywords = ?'); values.push(JSON.stringify(data.includeKeywords)); }
  if (data.excludeKeywords !== undefined) { updates.push('excludeKeywords = ?'); values.push(JSON.stringify(data.excludeKeywords)); }

  if (updates.length > 0) {
    values.push(id);
    await database.runAsync(`UPDATE sites SET ${updates.join(', ')} WHERE id = ?`, values);
  }
}

export async function deleteSiteById(id: string) {
  const database = await getDatabase();
  await database.runAsync('DELETE FROM changes WHERE siteId = ?', [id]);
  await database.runAsync('DELETE FROM snapshots WHERE siteId = ?', [id]);
  await database.runAsync('DELETE FROM sites WHERE id = ?', [id]);
}

// ========== Snapshots ==========

export async function insertSnapshot(snapshot: {
  id: string;
  siteId: string;
  productCount: number;
  products: Product[];
  checkedAt: string;
}) {
  const database = await getDatabase();
  await database.runAsync(
    'INSERT INTO snapshots (id, siteId, productCount, products, checkedAt) VALUES (?, ?, ?, ?, ?)',
    [snapshot.id, snapshot.siteId, snapshot.productCount, JSON.stringify(snapshot.products), snapshot.checkedAt]
  );
}

export async function getLatestSnapshot(siteId: string): Promise<Snapshot | null> {
  const database = await getDatabase();
  const row = await database.getFirstAsync<{
    id: string;
    siteId: string;
    productCount: number;
    products: string;
    checkedAt: string;
  }>('SELECT * FROM snapshots WHERE siteId = ? ORDER BY checkedAt DESC LIMIT 1', [siteId]);

  if (!row) return null;
  return { ...row, products: JSON.parse(row.products) };
}

// ========== Changes ==========

export async function insertChange(change: {
  id: string;
  siteId: string;
  oldCount: number;
  newCount: number;
  addedProducts: Product[];
  removedProducts: Product[];
  detectedAt: string;
}) {
  const database = await getDatabase();
  await database.runAsync(
    'INSERT INTO changes (id, siteId, oldCount, newCount, addedProducts, removedProducts, detectedAt) VALUES (?, ?, ?, ?, ?, ?, ?)',
    [
      change.id,
      change.siteId,
      change.oldCount,
      change.newCount,
      JSON.stringify(change.addedProducts),
      JSON.stringify(change.removedProducts),
      change.detectedAt,
    ]
  );
}

export async function getChangesBySiteId(siteId: string, limit = 50): Promise<Change[]> {
  const database = await getDatabase();
  const rows = await database.getAllAsync<{
    id: string;
    siteId: string;
    oldCount: number;
    newCount: number;
    addedProducts: string;
    removedProducts: string;
    detectedAt: string;
  }>('SELECT * FROM changes WHERE siteId = ? ORDER BY detectedAt DESC LIMIT ?', [siteId, limit]);

  return rows.map((row) => ({
    ...row,
    addedProducts: JSON.parse(row.addedProducts || '[]'),
    removedProducts: JSON.parse(row.removedProducts || '[]'),
  }));
}

// ========== Seed ==========

export async function seedDefaultSite() {
  const sites = await getAllSites();
  if (sites.length > 0) return;

  await insertSite({
    id: Date.now().toString(),
    name: 'RRL Double RL 데님',
    url: 'https://www.ralphlauren.co.kr/men/brands/double-rl?prefn1=CategoryCode&prefv1=%EB%8D%B0%EB%8B%98',
    baseUrl: 'https://www.ralphlauren.co.kr',
    selector: 'a.name-link.js-pdp-link',
    countSelector: 'input[name="totalProductsCount"]',
    refreshInterval: 30,
    isActive: true,
    createdAt: new Date().toISOString(),
    includeKeywords: ['빈티지'],
    excludeKeywords: ['에이버리 진'],
  });
}
