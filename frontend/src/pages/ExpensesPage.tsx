import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Plus, Receipt } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function ExpensesPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Expenses</h1>
          <p className="text-gray-500 mt-1">
            Track and manage your shared expenses
          </p>
        </div>
        <Button asChild>
          <Link to="/expenses/new">
            <Plus className="w-4 h-4 mr-2" />
            Add Expense
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Coming Soon</CardTitle>
          <CardDescription>
            Expenses page is under development
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="text-center py-12">
            <Receipt className="h-16 w-16 text-gray-500 mx-auto mb-4" />
            <h3 className="text-lg font-semibold mb-2">Expense Management</h3>
            <p className="text-gray-500 mb-6">
              This page will allow you to view, create, and manage expenses across all your groups.
            </p>
            <Button asChild>
              <Link to="/expenses/new">
                <Plus className="w-4 h-4 mr-2" />
                Add your first expense
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
