import axios, { AxiosError } from 'axios'
import i18n from '@/lib/i18n'
import type { ApiErrorBody } from './types'
import { ApiError } from './types'

// In production swap via VITE_API_BASE_URL; in dev Vite proxy handles /api → localhost:8090
const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api'

export const apiClient = axios.create({
  baseURL,
  withCredentials: true, // Required: sends HttpOnly cookies (hauly_at / hauly_rt)
  headers: { 'Content-Type': 'application/json' },
})

// Forward the active UI language to the backend so endpoints that resolve labels
// from common_code (FULFILLMENT_STATUS, PAYMENT_STATUS, …) return the right column.
apiClient.interceptors.request.use((config) => {
  const lang = i18n.resolvedLanguage ?? i18n.language ?? 'ko'
  config.headers.set('Accept-Language', lang)
  return config
})

// Track whether a refresh is in flight to avoid refresh loops
let isRefreshing = false
let refreshSuccessListeners: Array<() => void> = []
let refreshFailedListeners: Array<() => void> = []

function notifyRefreshSuccess() {
  refreshSuccessListeners.forEach((fn) => fn())
  refreshSuccessListeners = []
}

function notifyRefreshFailed() {
  refreshFailedListeners.forEach((fn) => fn())
  refreshFailedListeners = []
}

apiClient.interceptors.response.use(
  (res) => res,
  async (error: AxiosError<ApiErrorBody>) => {
    const status = error.response?.status
    const originalUrl = error.config?.url ?? ''

    // Only intercept 401s that are NOT from the refresh endpoint itself
    if (status === 401 && !originalUrl.includes('/auth/refresh')) {
      if (!isRefreshing) {
        isRefreshing = true
        try {
          await apiClient.post('/auth/refresh')
          notifyRefreshSuccess()
          // Retry the original request
          return apiClient.request(error.config!)
        } catch {
          notifyRefreshFailed()
          // Just reject — React Router (ProtectedRoute) handles navigation to /login.
          // Do NOT call window.location.href here: it causes infinite reload loops
          // when the failing request originates from /login itself.
          return Promise.reject(error)
        } finally {
          isRefreshing = false
        }
      }

      // If already refreshing, queue and replay on success or reject on failure
      const originalConfig = error.config!
      return new Promise((resolve, reject) => {
        refreshSuccessListeners.push(() => {
          resolve(apiClient.request(originalConfig))
        })
        refreshFailedListeners.push(() => reject(error))
      })
    }

    // Normalise API errors into ApiError instances
    const body = error.response?.data
    if (body?.error) {
      throw new ApiError(body.error.code, body.error.message, status ?? 0)
    }

    if (!error.response) {
      throw new ApiError('NETWORK_ERROR', 'Network error', 0)
    }

    return Promise.reject(error)
  }
)
