import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'

/**
 * Main Layout Component
 * Provides the shell for authenticated pages with sidebar and header
 */
export function MainLayout() {
  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <Sidebar />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <Header />

        {/* Page Content */}
        <main className="flex-1 overflow-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default MainLayout

