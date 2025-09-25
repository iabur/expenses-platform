import { ReactNode } from 'react';

interface AuthLayoutProps {
  children: ReactNode;
}

function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary/5 to-secondary/20">
      <div className="w-full max-w-md p-4 sm:p-6">
        <div className="text-center mb-6 sm:mb-8">
          <h1 className="text-2xl sm:text-3xl font-bold text-primary">Expenses Platform</h1>
          <p className="text-gray-500 mt-2">
            The next-generation expense sharing platform
          </p>
        </div>
        {children}
      </div>
    </div>
  );
}

export default AuthLayout;
