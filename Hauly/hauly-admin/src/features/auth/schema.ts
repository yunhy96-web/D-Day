import { z } from 'zod'

export const loginSchema = z.object({
  username: z
    .string()
    .min(1, { message: 'msg.error.required' })
    .min(3, { message: 'msg.error.username.invalid' })
    .max(32, { message: 'msg.error.username.invalid' })
    .regex(/^[a-zA-Z0-9_-]+$/, { message: 'msg.error.username.invalid' }),
  password: z
    .string()
    .min(1, { message: 'msg.error.required' })
    .min(12, { message: 'msg.error.password_too_short' }),
})

export type LoginFormValues = z.infer<typeof loginSchema>
