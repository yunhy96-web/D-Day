import { z } from 'zod'

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, { message: 'msg.error.required' })
    .email({ message: 'msg.error.invalid_email' }),
  password: z
    .string()
    .min(1, { message: 'msg.error.required' })
    .min(12, { message: 'msg.error.password_too_short' }),
})

export type LoginFormValues = z.infer<typeof loginSchema>
