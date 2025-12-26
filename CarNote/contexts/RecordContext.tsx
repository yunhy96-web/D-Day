import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';

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
  addRecord: (record: Omit<MaintenanceRecord, 'id' | 'createdAt'>) => void;
  updateRecord: (id: string, record: Partial<MaintenanceRecord>) => void;
  deleteRecord: (id: string) => void;
  getRecordsByCarId: (carId: string) => MaintenanceRecord[];
  getRecentRecords: (carId: string, limit?: number) => MaintenanceRecord[];
  getLastMaintenance: (carId: string) => MaintenanceRecord | null;
  getUpcomingSchedule: (carId: string) => MaintenanceRecord | null;
  getMonthlyExpense: (carId: string) => number;
}

const RecordContext = createContext<RecordContextType | undefined>(undefined);

export function RecordProvider({ children }: { children: ReactNode }) {
  const [records, setRecords] = useState<MaintenanceRecord[]>([]);

  const addRecord = useCallback((recordData: Omit<MaintenanceRecord, 'id' | 'createdAt'>) => {
    const newRecord: MaintenanceRecord = {
      ...recordData,
      id: Date.now().toString(),
      createdAt: new Date(),
    };
    setRecords(prev => [newRecord, ...prev]);
  }, []);

  const updateRecord = useCallback((id: string, recordData: Partial<MaintenanceRecord>) => {
    setRecords(prev => prev.map(record =>
      record.id === id ? { ...record, ...recordData } : record
    ));
  }, []);

  const deleteRecord = useCallback((id: string) => {
    setRecords(prev => prev.filter(record => record.id !== id));
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
