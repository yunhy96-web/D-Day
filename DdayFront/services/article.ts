import { api } from './api';
import { config } from '../config';
import {
  ApiResponse,
  Article,
  AddArticleRequest,
  UpdateArticleRequest,
} from '../types/api';

export const articleService = {
  // 게시글 목록 조회
  async getAll(): Promise<ApiResponse<Article[]>> {
    const response = await api.get<ApiResponse<Article[]>>(
      config.endpoints.articles
    );
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
