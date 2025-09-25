import { useParams } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export default function ExpenseDetailPage() {
  const { expenseId } = useParams();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Expense Details</h1>
        <p className="text-gray-500 mt-1">
          Expense ID: {expenseId}
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Coming Soon</CardTitle>
          <CardDescription>
            Expense detail page is under development
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p>This page will show detailed information about the selected expense, including:</p>
          <ul className="list-disc list-inside mt-2 space-y-1">
            <li>Expense details and participants</li>
            <li>Split calculations</li>
            <li>Payment status</li>
            <li>Attachments and receipts</li>
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
