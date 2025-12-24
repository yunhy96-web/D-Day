import { api } from './api';
import { config } from '../config';
import {
  ApiResponse,
  PageResponse,
  Article,
  AddArticleRequest,
  UpdateArticleRequest,
} from '../types/api';

interface ArticleFilters {
  articleType?: string;
  topic?: string;
  page?: number;
  size?: number;
}

export const articleService = {
  // 게시글 목록 조회 (타입/주제별 필터링 가능, 페이지네이션 지원)
  async getAll(filters?: ArticleFilters): Promise<ApiResponse<PageResponse<Article>>> {
    const params = new URLSearchParams();
    if (filters?.articleType) {
      params.append('articleType', filters.articleType);
    }
    if (filters?.topic) {
      params.append('topic', filters.topic);
    }
    params.append('page', String(filters?.page ?? 0));
    params.append('size', String(filters?.size ?? 20));

    const url = `${config.endpoints.articles}?${params.toString()}`;
    const response = await api.get<ApiResponse<PageResponse<Article>>>(url);
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
