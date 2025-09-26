import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Plus, Users, Receipt, CreditCard, TrendingUp } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '@/stores/auth';
import apiClient from '@/services/api';
import { formatCurrency, formatDate } from '@/lib/utils';

export default function DashboardPage() {
  const { user } = useAuthStore();

  // Fetch user's groups
  const { data: groupsData } = useQuery({
    queryKey: ['user-groups'],
    queryFn: () => apiClient.getUserGroups(0, 5),
  });

  // Fetch user's recent expenses
  const { data: expensesData } = useQuery({
    queryKey: ['user-expenses'],
    queryFn: () => apiClient.getMyExpenses(0, 5),
  });

  // Fetch user's participated expenses
  const { data: participatedData } = useQuery({
    queryKey: ['participated-expenses'],
    queryFn: () => apiClient.getParticipatedExpenses(0, 5),
  });

  const groups = groupsData?.content || [];
  const expenses = expensesData?.content || [];
  // const participatedExpenses = participatedData?.content || [];

  return (
    <div className="space-y-6">
      {/* Development Mode Banner */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex items-center">
          <div className="flex-shrink-0">
            <div className="w-2 h-2 bg-blue-400 rounded-full animate-pulse"></div>
          </div>
          <div className="ml-3">
            <p className="text-sm text-blue-800">
              <span className="font-medium">Development Mode:</span> Using mock data for demonstration
            </p>
          </div>
        </div>
      </div>

      {/* Welcome Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">
            Welcome back, {user?.name || 'User'}!
          </h1>
          <p className="text-gray-600 mt-1">
            Here's what's happening with your expenses today.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button asChild className="bg-blue-600 hover:bg-blue-700 text-white">
            <Link to="/groups">
              <Plus className="w-4 h-4 mr-2" />
              New Group
            </Link>
          </Button>
          <Button variant="outline" asChild className="border-gray-300 text-gray-700 hover:bg-gray-50">
            <Link to="/expenses">
              <Receipt className="w-4 h-4 mr-2" />
              Add Expense
            </Link>
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card className="bg-white border border-gray-200">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-900">Total Groups</CardTitle>
            <Users className="h-4 w-4 text-gray-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-gray-900">{groupsData?.totalElements || 0}</div>
            <p className="text-xs text-gray-500">
              Active expense groups
            </p>
          </CardContent>
        </Card>

        <Card className="bg-white border border-gray-200">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-900">My Expenses</CardTitle>
            <Receipt className="h-4 w-4 text-gray-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-gray-900">{expensesData?.totalElements || 0}</div>
            <p className="text-xs text-gray-500">
              Expenses you created
            </p>
          </CardContent>
        </Card>

        <Card className="bg-white border border-gray-200">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-900">Participated</CardTitle>
            <TrendingUp className="h-4 w-4 text-gray-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-gray-900">{participatedData?.totalElements || 0}</div>
            <p className="text-xs text-gray-500">
              Expenses you're involved in
            </p>
          </CardContent>
        </Card>

        <Card className="bg-white border border-gray-200">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-900">Pending Settlements</CardTitle>
            <CreditCard className="h-4 w-4 text-gray-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-gray-900">0</div>
            <p className="text-xs text-gray-500">
              Awaiting payment
            </p>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Groups */}
        <Card className="bg-white border border-gray-200">
          <CardHeader>
            <CardTitle className="text-gray-900">Recent Groups</CardTitle>
            <CardDescription className="text-gray-600">
              Your most recently active groups
            </CardDescription>
          </CardHeader>
          <CardContent>
            {groups.length === 0 ? (
              <div className="text-center py-6">
                <Users className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-500">No groups yet</p>
                <Button asChild className="mt-4 bg-blue-600 hover:bg-blue-700 text-white">
                  <Link to="/groups">
                    <Plus className="w-4 h-4 mr-2" />
                    Create your first group
                  </Link>
                </Button>
              </div>
            ) : (
              <div className="space-y-4">
                {groups.map((group) => (
                  <div key={group.id} className="flex items-center justify-between p-3 border border-gray-200 rounded-lg bg-gray-50">
                    <div>
                      <Link 
                        to={`/groups/${group.id}`}
                        className="font-medium text-gray-900 hover:text-blue-600"
                      >
                        {group.name}
                      </Link>
                      <p className="text-sm text-gray-500">
                        {group.memberCount} members • {group.currency}
                      </p>
                    </div>
                    <Button variant="ghost" size="sm" asChild>
                      <Link to={`/groups/${group.id}`}>View</Link>
                    </Button>
                  </div>
                ))}
                <Button variant="outline" className="w-full" asChild>
                  <Link to="/groups">View all groups</Link>
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Recent Expenses */}
        <Card>
          <CardHeader>
            <CardTitle>Recent Expenses</CardTitle>
            <CardDescription>
              Your latest expense activities
            </CardDescription>
          </CardHeader>
          <CardContent>
            {expenses.length === 0 ? (
              <div className="text-center py-6">
                <Receipt className="h-12 w-12 text-gray-500 mx-auto mb-4" />
                <p className="text-gray-500">No expenses yet</p>
                <Button asChild className="mt-4">
                  <Link to="/expenses">
                    <Plus className="w-4 h-4 mr-2" />
                    Add your first expense
                  </Link>
                </Button>
              </div>
            ) : (
              <div className="space-y-4">
                {expenses.map((expense) => (
                  <div key={expense.id} className="flex items-center justify-between p-3 border rounded-lg">
                    <div>
                      <Link 
                        to={`/expenses/${expense.id}`}
                        className="font-medium hover:text-primary"
                      >
                        {expense.note || 'Expense'}
                      </Link>
                      <p className="text-sm text-gray-500">
                        {formatDate(expense.occurredAt)} • {expense.category}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="font-medium">
                        {formatCurrency(expense.amount, expense.currency)}
                      </p>
                      <p className="text-sm text-gray-500">
                        {expense.participantCount} people
                      </p>
                    </div>
                  </div>
                ))}
                <Button variant="outline" className="w-full" asChild>
                  <Link to="/expenses">View all expenses</Link>
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
