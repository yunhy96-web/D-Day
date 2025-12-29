import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { getAllRecords, insertRecord, updateRecordById, deleteRecordById } from '@/utils/database';

export type RecordCategory = '정비' | '주유' | '기타';

export interface MaintenanceRecord {
  id: string;
  carId: string;
  category: RecordCategory;
  type: string; // 엔진오일, 주유, 세차 등
  date: Date;
  cost?: number;
  mileage?: number;
  location?: string;
  fuelAmount?: number;
  memo?: string;
  createdAt: Date;
}

interface RecordContextType {
  records: MaintenanceRecord[];
  isLoading: boolean;
  addRecord: (record: Omit<MaintenanceRecord, 'id' | 'createdAt'>) => Promise<void>;
  updateRecord: (id: string, record: Partial<MaintenanceRecord>) => Promise<void>;
  deleteRecord: (id: string) => Promise<void>;
  getRecordsByCarId: (carId: string) => MaintenanceRecord[];
  getRecentRecords: (carId: string, limit?: number) => MaintenanceRecord[];
  getLastMaintenance: (carId: string) => MaintenanceRecord | null;
  getUpcomingSchedule: (carId: string) => MaintenanceRecord | null;
  getMonthlyExpense: (carId: string) => number;
}

const RecordContext = createContext<RecordContextType | undefined>(undefined);

export function RecordProvider({ children }: { children: ReactNode }) {
  const [records, setRecords] = useState<MaintenanceRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // 초기 데이터 로드
  useEffect(() => {
    loadRecords();
  }, []);

  const loadRecords = async () => {
    try {
      const dbRecords = await getAllRecords();
      const formattedRecords: MaintenanceRecord[] = dbRecords.map(record => ({
        id: record.id,
        carId: record.carId,
        category: record.category as RecordCategory,
        type: record.type,
        date: new Date(record.date),
        cost: record.cost || undefined,
        mileage: record.mileage || undefined,
        location: record.location || undefined,
        fuelAmount: record.fuelAmount || undefined,
        memo: record.memo || undefined,
        createdAt: new Date(record.createdAt),
      }));
      setRecords(formattedRecords);
    } catch (error) {
      console.error('Failed to load records:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const addRecord = useCallback(async (recordData: Omit<MaintenanceRecord, 'id' | 'createdAt'>) => {
    const newRecord: MaintenanceRecord = {
      ...recordData,
      id: Date.now().toString(),
      createdAt: new Date(),
    };

    try {
      await insertRecord({
        id: newRecord.id,
        carId: newRecord.carId,
        category: newRecord.category,
        type: newRecord.type,
        date: newRecord.date.toISOString(),
        cost: newRecord.cost,
        mileage: newRecord.mileage,
        location: newRecord.location,
        fuelAmount: newRecord.fuelAmount,
        memo: newRecord.memo,
        createdAt: newRecord.createdAt.toISOString(),
      });
      setRecords(prev => [newRecord, ...prev]);
    } catch (error) {
      console.error('Failed to add record:', error);
      throw error;
    }
  }, []);

  const updateRecord = useCallback(async (id: string, recordData: Partial<MaintenanceRecord>) => {
    try {
      const dbData: Record<string, string | number | undefined> = {};
      if (recordData.category !== undefined) dbData.category = recordData.category;
      if (recordData.type !== undefined) dbData.type = recordData.type;
      if (recordData.date !== undefined) dbData.date = recordData.date.toISOString();
      if (recordData.cost !== undefined) dbData.cost = recordData.cost;
      if (recordData.mileage !== undefined) dbData.mileage = recordData.mileage;
      if (recordData.location !== undefined) dbData.location = recordData.location;
      if (recordData.fuelAmount !== undefined) dbData.fuelAmount = recordData.fuelAmount;
      if (recordData.memo !== undefined) dbData.memo = recordData.memo;

      await updateRecordById(id, dbData);
      setRecords(prev => prev.map(record =>
        record.id === id ? { ...record, ...recordData } : record
      ));
    } catch (error) {
      console.error('Failed to update record:', error);
      throw error;
    }
  }, []);

  const deleteRecord = useCallback(async (id: string) => {
    try {
      await deleteRecordById(id);
      setRecords(prev => prev.filter(record => record.id !== id));
    } catch (error) {
      console.error('Failed to delete record:', error);
      throw error;
    }
  }, []);

  const getRecordsByCarId = useCallback((carId: string) => {
    return records
      .filter(record => record.carId === carId)
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }, [records]);

  const getRecentRecords = useCallback((carId: string, limit: number = 5) => {
    return getRecordsByCarId(carId).slice(0, limit);
  }, [getRecordsByCarId]);

  // 마지막 정비: 오늘 이전의 기록 중 가장 최근
  const getLastMaintenance = useCallback((carId: string) => {
    const now = new Date();
    now.setHours(23, 59, 59, 999); // 오늘 끝까지 포함

    const pastRecords = getRecordsByCarId(carId)
      .filter(record => new Date(record.date) <= now);

    return pastRecords.length > 0 ? pastRecords[0] : null;
  }, [getRecordsByCarId]);

  // 다가오는 일정: 내일 이후의 기록 중 가장 가까운 것 (오늘은 제외)
  const getUpcomingSchedule = useCallback((carId: string) => {
    const tomorrow = new Date();
    tomorrow.setHours(0, 0, 0, 0);
    tomorrow.setDate(tomorrow.getDate() + 1); // 내일부터

    const futureRecords = getRecordsByCarId(carId)
      .filter(record => {
        const recordDate = new Date(record.date);
        recordDate.setHours(0, 0, 0, 0);
        return recordDate >= tomorrow;
      })
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()); // 가까운 순으로 정렬

    return futureRecords.length > 0 ? futureRecords[0] : null;
  }, [getRecordsByCarId]);

  const getMonthlyExpense = useCallback((carId: string) => {
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);

    return records
      .filter(record =>
        record.carId === carId &&
        new Date(record.date) >= startOfMonth &&
        record.cost
      )
      .reduce((sum, record) => sum + (record.cost || 0), 0);
  }, [records]);

  return (
    <RecordContext.Provider value={{
      records,
      isLoading,
      addRecord,
      updateRecord,
      deleteRecord,
      getRecordsByCarId,
      getRecentRecords,
      getLastMaintenance,
      getUpcomingSchedule,
      getMonthlyExpense,
    }}>
      {children}
    </RecordContext.Provider>
  );
}

export function useRecord() {
  const context = useContext(RecordContext);
  if (context === undefined) {
    throw new Error('useRecord must be used within a RecordProvider');
  }
  return context;
}
