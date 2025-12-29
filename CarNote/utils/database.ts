import * as SQLite from 'expo-sqlite';

const DATABASE_NAME = 'carnote.db';

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

  // Cars 테이블
  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS cars (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      plateNumber TEXT,
      mileage INTEGER DEFAULT 0
    );
  `);

  // Records 테이블
  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS records (
      id TEXT PRIMARY KEY,
      carId TEXT NOT NULL,
      category TEXT NOT NULL,
      type TEXT NOT NULL,
      date TEXT NOT NULL,
      cost INTEGER,
      mileage INTEGER,
      location TEXT,
      fuelAmount REAL,
      memo TEXT,
      createdAt TEXT NOT NULL,
      FOREIGN KEY (carId) REFERENCES cars(id) ON DELETE CASCADE
    );
  `);
}

// Car CRUD
export async function getAllCars() {
  const database = await getDatabase();
  return database.getAllAsync<{
    id: string;
    name: string;
    plateNumber: string | null;
    mileage: number;
  }>('SELECT * FROM cars');
}

export async function insertCar(car: {
  id: string;
  name: string;
  plateNumber?: string;
  mileage: number;
}) {
  const database = await getDatabase();
  await database.runAsync(
    'INSERT INTO cars (id, name, plateNumber, mileage) VALUES (?, ?, ?, ?)',
    [car.id, car.name, car.plateNumber || null, car.mileage]
  );
}

export async function updateCarById(
  id: string,
  car: { name?: string; plateNumber?: string; mileage?: number }
) {
  const database = await getDatabase();
  const updates: string[] = [];
  const values: (string | number | null)[] = [];

  if (car.name !== undefined) {
    updates.push('name = ?');
    values.push(car.name);
  }
  if (car.plateNumber !== undefined) {
    updates.push('plateNumber = ?');
    values.push(car.plateNumber || null);
  }
  if (car.mileage !== undefined) {
    updates.push('mileage = ?');
    values.push(car.mileage);
  }

  if (updates.length > 0) {
    values.push(id);
    await database.runAsync(
      `UPDATE cars SET ${updates.join(', ')} WHERE id = ?`,
      values
    );
  }
}

export async function deleteCarById(id: string) {
  const database = await getDatabase();
  // 관련 레코드 먼저 삭제
  await database.runAsync('DELETE FROM records WHERE carId = ?', [id]);
  await database.runAsync('DELETE FROM cars WHERE id = ?', [id]);
}

// Record CRUD
export async function getAllRecords() {
  const database = await getDatabase();
  return database.getAllAsync<{
    id: string;
    carId: string;
    category: string;
    type: string;
    date: string;
    cost: number | null;
    mileage: number | null;
    location: string | null;
    fuelAmount: number | null;
    memo: string | null;
    createdAt: string;
  }>('SELECT * FROM records ORDER BY date DESC');
}

export async function insertRecord(record: {
  id: string;
  carId: string;
  category: string;
  type: string;
  date: string;
  cost?: number;
  mileage?: number;
  location?: string;
  fuelAmount?: number;
  memo?: string;
  createdAt: string;
}) {
  const database = await getDatabase();
  await database.runAsync(
    `INSERT INTO records (id, carId, category, type, date, cost, mileage, location, fuelAmount, memo, createdAt)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      record.id,
      record.carId,
      record.category,
      record.type,
      record.date,
      record.cost ?? null,
      record.mileage ?? null,
      record.location ?? null,
      record.fuelAmount ?? null,
      record.memo ?? null,
      record.createdAt,
    ]
  );
}

export async function updateRecordById(
  id: string,
  record: {
    category?: string;
    type?: string;
    date?: string;
    cost?: number;
    mileage?: number;
    location?: string;
    fuelAmount?: number;
    memo?: string;
  }
) {
  const database = await getDatabase();
  const updates: string[] = [];
  const values: (string | number | null)[] = [];

  if (record.category !== undefined) {
    updates.push('category = ?');
    values.push(record.category);
  }
  if (record.type !== undefined) {
    updates.push('type = ?');
    values.push(record.type);
  }
  if (record.date !== undefined) {
    updates.push('date = ?');
    values.push(record.date);
  }
  if (record.cost !== undefined) {
    updates.push('cost = ?');
    values.push(record.cost);
  }
  if (record.mileage !== undefined) {
    updates.push('mileage = ?');
    values.push(record.mileage);
  }
  if (record.location !== undefined) {
    updates.push('location = ?');
    values.push(record.location || null);
  }
  if (record.fuelAmount !== undefined) {
    updates.push('fuelAmount = ?');
    values.push(record.fuelAmount);
  }
  if (record.memo !== undefined) {
    updates.push('memo = ?');
    values.push(record.memo || null);
  }

  if (updates.length > 0) {
    values.push(id);
    await database.runAsync(
      `UPDATE records SET ${updates.join(', ')} WHERE id = ?`,
      values
    );
  }
}

export async function deleteRecordById(id: string) {
  const database = await getDatabase();
  await database.runAsync('DELETE FROM records WHERE id = ?', [id]);
}
