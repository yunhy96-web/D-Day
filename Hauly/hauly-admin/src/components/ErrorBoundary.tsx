import { Component, type ErrorInfo, type ReactNode } from 'react'
import { withTranslation, type WithTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

interface Props extends WithTranslation {
  children: ReactNode
}

interface State {
  hasError: boolean
}

class ErrorBoundaryBase extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, info)
  }

  render() {
    const { t, children } = this.props
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center">
          <Card className="max-w-sm w-full">
            <CardHeader>
              <CardTitle>{t('msg.error.unexpected')}</CardTitle>
            </CardHeader>
            <CardContent>
              <Button onClick={() => window.location.reload()} className="w-full">
                {t('btn.reload')}
              </Button>
            </CardContent>
          </Card>
        </div>
      )
    }
    return children
  }
}

export const ErrorBoundary = withTranslation()(ErrorBoundaryBase)
export default ErrorBoundary
