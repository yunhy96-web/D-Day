import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { getAllCars, insertCar, updateCarById, deleteCarById } from '@/utils/database';

export interface Car {
  id: string;
  name: string;
  plateNumber?: string;
  mileage: number;
}

interface CarContextType {
  cars: Car[];
  selectedCarId: string | null;
  selectedCar: Car | null;
  isLoading: boolean;
  addCar: (car: Omit<Car, 'id'>) => Promise<void>;
  updateCar: (id: string, car: Partial<Car>) => Promise<void>;
  deleteCar: (id: string) => Promise<void>;
  selectCar: (id: string | null) => void;
}

const CarContext = createContext<CarContextType | undefined>(undefined);

export function CarProvider({ children }: { children: ReactNode }) {
  const [cars, setCars] = useState<Car[]>([]);
  const [selectedCarId, setSelectedCarId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 초기 데이터 로드
  useEffect(() => {
    loadCars();
  }, []);

  const loadCars = async () => {
    try {
      const dbCars = await getAllCars();
      const formattedCars: Car[] = dbCars.map(car => ({
        id: car.id,
        name: car.name,
        plateNumber: car.plateNumber || undefined,
        mileage: car.mileage,
      }));
      setCars(formattedCars);

      // 첫 번째 차량 자동 선택
      if (formattedCars.length > 0 && !selectedCarId) {
        setSelectedCarId(formattedCars[0].id);
      }
    } catch (error) {
      console.error('Failed to load cars:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const addCar = useCallback(async (carData: Omit<Car, 'id'>) => {
    const newCar: Car = {
      ...carData,
      id: Date.now().toString(),
    };

    try {
      await insertCar(newCar);
      setCars(prev => {
        const updated = [...prev, newCar];
        // 첫 번째 차량이면 자동 선택
        if (updated.length === 1) {
          setSelectedCarId(newCar.id);
        }
        return updated;
      });
    } catch (error) {
      console.error('Failed to add car:', error);
      throw error;
    }
  }, []);

  const updateCar = useCallback(async (id: string, carData: Partial<Car>) => {
    try {
      await updateCarById(id, carData);
      setCars(prev => prev.map(car =>
        car.id === id ? { ...car, ...carData } : car
      ));
    } catch (error) {
      console.error('Failed to update car:', error);
      throw error;
    }
  }, []);

  const deleteCar = useCallback(async (id: string) => {
    try {
      await deleteCarById(id);
      setCars(prev => {
        const updated = prev.filter(car => car.id !== id);
        // 선택된 차량이 삭제되면 첫 번째 차량 선택
        if (selectedCarId === id) {
          setSelectedCarId(updated.length > 0 ? updated[0].id : null);
        }
        return updated;
      });
    } catch (error) {
      console.error('Failed to delete car:', error);
      throw error;
    }
  }, [selectedCarId]);

  const selectCar = useCallback((id: string | null) => {
    setSelectedCarId(id);
  }, []);

  const selectedCar = cars.find(car => car.id === selectedCarId) || null;

  return (
    <CarContext.Provider value={{
      cars,
      selectedCarId,
      selectedCar,
      isLoading,
      addCar,
      updateCar,
      deleteCar,
      selectCar,
    }}>
      {children}
    </CarContext.Provider>
  );
}

export function useCar() {
  const context = useContext(CarContext);
  if (context === undefined) {
    throw new Error('useCar must be used within a CarProvider');
  }
  return context;
}
