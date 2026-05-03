import { apiClient } from './client'

export interface TempUploadResponse {
  tempKey: string
  url: string
}

export async function uploadTempImage(file: File): Promise<TempUploadResponse> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await apiClient.post<TempUploadResponse>(
    '/intake/uploads/temp',
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return data
}

export async function deleteTempImage(tempKey: string): Promise<void> {
  // tempKey looks like "temp/123/abc.jpg" — strip the leading "temp/" segment for the path.
  if (!tempKey.startsWith('temp/')) {
    throw new Error('invalid temp key')
  }
  const path = tempKey.substring('temp/'.length)
  await apiClient.delete(`/intake/uploads/temp/${path}`)
}
