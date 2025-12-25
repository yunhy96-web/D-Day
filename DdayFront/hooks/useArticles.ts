import { useState, useCallback, useRef } from 'react';
import { articleService } from '../services';
import { Article, AddArticleRequest, UpdateArticleRequest } from '../types/api';

interface ArticleFilters {
  articleType?: string;
  topic?: string;
  keyword?: string;
}

interface UseArticlesReturn {
  articles: Article[];
  isLoading: boolean;
  isLoadingMore: boolean;
  error: string | null;
  hasMore: boolean;
  fetchArticles: (filters?: ArticleFilters) => Promise<void>;
  loadMore: () => Promise<void>;
  createArticle: (data: AddArticleRequest) => Promise<Article | null>;
  updateArticle: (uuid: string, data: UpdateArticleRequest) => Promise<Article | null>;
  deleteArticle: (uuid: string) => Promise<boolean>;
  getArticle: (uuid: string) => Promise<Article | null>;
}

const PAGE_SIZE = 20;

export function useArticles(): UseArticlesReturn {
  const [articles, setArticles] = useState<Article[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const currentPage = useRef(0);
  const currentFilters = useRef<ArticleFilters | undefined>(undefined);

  const fetchArticles = useCallback(async (filters?: ArticleFilters) => {
    setIsLoading(true);
    setError(null);
    currentPage.current = 0;
    currentFilters.current = filters;
    try {
      const response = await articleService.getAll({ ...filters, page: 0, size: PAGE_SIZE });
      if (response.data) {
        setArticles(response.data.content);
        setHasMore(response.data.hasNext);
      }
    } catch (err) {
      setError('Failed to load posts.');
      console.error('Fetch articles error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loadMore = useCallback(async () => {
    if (isLoadingMore || !hasMore) return;

    setIsLoadingMore(true);
    try {
      const nextPage = currentPage.current + 1;
      const response = await articleService.getAll({
        ...currentFilters.current,
        page: nextPage,
        size: PAGE_SIZE,
      });
      if (response.data) {
        // 중복 제거: 기존 uuid와 겹치지 않는 새 글만 추가
        setArticles((prev) => {
          const existingUuids = new Set(prev.map((a) => a.uuid));
          const newArticles = response.data!.content.filter(
            (a) => !existingUuids.has(a.uuid)
          );
          return [...prev, ...newArticles];
        });
        setHasMore(response.data.hasNext);
        currentPage.current = nextPage;
      }
    } catch (err) {
      console.error('Load more articles error:', err);
    } finally {
      setIsLoadingMore(false);
    }
  }, [isLoadingMore, hasMore]);

  const createArticle = useCallback(async (data: AddArticleRequest): Promise<Article | null> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await articleService.create(data);
      if (response.data) {
        // 중복 방지: 같은 uuid가 없을 때만 추가
        setArticles((prev) => {
          if (prev.some((a) => a.uuid === response.data!.uuid)) {
            return prev;
          }
          return [response.data!, ...prev];
        });
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
    isLoadingMore,
    error,
    hasMore,
    fetchArticles,
    loadMore,
    createArticle,
    updateArticle,
    deleteArticle,
    getArticle,
  };
}
