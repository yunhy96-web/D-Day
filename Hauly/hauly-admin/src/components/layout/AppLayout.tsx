import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  KeyRound,
  LayoutDashboard,
  LogOut,
  Menu,
  Package,
  PlusCircle,
  Globe,
  Wallet,
} from 'lucide-react'
import { useIsAdmin, useLogout, useMe, useUpdateLanguagePreference } from '@/features/auth/hooks'
import { ChangePasswordModal } from '@/features/auth/ChangePasswordModal'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'

const LANGUAGES = [
  { code: 'ko', key: 'lang.ko' },
  { code: 'en', key: 'lang.en' },
  { code: 'th', key: 'lang.th' },
] as const

export default function AppLayout() {
  const { t, i18n } = useTranslation()
  const logoutMutation = useLogout()
  const { data: me } = useMe()
  const isAdmin = useIsAdmin()
  const langMutation = useUpdateLanguagePreference()
  const location = useLocation()
  const [pwOpen, setPwOpen] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)

  // Close mobile drawer whenever the route changes (link click) so the user
  // doesn't land on the new page with the menu still covering it.
  useEffect(() => {
    setDrawerOpen(false)
  }, [location.pathname])

  // 계정 디폴트 언어가 설정되어 있고 현재 i18n과 다르면 동기화 (로그인 직후 1회).
  useEffect(() => {
    if (me?.preferredLanguage && me.preferredLanguage !== i18n.resolvedLanguage) {
      i18n.changeLanguage(me.preferredLanguage)
    }
    // me.preferredLanguage 변경 시에만 트리거.
  }, [me?.preferredLanguage]) // eslint-disable-line react-hooks/exhaustive-deps

  function changeLanguage(code: string) {
    i18n.changeLanguage(code)
    if (code === 'ko' || code === 'en' || code === 'th') {
      langMutation.mutate(code)
    }
  }

  const navItem = (to: string, label: string, Icon: typeof LayoutDashboard) => (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        cn(
          'flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors',
          isActive
            ? 'bg-primary text-primary-foreground'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground',
        )
      }
    >
      <Icon className="h-4 w-4" />
      {label}
    </NavLink>
  )

  const navContent = (
    <nav className="flex-1 p-3 space-y-1">
      {navItem('/dashboard', t('menu.dashboard'), LayoutDashboard)}
      {navItem('/orders', t('menu.orders'), Package)}
      {isAdmin && navItem('/orders/new', t('menu.orders_new'), PlusCircle)}
      {navItem('/deposits', t('menu.deposits'), Wallet)}
    </nav>
  )

  const avatarLetter = me?.username?.[0]?.toUpperCase() ?? '?'

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="h-14 border-b bg-background flex items-center px-3 md:px-4 gap-2 sticky top-0 z-30">
        <Button
          variant="ghost"
          size="icon"
          className="md:hidden"
          onClick={() => setDrawerOpen(true)}
          aria-label={t('aria.menu_open')}
        >
          <Menu className="h-5 w-5" />
        </Button>
        <div className="font-semibold text-lg">Hauly Admin</div>
        <div className="flex-1" />
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="rounded-full bg-muted text-sm font-medium"
              aria-label={t('aria.account_menu')}
            >
              {avatarLetter}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            {me?.username && (
              <>
                <DropdownMenuLabel className="truncate">
                  {me.displayName ? `${me.displayName} (${me.username})` : me.username}
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
              </>
            )}
            <DropdownMenuSub>
              <DropdownMenuSubTrigger>
                <Globe className="h-4 w-4 mr-2" />
                {t('menu.language')}
              </DropdownMenuSubTrigger>
              <DropdownMenuSubContent>
                <DropdownMenuRadioGroup
                  value={i18n.resolvedLanguage}
                  onValueChange={changeLanguage}
                >
                  {LANGUAGES.map(({ code, key }) => (
                    <DropdownMenuRadioItem key={code} value={code}>
                      {t(key)}
                    </DropdownMenuRadioItem>
                  ))}
                </DropdownMenuRadioGroup>
              </DropdownMenuSubContent>
            </DropdownMenuSub>
            <DropdownMenuItem onSelect={() => setPwOpen(true)}>
              <KeyRound className="h-4 w-4 mr-2" />
              {t('menu.password_change')}
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onSelect={() => logoutMutation.mutate()}
              disabled={logoutMutation.isPending}
            >
              <LogOut className="h-4 w-4 mr-2" />
              {t('btn.logout')}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </header>

      <div className="flex-1 flex overflow-hidden">
        {/* Desktop sidebar — md+ only */}
        <aside className="hidden md:flex md:w-60 border-r bg-background flex-col">
          {navContent}
        </aside>

        {/* Mobile drawer + backdrop — only when open */}
        {drawerOpen && (
          <div className="md:hidden fixed inset-0 z-40 flex">
            <div
              className="absolute inset-0 bg-black/50"
              onClick={() => setDrawerOpen(false)}
              aria-label={t('aria.menu_close')}
            />
            <aside className="relative w-64 max-w-[80%] bg-background border-r flex flex-col shadow-xl">
              <div className="h-14 border-b flex items-center px-4 font-semibold">
                {t('menu.title')}
              </div>
              {navContent}
            </aside>
          </div>
        )}

        <main className="flex-1 bg-muted/20 overflow-auto">
          <Outlet />
        </main>
      </div>

      <ChangePasswordModal open={pwOpen} onClose={() => setPwOpen(false)} />
    </div>
  )
}
