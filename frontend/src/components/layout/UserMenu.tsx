import * as Dropdown from '@radix-ui/react-dropdown-menu';
import { LogOut, Settings, User as UserIcon } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { useAuthStore } from '@/stores/auth';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/lib/utils';

export function UserMenu() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const initial = user?.name?.charAt(0).toUpperCase() || 'U';

  const go = (path: string) => {
    navigate(path);
  };

  return (
    <Dropdown.Root>
      <Dropdown.Trigger
        className={cn(
          'flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-gray-100',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500'
        )}
        aria-label="Open user menu"
      >
        <Avatar className="w-8 h-8">
          <AvatarImage src="" alt={user?.name || 'User'} />
          <AvatarFallback className="bg-blue-600 text-white text-sm font-medium">
            {initial}
          </AvatarFallback>
        </Avatar>
        <span className="hidden md:flex flex-col text-left leading-tight">
            <span className="text-sm font-medium text-gray-900">{user?.name || 'User'}</span>
            <span className="text-[11px] text-gray-500 max-w-[140px] truncate">{user?.email}</span>
        </span>
      </Dropdown.Trigger>
      <Dropdown.Portal>
        <Dropdown.Content
          side="bottom"
          align="end"
          className="z-50 min-w-[200px] rounded-xl border border-gray-200 bg-white shadow-md p-1 will-change-[opacity,transform] animate-fade-in"
        >
          <div className="py-1">
          <Dropdown.Item
            onSelect={() => go('/profile')}
            className="flex cursor-pointer items-center gap-2 rounded-md px-3 py-1.5 text-sm text-gray-700 outline-none hover:bg-gray-100 focus:bg-gray-100"
          >
            <UserIcon className="w-4 h-4 text-gray-500" />
            Profile
          </Dropdown.Item>
          <Dropdown.Item
            onSelect={() => go('/settings')}
            className="flex cursor-pointer items-center gap-2 rounded-md px-3 py-1.5 text-sm text-gray-700 outline-none hover:bg-gray-100 focus:bg-gray-100"
          >
            <Settings className="w-4 h-4 text-gray-500" />
            Settings
          </Dropdown.Item>
          </div>
          <Dropdown.Separator className="h-px bg-gray-200 my-1" />
          <div className="p-1 pt-0">
          <Dropdown.Item
            onSelect={logout}
            className="flex cursor-pointer items-center gap-2 rounded-md px-3 py-1.5 text-sm text-red-600 outline-none hover:bg-red-50 focus:bg-red-50"
          >
            <LogOut className="w-4 h-4" />
            Logout
          </Dropdown.Item>
          </div>
        </Dropdown.Content>
      </Dropdown.Portal>
    </Dropdown.Root>
  );
}

export default UserMenu;