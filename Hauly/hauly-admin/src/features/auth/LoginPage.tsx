import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { useLogin, useMe } from './hooks'
import { loginSchema, type LoginFormValues } from './schema'
import { ApiError } from '@/lib/api/types'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { LanguageSwitcher } from '@/components/LanguageSwitcher'

export default function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/dashboard'

  const { data: me } = useMe()
  const loginMutation = useLogin()

  // Already authenticated → redirect
  useEffect(() => {
    if (me) navigate(redirectTo, { replace: true })
  }, [me, navigate, redirectTo])

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) })

  function onSubmit(values: LoginFormValues) {
    loginMutation.mutate(
      { username: values.username, password: values.password },
      { onSuccess: () => navigate(redirectTo, { replace: true }) }
    )
  }

  function getErrorMessage(): string | null {
    const err = loginMutation.error
    if (!err) return null
    if (err instanceof ApiError) {
      const i18nKey = `msg.error.${err.code}`
      const translated = t(i18nKey)
      // If key not found, i18next returns the key; fall back to raw message
      return translated !== i18nKey ? translated : err.message
    }
    return t('msg.error.network')
  }

  const errorMsg = getErrorMessage()

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-muted/40 px-4">
      <LanguageSwitcher className="absolute top-4 right-4" />

      <Card className="w-full max-w-sm">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl text-center">{t('auth.login.title')}</CardTitle>
          <CardDescription className="text-center">{t('auth.login.subtitle')}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {errorMsg && (
              <Alert variant="destructive">
                <AlertDescription>{errorMsg}</AlertDescription>
              </Alert>
            )}

            <div className="space-y-2">
              <Label htmlFor="username">{t('field.username.label')}</Label>
              <Input
                id="username"
                type="text"
                placeholder={t('field.username.placeholder')}
                autoComplete="username"
                autoCapitalize="off"
                spellCheck={false}
                {...register('username')}
              />
              {errors.username && (
                <p className="text-sm text-destructive">{t(errors.username.message ?? '')}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">{t('field.password.label')}</Label>
              <Input
                id="password"
                type="password"
                placeholder={t('field.password.placeholder')}
                autoComplete="current-password"
                {...register('password')}
              />
              {errors.password && (
                <p className="text-sm text-destructive">{t(errors.password.message ?? '')}</p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={loginMutation.isPending}>
              {loginMutation.isPending ? t('btn.logging_in') : t('btn.login')}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
