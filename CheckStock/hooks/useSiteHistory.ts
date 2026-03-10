import { useState, useCallback, useEffect } from 'react';
import { getChangesBySiteId } from '@/utils/database';
import type { Change } from '@/types';

export function useSiteHistory(siteId: string | null) {
  const [changes, setChanges] = useState<Change[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const loadChanges = useCallback(async () => {
    if (!siteId) return;
    setIsLoading(true);
    try {
      const result = await getChangesBySiteId(siteId);
      setChanges(result);
    } catch (error) {
      console.error('Failed to load changes:', error);
    } finally {
      setIsLoading(false);
    }
  }, [siteId]);

  useEffect(() => {
    loadChanges();
  }, [loadChanges]);

  return { changes, isLoading, reload: loadChanges };
}
