import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { useState, useEffect } from 'react';

// Basic lock body scroll when mobile nav open
function useBodyScrollLock(locked: boolean) {
  useEffect(() => {
    if (locked) {
      const original = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => { document.body.style.overflow = original; };
    }
  }, [locked]);
}

interface MainLayoutProps {
  children?: React.ReactNode;
}

function MainLayout({ children }: MainLayoutProps) {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  useBodyScrollLock(mobileNavOpen);

  return (
    <div className="min-h-dvh bg-background flex overscroll-none">
      {/* Desktop sidebar */}
      <div className="hidden md:block h-full">
        <Sidebar />
      </div>

      {/* Mobile overlay */}
      {mobileNavOpen && (
        <div className="md:hidden fixed inset-0 z-40 flex">
          <div className="relative z-50 w-64 h-full">
            <Sidebar mobile onClose={() => setMobileNavOpen(false)} />
          </div>
          <button
            aria-label="Close navigation overlay"
            onClick={() => setMobileNavOpen(false)}
            className="flex-1 bg-black/40 backdrop-blur-sm"
          />
        </div>
      )}

      <div className="flex-1 flex flex-col overflow-hidden">
        <Header onOpenMobileNav={() => setMobileNavOpen(true)} />
        <main className="flex-1 p-4 sm:p-6 overflow-y-auto overflow-x-hidden">
          {children || <Outlet />}
        </main>
      </div>
    </div>
  );
}

export default MainLayout;
