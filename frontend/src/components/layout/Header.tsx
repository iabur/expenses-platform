import { Bell, Search, Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { UserMenu } from '@/components/layout/UserMenu';

interface HeaderProps {
  onOpenMobileNav?: () => void;
}

export function Header({ onOpenMobileNav }: HeaderProps) {
  // user state accessed inside UserMenu; no need to pull here

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4">
      <div className="flex items-center justify-between">
        {/* Search */}
        <div className="flex-1 max-w-lg">
          <div className="relative flex-1">
            <button
              type="button"
              aria-label="Open navigation menu"
              onClick={onOpenMobileNav}
              className="md:hidden p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
            >
              <Menu className="w-5 h-5" />
            </button>
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
            <Input
              placeholder="Search groups, expenses..."
              className="pl-10 border-gray-300 focus:border-blue-500 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* Right side */}
        <div className="flex items-center space-x-2 sm:space-x-4">
          {/* Notifications */}
          <Button variant="ghost" size="icon" className="text-gray-600 hover:text-gray-900 hover:bg-gray-100">
            <Bell className="w-5 h-5" />
          </Button>

          <UserMenu />
        </div>
      </div>
    </header>
  );
}
