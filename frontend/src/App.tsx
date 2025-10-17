import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ErrorBoundary from './components/ErrorBoundary'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import MainLayout from './components/layout/MainLayout'
import './App.css'

// Placeholder pages - will be implemented in later days
const DashboardPage = () => (
  <div className="p-6">
    <h1 className="text-2xl font-bold mb-4">Dashboard</h1>
    <p className="text-gray-600">Dashboard page coming soon...</p>
  </div>
)

const GroupsPage = () => (
  <div className="p-6">
    <h1 className="text-2xl font-bold mb-4">Groups</h1>
    <p className="text-gray-600">Groups page coming soon (Day 6-10)...</p>
  </div>
)

const ExpensesPage = () => (
  <div className="p-6">
    <h1 className="text-2xl font-bold mb-4">Expenses</h1>
    <p className="text-gray-600">Expenses page coming soon (Day 11-15)...</p>
  </div>
)

const BalancesPage = () => (
  <div className="p-6">
    <h1 className="text-2xl font-bold mb-4">Balances</h1>
    <p className="text-gray-600">Balances page coming soon (Day 16-20)...</p>
  </div>
)

const SettlementsPage = () => (
  <div className="p-6">
    <h1 className="text-2xl font-bold mb-4">Settlements</h1>
    <p className="text-gray-600">Settlements page coming soon (Day 21-25)...</p>
  </div>
)

const ProfilePage = () => (
  <div className="p-6">
    <h1 className="text-2xl font-bold mb-4">Profile</h1>
    <p className="text-gray-600">Profile page coming soon (Day 4)...</p>
  </div>
)

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />

          {/* Protected routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <MainLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="groups" element={<GroupsPage />} />
            <Route path="expenses" element={<ExpensesPage />} />
            <Route path="balances" element={<BalancesPage />} />
            <Route path="settlements" element={<SettlementsPage />} />
            <Route path="profile" element={<ProfilePage />} />
          </Route>

          {/* Catch all - redirect to dashboard */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  )
}

export default App
