import { Article } from '@/types/api';

// Timezone to language code mapping
const TIMEZONE_LANG_MAP: Record<string, string> = {
  // 한국
  'Asia/Seoul': 'ko',
  // 태국
  'Asia/Bangkok': 'th',
  // 기본값 (영어권)
  'UTC': 'en',
  'America/New_York': 'en',
  'America/Los_Angeles': 'en',
  'Europe/London': 'en',
};

/**
 * timezone에서 언어 코드 추출
 */
export function getLanguageFromTimezone(timezone: string | null | undefined): string {
  if (!timezone) return 'en';
  return TIMEZONE_LANG_MAP[timezone] || 'en';
}

/**
 * Article에서 사용자 언어에 맞는 제목 반환
 */
export function getLocalizedTitle(article: Article, timezone: string | null | undefined): string {
  const lang = getLanguageFromTimezone(timezone);

  // 번역이 완료된 경우에만 번역본 사용
  if (article.translationStatus !== 'COMPLETED') {
    return article.title;
  }

  switch (lang) {
    case 'ko':
      return article.titleKo || article.title;
    case 'th':
      return article.titleTh || article.title;
    default:
      return article.title;
  }
}

/**
 * Article에서 사용자 언어에 맞는 내용 반환
 */
export function getLocalizedContent(article: Article, timezone: string | null | undefined): string {
  const lang = getLanguageFromTimezone(timezone);

  // 번역이 완료된 경우에만 번역본 사용
  if (article.translationStatus !== 'COMPLETED') {
    return article.content;
  }

  switch (lang) {
    case 'ko':
      return article.contentKo || article.content;
    case 'th':
      return article.contentTh || article.content;
    default:
      return article.content;
  }
}
