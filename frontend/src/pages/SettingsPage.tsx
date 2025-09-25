import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export default function SettingsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Settings</h1>
        <p className="text-gray-500 mt-1">
          Configure your application preferences
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Coming Soon</CardTitle>
          <CardDescription>
            Settings page is under development
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p>This page will include:</p>
          <ul className="list-disc list-inside mt-2 space-y-1">
            <li>General preferences</li>
            <li>Notification settings</li>
            <li>Privacy options</li>
            <li>Data export/import</li>
            <li>Account management</li>
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
