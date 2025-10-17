import { NavLink } from 'react-router-dom'
import { 
  LayoutDashboard, 
  Users, 
  Receipt, 
  Scale, 
  HandCoins,
  Menu,
  X
} from 'lucide-react'
import { useState } from 'react'

/**
 * Sidebar Navigation Component
 * Main navigation menu with icons and labels
 */
export function Sidebar() {
  const [isMobileOpen, setIsMobileOpen] = useState(false)

  const navItems = [
    { path: '/', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/groups', label: 'Groups', icon: Users },
    { path: '/expenses', label: 'Expenses', icon: Receipt },
    { path: '/balances', label: 'Balances', icon: Scale },
    { path: '/settlements', label: 'Settlements', icon: HandCoins },
  ]

  const NavItems = () => (
    <>
      {navItems.map((item) => {
        const Icon = item.icon
        return (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            onClick={() => setIsMobileOpen(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-700 hover:bg-gray-100'
              }`
            }
          >
            <Icon className="w-5 h-5" />
            <span className="font-medium">{item.label}</span>
          </NavLink>
        )
      })}
    </>
  )

  return (
    <>
      {/* Mobile Menu Button */}
      <button
        onClick={() => setIsMobileOpen(!isMobileOpen)}
        className="lg:hidden fixed top-4 left-4 z-50 p-2 bg-white rounded-lg shadow-lg"
      >
        {isMobileOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
      </button>

      {/* Mobile Overlay */}
      {isMobileOpen && (
        <div
          className="lg:hidden fixed inset-0 bg-black bg-opacity-50 z-30"
          onClick={() => setIsMobileOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`
          w-64 bg-white border-r border-gray-200 flex flex-col
          fixed lg:static inset-y-0 left-0 z-40
          transform transition-transform duration-200 ease-in-out
          ${isMobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
        `}
      >
        {/* Logo */}
        <div className="p-6 border-b border-gray-200">
          <h1 className="text-xl font-bold text-gray-900">
            💰 Expenses Platform
          </h1>
          <p className="text-xs text-gray-500 mt-1">Manage & Split Expenses</p>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-4 space-y-1">
          <NavItems />
        </nav>

        {/* Footer */}
        <div className="p-4 border-t border-gray-200">
          <p className="text-xs text-gray-500 text-center">
            v1.0.0 • {import.meta.env.VITE_APP_ENVIRONMENT || 'dev'}
          </p>
        </div>
      </aside>
    </>
  )
}

export default Sidebar

