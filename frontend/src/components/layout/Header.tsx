import { Bell } from 'lucide-react'
import UserMenu from './UserMenu'

/**
 * Header Component
 * Top navigation bar with notifications and user menu
 */
export function Header() {
  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4">
      <div className="flex items-center justify-between">
        {/* Left side - could add breadcrumbs or search */}
        <div className="flex-1">
          {/* Placeholder for future breadcrumbs or global search */}
        </div>

        {/* Right side - notifications and user menu */}
        <div className="flex items-center gap-4">
          {/* Notifications - placeholder for future */}
          <button
            className="relative p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition"
            aria-label="Notifications"
          >
            <Bell className="w-5 h-5" />
            {/* Badge for unread notifications */}
            <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
          </button>

          {/* User Menu */}
          <UserMenu />
        </div>
      </div>
    </header>
  )
}

export default Header

