import { useState, useCallback, useEffect } from 'react';
import { commonCodeService } from '../services';
import { CommonCode } from '../types/api';

interface UseCommonCodesReturn {
  articleTypes: CommonCode[];
  articleTopics: CommonCode[];
  isLoading: boolean;
  error: string | null;
  fetchArticleTypes: (lang?: string) => Promise<void>;
  fetchArticleTopics: (lang?: string) => Promise<void>;
}

export function useCommonCodes(): UseCommonCodesReturn {
  const [articleTypes, setArticleTypes] = useState<CommonCode[]>([]);
  const [articleTopics, setArticleTopics] = useState<CommonCode[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchArticleTypes = useCallback(async (lang: string = 'en') => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await commonCodeService.getArticleTypes(lang);
      setArticleTypes(data);
    } catch (err) {
      setError('Failed to load article types.');
      console.error('Fetch article types error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fetchArticleTopics = useCallback(async (lang: string = 'en') => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await commonCodeService.getArticleTopics(lang);
      setArticleTopics(data);
    } catch (err) {
      setError('Failed to load article topics.');
      console.error('Fetch article topics error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchArticleTypes();
    fetchArticleTopics();
  }, [fetchArticleTypes, fetchArticleTopics]);

  return {
    articleTypes,
    articleTopics,
    isLoading,
    error,
    fetchArticleTypes,
    fetchArticleTopics,
  };
}
