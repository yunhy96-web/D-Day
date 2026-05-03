import { QueryClient } from '@tanstack/react-query'

/**
 * Tighter defaults to limit React Query observer accumulation when third-party
 * extensions cause repeated re-renders. The Heap snapshot diagnosed in the
 * memory regression showed 127k+ Query observers piling up; shrinking gcTime
 * and disabling focus-refetch keeps the cache footprint bounded.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,           // 30s — same as before
      gcTime: 60 * 1000,              // 1 min (was default 5 min): inactive caches drop sooner
      refetchOnWindowFocus: false,    // browser-extension DOM mutations were triggering refetch storms
      refetchOnReconnect: false,      // explicit refetch handlers cover reconnect for queries that need it
      retry: (failureCount, error) => {
        // Do not retry on 4xx errors
        if (error instanceof Error && 'status' in error) {
          const status = (error as { status: number }).status
          if (status >= 400 && status < 500) return false
        }
        return failureCount < 2
      },
    },
  },
})
