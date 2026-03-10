import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { getAllSites, insertSite, updateSiteById, deleteSiteById, seedDefaultSite } from '@/utils/database';
import type { Site } from '@/types';

interface SiteContextType {
  sites: Site[];
  isLoading: boolean;
  addSite: (site: Omit<Site, 'id' | 'createdAt'>) => Promise<void>;
  updateSite: (id: string, data: Partial<Site>) => Promise<void>;
  deleteSite: (id: string) => Promise<void>;
  loadSites: () => Promise<void>;
}

const SiteContext = createContext<SiteContextType | undefined>(undefined);

export function SiteProvider({ children }: { children: ReactNode }) {
  const [sites, setSites] = useState<Site[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadSites = useCallback(async () => {
    try {
      await seedDefaultSite();
      const dbSites = await getAllSites();
      setSites(dbSites);
    } catch (error) {
      console.error('Failed to load sites:', error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSites();
  }, [loadSites]);

  const addSite = useCallback(async (siteData: Omit<Site, 'id' | 'createdAt'>) => {
    const newSite: Site = {
      ...siteData,
      id: Date.now().toString(),
      createdAt: new Date().toISOString(),
    };
    try {
      await insertSite(newSite);
      setSites((prev) => [...prev, newSite]);
    } catch (error) {
      console.error('Failed to add site:', error);
      throw error;
    }
  }, []);

  const updateSite = useCallback(async (id: string, data: Partial<Site>) => {
    try {
      await updateSiteById(id, data);
      setSites((prev) => prev.map((s) => (s.id === id ? { ...s, ...data } : s)));
    } catch (error) {
      console.error('Failed to update site:', error);
      throw error;
    }
  }, []);

  const deleteSite = useCallback(async (id: string) => {
    try {
      await deleteSiteById(id);
      setSites((prev) => prev.filter((s) => s.id !== id));
    } catch (error) {
      console.error('Failed to delete site:', error);
      throw error;
    }
  }, []);

  return (
    <SiteContext.Provider value={{ sites, isLoading, addSite, updateSite, deleteSite, loadSites }}>
      {children}
    </SiteContext.Provider>
  );
}

export function useSites() {
  const context = useContext(SiteContext);
  if (context === undefined) {
    throw new Error('useSites must be used within a SiteProvider');
  }
  return context;
}
