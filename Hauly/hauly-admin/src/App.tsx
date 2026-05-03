import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/lib/query'

import AppLayout from '@/components/layout/AppLayout'
import ProtectedRoute from '@/components/guards/ProtectedRoute'
import ErrorBoundary from '@/components/ErrorBoundary'
import LoginPage from '@/features/auth/LoginPage'
import DashboardPage from '@/features/dashboard/DashboardPage'
import OrderListPage from '@/features/orders/OrderListPage'
import OrderCreatePage from '@/features/orders/OrderCreatePage'
import OrderDetailPage from '@/features/orders/OrderDetailPage'
import NotFoundPage from '@/features/NotFoundPage'

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <Routes>
              {/* Public */}
              <Route path="/login" element={<LoginPage />} />

              {/* Root redirect */}
              <Route path="/" element={<Navigate to="/dashboard" replace />} />

              {/* Guard: checks auth, renders <Outlet /> on success */}
              <Route element={<ProtectedRoute />}>
                {/* Layout: header + <Outlet /> */}
                <Route element={<AppLayout />}>
                  <Route path="/dashboard" element={<DashboardPage />} />
                  <Route path="/orders" element={<OrderListPage />} />
                  <Route path="/orders/new" element={<OrderCreatePage />} />
                  <Route path="/orders/:id" element={<OrderDetailPage />} />
                </Route>
              </Route>

              {/* 404 */}
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </BrowserRouter>
        </QueryClientProvider>
    </ErrorBoundary>
  )
}
