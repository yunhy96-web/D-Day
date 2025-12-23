import { api } from './api';
import { config } from '../config';
import { ApiResponse, CommonCode } from '../types/api';

export const commonCodeService = {
  // 게시글 타입 조회 (사이드바용)
  async getArticleTypes(lang: string = 'en'): Promise<CommonCode[]> {
    const response = await api.get<ApiResponse<CommonCode[]>>(
      `${config.endpoints.codes.articleTypes}?lang=${lang}`
    );
    return response.data.data || [];
  },

  // 게시글 주제 조회 (셀렉트박스용)
  async getArticleTopics(lang: string = 'en'): Promise<CommonCode[]> {
    const response = await api.get<ApiResponse<CommonCode[]>>(
      `${config.endpoints.codes.articleTopics}?lang=${lang}`
    );
    return response.data.data || [];
  },
};
