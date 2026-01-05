import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { getAllInsurers, insertInsurer, updateInsurerById, deleteInsurerById } from '@/utils/database';

export interface Insurer {
  id: string;
  name: string;
}

interface InsuranceContextType {
  insurers: Insurer[];
  isLoading: boolean;
  addInsurer: (name: string) => Promise<void>;
  updateInsurer: (id: string, name: string) => Promise<void>;
  deleteInsurer: (id: string) => Promise<void>;
}

const InsuranceContext = createContext<InsuranceContextType | undefined>(undefined);

export function InsuranceProvider({ children }: { children: ReactNode }) {
  const [insurers, setInsurers] = useState<Insurer[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // 초기 데이터 로드
  useEffect(() => {
    loadInsurers();
  }, []);

  const loadInsurers = async () => {
    try {
      const dbInsurers = await getAllInsurers();
      setInsurers(dbInsurers);
    } catch (error) {
      console.error('Failed to load insurers:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const addInsurer = useCallback(async (name: string) => {
    const newInsurer: Insurer = {
      id: Date.now().toString(),
      name,
    };

    try {
      await insertInsurer(newInsurer);
      setInsurers(prev => [...prev, newInsurer]);
    } catch (error) {
      console.error('Failed to add insurer:', error);
      throw error;
    }
  }, []);

  const updateInsurer = useCallback(async (id: string, name: string) => {
    try {
      await updateInsurerById(id, name);
      setInsurers(prev => prev.map(insurer =>
        insurer.id === id ? { ...insurer, name } : insurer
      ));
    } catch (error) {
      console.error('Failed to update insurer:', error);
      throw error;
    }
  }, []);

  const deleteInsurer = useCallback(async (id: string) => {
    try {
      await deleteInsurerById(id);
      setInsurers(prev => prev.filter(insurer => insurer.id !== id));
    } catch (error) {
      console.error('Failed to delete insurer:', error);
      throw error;
    }
  }, []);

  return (
    <InsuranceContext.Provider value={{
      insurers,
      isLoading,
      addInsurer,
      updateInsurer,
      deleteInsurer,
    }}>
      {children}
    </InsuranceContext.Provider>
  );
}

export function useInsurance() {
  const context = useContext(InsuranceContext);
  if (context === undefined) {
    throw new Error('useInsurance must be used within an InsuranceProvider');
  }
  return context;
}
