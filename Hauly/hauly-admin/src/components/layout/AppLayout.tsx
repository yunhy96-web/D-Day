import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LayoutDashboard, Package, PlusCircle, KeyRound } from 'lucide-react'
import { useLogout } from '@/features/auth/hooks'
import { ChangePasswordModal } from '@/features/auth/ChangePasswordModal'
import { Button } from '@/components/ui/button'
import { LanguageSwitcher } from '@/components/LanguageSwitcher'
import { cn } from '@/lib/utils'

export default function AppLayout() {
  const { t } = useTranslation()
  const logoutMutation = useLogout()
  const [pwOpen, setPwOpen] = useState(false)

  const navItem = (to: string, label: string, Icon: typeof LayoutDashboard) => (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        cn(
          'flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors',
          isActive
            ? 'bg-primary text-primary-foreground'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground'
        )
      }
    >
      <Icon className="h-4 w-4" />
      {label}
    </NavLink>
  )

  return (
    <div className="min-h-screen flex">
      {/* Sidebar */}
      <aside className="w-60 border-r bg-background flex flex-col">
        <div className="h-14 border-b flex items-center px-4 font-semibold text-lg">
          Hauly Admin
        </div>
        <nav className="flex-1 p-3 space-y-1">
          {navItem('/dashboard', t('menu.dashboard'), LayoutDashboard)}
          {navItem('/orders', t('menu.orders'), Package)}
          {navItem('/orders/new', t('menu.orders_new'), PlusCircle)}
        </nav>
        <div className="p-3 border-t space-y-2">
          <LanguageSwitcher className="justify-center" />
          <Button
            variant="outline"
            size="sm"
            className="w-full"
            onClick={() => setPwOpen(true)}
          >
            <KeyRound className="h-4 w-4 mr-1" />
            비밀번호 변경
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="w-full"
            onClick={() => logoutMutation.mutate()}
            disabled={logoutMutation.isPending}
          >
            {t('btn.logout')}
          </Button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 bg-muted/20 overflow-auto">
        <Outlet />
      </main>

      <ChangePasswordModal open={pwOpen} onClose={() => setPwOpen(false)} />
    </div>
  )
}
