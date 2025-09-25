import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { Toaster } from '@/components/ui/toaster';
import { useAuthStore } from '@/stores/auth';
import { useEffect } from 'react';

// Import pages
import LoginPage from '@/pages/LoginPage';
import DashboardPage from '@/pages/DashboardPage';
import GroupsPage from '@/pages/GroupsPage';
import GroupDetailPage from '@/pages/GroupDetailPage';
import ExpensesPage from '@/pages/ExpensesPage';
import ExpenseDetailPage from '@/pages/ExpenseDetailPage';
import ProfilePage from '@/pages/ProfilePage';
import SettingsPage from '@/pages/SettingsPage';

// Import layouts
import MainLayout from '@/components/layout/MainLayout';
import AuthLayout from '@/components/layout/AuthLayout';

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: (failureCount, error: any) => {
        if (error?.response?.status === 401) {
          return false; // Don't retry on auth errors
        }
        return failureCount < 3;
      },
    },
  },
});

function App() {
  const { isAuthenticated, getCurrentUser } = useAuthStore();

  useEffect(() => {
    // Try to get current user on app load
    if (isAuthenticated) {
      getCurrentUser();
    }
  }, [isAuthenticated, getCurrentUser]);

  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <div className="min-h-screen bg-background">
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={
              isAuthenticated ? <Navigate to="/dashboard" replace /> : (
                <AuthLayout>
                  <LoginPage />
                </AuthLayout>
              )
            } />

            {/* Protected routes */}
            <Route path="/" element={
              <MainLayout>
                <Navigate to="/dashboard" replace />
              </MainLayout>
            } />
            
            <Route path="/dashboard" element={
              isAuthenticated ? (
                <MainLayout>
                  <DashboardPage />
                </MainLayout>
              ) : <Navigate to="/login" replace />
            } />

            <Route path="/groups" element={
              isAuthenticated ? (
                <MainLayout>
                  <GroupsPage />
                </MainLayout>
              ) : <Navigate to="/login" replace />
            } />

            <Route path="/groups/:groupId" element={
              isAuthenticated ? (
                <MainLayout>
                  <GroupDetailPage />
                </MainLayout>
              ) : <Navigate to="/login" replace />
            } />

            <Route path="/expenses" element={
              isAuthenticated ? (
                <MainLayout>
                  <ExpensesPage />
                </MainLayout>
              ) : <Navigate to="/login" replace />
            } />

            <Route path="/expenses/:expenseId" element={
              isAuthenticated ? (
                <MainLayout>
                  <ExpenseDetailPage />
                </MainLayout>
              ) : <Navigate to="/login" replace />
            } />

            <Route path="/profile" element={
              isAuthenticated ? (
                <MainLayout>
                  <ProfilePage />
                </MainLayout>
              ) : <Navigate to="/login" replace />
            } />

            <Route path="/settings" element={
              isAuthenticated ? (
                <MainLayout>
                  <SettingsPage />
                </MainLayout>
              ) : <Navigate to="/login" replace />
            } />

            {/* Catch all route */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </div>
        
        <Toaster />
        <ReactQueryDevtools initialIsOpen={false} />
      </Router>
    </QueryClientProvider>
  );
}

export default App;