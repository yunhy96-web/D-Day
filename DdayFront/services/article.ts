import { api } from './api';
import { config } from '../config';
import {
  ApiResponse,
  Article,
  AddArticleRequest,
  UpdateArticleRequest,
} from '../types/api';

interface ArticleFilters {
  articleType?: string;
  topic?: string;
}

export const articleService = {
  // 게시글 목록 조회 (타입/주제별 필터링 가능)
  async getAll(filters?: ArticleFilters): Promise<ApiResponse<Article[]>> {
    const params = new URLSearchParams();
    if (filters?.articleType) {
      params.append('articleType', filters.articleType);
    }
    if (filters?.topic) {
      params.append('topic', filters.topic);
    }
    const queryString = params.toString();
    const url = queryString
      ? `${config.endpoints.articles}?${queryString}`
      : config.endpoints.articles;
    const response = await api.get<ApiResponse<Article[]>>(url);
    return response.data;
  },

  // 게시글 단건 조회
  async getByUuid(uuid: string): Promise<ApiResponse<Article>> {
    const response = await api.get<ApiResponse<Article>>(
      `${config.endpoints.articles}/${uuid}`
    );
    return response.data;
  },

  // 게시글 등록
  async create(data: AddArticleRequest): Promise<ApiResponse<Article>> {
    const response = await api.post<ApiResponse<Article>>(
      config.endpoints.articles,
      data
    );
    return response.data;
  },

  // 게시글 수정
  async update(
    uuid: string,
    data: UpdateArticleRequest
  ): Promise<ApiResponse<Article>> {
    const response = await api.put<ApiResponse<Article>>(
      `${config.endpoints.articles}/${uuid}`,
      data
    );
    return response.data;
  },

  // 게시글 삭제
  async delete(uuid: string): Promise<ApiResponse<void>> {
    const response = await api.delete<ApiResponse<void>>(
      `${config.endpoints.articles}/${uuid}`
    );
    return response.data;
  },
};
