import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/index.css'
import './lib/i18n'
import { installBackendI18nBridge } from './lib/i18n/api'

// Subscribe i18next to the backend i18n_message endpoint so DB rows override
// the bundled JSON for any language as soon as the user switches into it.
installBackendI18nBridge()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
