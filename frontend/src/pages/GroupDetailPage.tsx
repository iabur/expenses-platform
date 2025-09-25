import { useParams } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export default function GroupDetailPage() {
  const { groupId } = useParams();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Group Details</h1>
        <p className="text-gray-500 mt-1">
          Group ID: {groupId}
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Coming Soon</CardTitle>
          <CardDescription>
            Group detail page is under development
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p>This page will show detailed information about the selected group, including:</p>
          <ul className="list-disc list-inside mt-2 space-y-1">
            <li>Group members and their roles</li>
            <li>Recent expenses</li>
            <li>Balance summaries</li>
            <li>Group settings</li>
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
