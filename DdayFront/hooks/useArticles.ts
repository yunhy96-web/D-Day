import { useState, useCallback } from 'react';
import { articleService } from '../services';
import { Article, AddArticleRequest, UpdateArticleRequest } from '../types/api';

interface UseArticlesReturn {
  articles: Article[];
  isLoading: boolean;
  error: string | null;
  fetchArticles: () => Promise<void>;
  createArticle: (data: AddArticleRequest) => Promise<Article | null>;
  updateArticle: (uuid: string, data: UpdateArticleRequest) => Promise<Article | null>;
  deleteArticle: (uuid: string) => Promise<boolean>;
  getArticle: (uuid: string) => Promise<Article | null>;
}

export function useArticles(): UseArticlesReturn {
  const [articles, setArticles] = useState<Article[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchArticles = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await articleService.getAll();
      if (response.data) {
        setArticles(response.data);
      }
    } catch (err) {
      setError('Failed to load articles.');
      console.error('Fetch articles error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const createArticle = useCallback(async (data: AddArticleRequest): Promise<Article | null> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await articleService.create(data);
      if (response.data) {
        setArticles((prev) => [response.data!, ...prev]);
        return response.data;
      }
      return null;
    } catch (err) {
      setError('Failed to create article.');
      console.error('Create article error:', err);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const updateArticle = useCallback(
    async (uuid: string, data: UpdateArticleRequest): Promise<Article | null> => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await articleService.update(uuid, data);
        if (response.data) {
          setArticles((prev) =>
            prev.map((article) =>
              article.uuid === uuid ? response.data! : article
            )
          );
          return response.data;
        }
        return null;
      } catch (err) {
        setError('Failed to update article.');
        console.error('Update article error:', err);
        return null;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const deleteArticle = useCallback(async (uuid: string): Promise<boolean> => {
    setIsLoading(true);
    setError(null);
    try {
      await articleService.delete(uuid);
      setArticles((prev) => prev.filter((article) => article.uuid !== uuid));
      return true;
    } catch (err) {
      setError('Failed to delete article.');
      console.error('Delete article error:', err);
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const getArticle = useCallback(async (uuid: string): Promise<Article | null> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await articleService.getByUuid(uuid);
      return response.data || null;
    } catch (err) {
      setError('Failed to load article.');
      console.error('Get article error:', err);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    articles,
    isLoading,
    error,
    fetchArticles,
    createArticle,
    updateArticle,
    deleteArticle,
    getArticle,
  };
}
