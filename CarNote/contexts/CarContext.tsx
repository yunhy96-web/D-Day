import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';

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
  addCar: (car: Omit<Car, 'id'>) => void;
  updateCar: (id: string, car: Partial<Car>) => void;
  deleteCar: (id: string) => void;
  selectCar: (id: string | null) => void;
}

const CarContext = createContext<CarContextType | undefined>(undefined);

export function CarProvider({ children }: { children: ReactNode }) {
  const [cars, setCars] = useState<Car[]>([]);
  const [selectedCarId, setSelectedCarId] = useState<string | null>(null);

  const addCar = useCallback((carData: Omit<Car, 'id'>) => {
    const newCar: Car = {
      ...carData,
      id: Date.now().toString(),
    };
    setCars(prev => {
      const updated = [...prev, newCar];
      // 첫 번째 차량이면 자동 선택
      if (updated.length === 1) {
        setSelectedCarId(newCar.id);
      }
      return updated;
    });
  }, []);

  const updateCar = useCallback((id: string, carData: Partial<Car>) => {
    setCars(prev => prev.map(car =>
      car.id === id ? { ...car, ...carData } : car
    ));
  }, []);

  const deleteCar = useCallback((id: string) => {
    setCars(prev => {
      const updated = prev.filter(car => car.id !== id);
      // 선택된 차량이 삭제되면 첫 번째 차량 선택
      if (selectedCarId === id) {
        setSelectedCarId(updated.length > 0 ? updated[0].id : null);
      }
      return updated;
    });
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
