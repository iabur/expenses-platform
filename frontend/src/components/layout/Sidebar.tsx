import { Link, useLocation } from 'react-router-dom';
import {
  Home,
  Users,
  Receipt,
  CreditCard,
  X
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/stores/auth';

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: Home },
  { name: 'Groups', href: '/groups', icon: Users },
  { name: 'Expenses', href: '/expenses', icon: Receipt },
  { name: 'Settlements', href: '/settlements', icon: CreditCard },
];

// Account navigation removed (handled via avatar dropdown)

interface SidebarProps {
  mobile?: boolean;
  onClose?: () => void;
}

export function Sidebar({ mobile = false, onClose }: SidebarProps) {
  const location = useLocation();
  useAuthStore(); // still initialize store in case it sets up listeners

  return (
    <div className={cn(
      "w-64 bg-white border-r border-gray-200 flex flex-col h-full",
      mobile && "shadow-lg"
    )}>
      {/* Logo */}
      <div className="p-6 border-b border-gray-200 flex items-center justify-between">
        <Link to="/dashboard" className="flex items-center space-x-2" onClick={onClose}>
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
            <Receipt className="w-5 h-5 text-white" />
          </div>
          <span className="text-xl font-bold text-gray-900">Expenses</span>
        </Link>
        {mobile && (
          <button
            aria-label="Close navigation"
            onClick={onClose}
            className="ml-2 p-2 rounded-md text-gray-500 hover:text-gray-700 hover:bg-gray-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
          >
            <X className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* Main Navigation */}
      <nav className="flex-1 p-4 pb-2 space-y-2">
        <p className="px-3 text-[11px] font-medium uppercase tracking-wide text-gray-400">Main</p>
        {navigation.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.href;
          
          return (
            <Link
              key={item.name}
              to={item.href}
              className={cn(
                "flex items-center space-x-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors",
                isActive
                  ? "bg-blue-600 text-white"
                  : "text-gray-600 hover:text-gray-900 hover:bg-gray-100"
              )}
              onClick={onClose}
            >
              <Icon className="w-5 h-5" />
              <span>{item.name}</span>
            </Link>
          );
        })}
      </nav>

      {/* Bottom Navigation */}
      {/* Account actions removed from mobile sidebar to avoid duplication. */}
    </div>
  );
}
