import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Plus, Users, Calendar } from 'lucide-react';
import { Link } from 'react-router-dom';
import apiClient from '@/services/api';
import { formatDate } from '@/lib/utils';

export default function GroupsPage() {
  const { data: groupsData, isLoading } = useQuery({
    queryKey: ['user-groups'],
    queryFn: () => apiClient.getUserGroups(),
  });

  const groups = groupsData?.content || [];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Groups</h1>
          <p className="text-gray-500 mt-1">
            Manage your expense sharing groups
          </p>
        </div>
        <Button asChild>
          <Link to="/groups/new">
            <Plus className="w-4 h-4 mr-2" />
            Create Group
          </Link>
        </Button>
      </div>

      {groups.length === 0 ? (
        <Card>
          <CardContent className="text-center py-12">
            <Users className="h-16 w-16 text-gray-500 mx-auto mb-4" />
            <h3 className="text-lg font-semibold mb-2">No groups yet</h3>
            <p className="text-gray-500 mb-6">
              Create your first group to start sharing expenses with friends, family, or colleagues.
            </p>
            <Button asChild>
              <Link to="/groups/new">
                <Plus className="w-4 h-4 mr-2" />
                Create your first group
              </Link>
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {groups.map((group) => (
            <Card key={group.id} className="hover:shadow-md transition-shadow">
              <CardHeader>
                <CardTitle className="flex items-center justify-between">
                  <Link 
                    to={`/groups/${group.id}`}
                    className="hover:text-primary"
                  >
                    {group.name}
                  </Link>
                  <span className="text-sm font-normal text-gray-500">
                    {group.currency}
                  </span>
                </CardTitle>
                <CardDescription>
                  {group.description || 'No description'}
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div className="flex items-center text-sm text-gray-500">
                    <Users className="w-4 h-4 mr-2" />
                    {group.memberCount} members
                  </div>
                  <div className="flex items-center text-sm text-gray-500">
                    <Calendar className="w-4 h-4 mr-2" />
                    Created {formatDate(group.createdAt)}
                  </div>
                  <div className="flex space-x-2 pt-2">
                    <Button variant="outline" size="sm" asChild className="flex-1">
                      <Link to={`/groups/${group.id}`}>View Details</Link>
                    </Button>
                    <Button variant="ghost" size="sm" asChild>
                      <Link to={`/groups/${group.id}/expenses`}>Expenses</Link>
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
