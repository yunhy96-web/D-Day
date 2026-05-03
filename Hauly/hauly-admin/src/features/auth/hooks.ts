import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { login, fetchMe, logout, changePassword } from '@/lib/api/auth'
import type { MeResponse } from '@/lib/api/types'

export const ME_QUERY_KEY = ['me'] as const

export function useMe() {
  return useQuery<MeResponse>({
    queryKey: ME_QUERY_KEY,
    queryFn: fetchMe,
    retry: false,
  })
}

export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ username, password }: { username: string; password: string }) =>
      login(username, password),
    onSuccess: (data) => {
      // Cache the user from the login response body (in-memory only, no localStorage)
      queryClient.setQueryData(ME_QUERY_KEY, data.user)
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: logout,
    onSettled: () => {
      queryClient.clear()
      navigate('/login', { replace: true })
    },
  })
}

export function useChangePassword() {
  return useMutation({
    mutationFn: changePassword,
  })
}
