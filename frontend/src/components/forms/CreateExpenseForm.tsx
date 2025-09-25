import { useState } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { useToast } from '@/hooks/use-toast';
import apiClient from '@/services/api';
import { Loader2, X, Users } from 'lucide-react';
import { formatCurrency } from '@/lib/utils';

const participantSchema = z.object({
  userId: z.string().min(1, 'User is required'),
  ruleType: z.enum(['EQUAL', 'PERCENTAGE', 'EXACT', 'SHARES']),
  ruleValue: z.number().min(0, 'Rule value must be non-negative'),
});

const createExpenseSchema = z.object({
  groupId: z.string().min(1, 'Group is required'),
  currency: z.string().min(3, 'Currency is required').max(3, 'Currency must be 3 characters'),
  amount: z.number().min(0.01, 'Amount must be positive'),
  occurredAt: z.string().min(1, 'Date is required'),
  note: z.string().max(1000, 'Note must be less than 1000 characters').optional(),
  category: z.string().min(1, 'Category is required'),
  participants: z.array(participantSchema).min(1, 'At least one participant is required'),
  paidBy: z.string().optional(),
});

type CreateExpenseForm = z.infer<typeof createExpenseSchema>;

const EXPENSE_CATEGORIES = [
  'Food & Dining',
  'Transportation',
  'Entertainment',
  'Shopping',
  'Travel',
  'Utilities',
  'Healthcare',
  'Education',
  'Other',
];

const SPLIT_METHODS = [
  { value: 'EQUAL', label: 'Split equally' },
  { value: 'PERCENTAGE', label: 'Split by percentage' },
  { value: 'EXACT', label: 'Split by exact amount' },
  { value: 'SHARES', label: 'Split by shares' },
];

interface CreateExpenseFormProps {
  groupId?: string;
  onSuccess?: (expense: any) => void;
}

export function CreateExpenseForm({ groupId: propGroupId, onSuccess }: CreateExpenseFormProps) {
  const navigate = useNavigate();
  const { groupId: paramGroupId } = useParams();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const groupId = propGroupId || paramGroupId;

  const [selectedUsers, setSelectedUsers] = useState<Set<string>>(new Set());

  // Fetch user's groups (for future use)
  // const { data: groupsData } = useQuery({
  //   queryKey: ['user-groups'],
  //   queryFn: () => apiClient.getUserGroups(),
  // });

  // Fetch group members if groupId is provided
  const { data: groupData } = useQuery({
    queryKey: ['group', groupId],
    queryFn: () => apiClient.getGroup(groupId!),
    enabled: !!groupId,
  });

  // Fetch users for selection
  const { data: usersData } = useQuery({
    queryKey: ['users'],
    queryFn: () => apiClient.searchUsers(),
  });

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<CreateExpenseForm>({
    resolver: zodResolver(createExpenseSchema),
    defaultValues: {
      groupId: groupId || '',
      currency: groupData?.currency || 'USD',
      occurredAt: new Date().toISOString().split('T')[0],
      category: 'Food & Dining',
      participants: [],
      paidBy: '',
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'participants',
  });

  const watchedAmount = watch('amount');
  const watchedParticipants = watch('participants');

  const createExpenseMutation = useMutation({
    mutationFn: apiClient.createExpense,
    onSuccess: (expense) => {
      queryClient.invalidateQueries({ queryKey: ['user-expenses'] });
      queryClient.invalidateQueries({ queryKey: ['group', groupId] });
      toast({
        title: 'Expense created successfully!',
        description: `Expense has been added to the group.`,
      });
      
      if (onSuccess) {
        onSuccess(expense);
      } else {
        navigate(`/groups/${groupId || expense.groupId}`);
      }
    },
    onError: (error: any) => {
      toast({
        title: 'Failed to create expense',
        description: error.message || 'Please try again.',
        variant: 'destructive',
      });
    },
  });

  const onSubmit = (data: CreateExpenseForm) => {
    createExpenseMutation.mutate({
      ...data,
      paidBy: data.paidBy || undefined,
    });
  };

  const addParticipant = (userId: string) => {
    if (selectedUsers.has(userId)) return;
    
    setSelectedUsers(prev => new Set([...prev, userId]));
    append({
      userId,
      ruleType: 'EQUAL',
      ruleValue: 0,
    });
  };

  const removeParticipant = (index: number) => {
    const participant = watchedParticipants[index];
    if (participant) {
      setSelectedUsers(prev => {
        const newSet = new Set(prev);
        newSet.delete(participant.userId);
        return newSet;
      });
    }
    remove(index);
  };

  const availableUsers = usersData?.content?.filter(user => 
    !selectedUsers.has(user.id) && 
    (groupId ? groupData?.members?.some(member => member.userId === user.id) : true)
  ) || [];

  const totalAmount = watchedAmount || 0;
  const equalShare = fields.length > 0 ? totalAmount / fields.length : 0;

  return (
    <Card className="w-full max-w-4xl mx-auto">
      <CardHeader>
        <CardTitle>Create New Expense</CardTitle>
        <CardDescription>
          Add a new expense to track shared costs with your group.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Basic Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="amount">Amount *</Label>
              <Input
                id="amount"
                type="number"
                step="0.01"
                min="0.01"
                placeholder="0.00"
                {...register('amount', { valueAsNumber: true })}
                className={errors.amount ? 'border-destructive' : ''}
              />
              {errors.amount && (
                <p className="text-sm text-destructive">{errors.amount.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="currency">Currency *</Label>
              <Select
                value={watch('currency')}
                onValueChange={(value) => setValue('currency', value)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="USD">USD - US Dollar</SelectItem>
                  <SelectItem value="EUR">EUR - Euro</SelectItem>
                  <SelectItem value="GBP">GBP - British Pound</SelectItem>
                  <SelectItem value="JPY">JPY - Japanese Yen</SelectItem>
                </SelectContent>
              </Select>
              {errors.currency && (
                <p className="text-sm text-destructive">{errors.currency.message}</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="occurredAt">Date *</Label>
              <Input
                id="occurredAt"
                type="date"
                {...register('occurredAt')}
                className={errors.occurredAt ? 'border-destructive' : ''}
              />
              {errors.occurredAt && (
                <p className="text-sm text-destructive">{errors.occurredAt.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="category">Category *</Label>
              <Select
                value={watch('category')}
                onValueChange={(value) => setValue('category', value)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {EXPENSE_CATEGORIES.map((category) => (
                    <SelectItem key={category} value={category}>
                      {category}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.category && (
                <p className="text-sm text-destructive">{errors.category.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="note">Description</Label>
            <textarea
              id="note"
              placeholder="What was this expense for?"
              rows={3}
              className="flex min-h-[80px] w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm ring-offset-white placeholder:text-gray-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
              {...register('note')}
            />
            {errors.note && (
              <p className="text-sm text-destructive">{errors.note.message}</p>
            )}
          </div>

          <Separator />

          {/* Participants */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <Label className="text-base font-semibold">Participants</Label>
              {totalAmount > 0 && fields.length > 0 && (
                <Badge variant="secondary">
                  Equal share: {formatCurrency(equalShare, watch('currency'))}
                </Badge>
              )}
            </div>

            {/* Add Participants */}
            <div className="space-y-2">
              <Label>Add Participants</Label>
              <div className="flex flex-wrap gap-2">
                {availableUsers.map((user) => (
                  <Button
                    key={user.id}
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => addParticipant(user.id)}
                  >
                    <Users className="w-4 h-4 mr-2" />
                    {user.displayName || user.email}
                  </Button>
                ))}
              </div>
            </div>

            {/* Selected Participants */}
            <div className="space-y-3">
              {fields.map((field, index) => {
                const participant = watchedParticipants[index];
                const user = usersData?.content?.find(u => u.id === participant?.userId);
                
                return (
                  <div key={field.id} className="flex items-center space-x-3 p-3 border rounded-lg">
                    <div className="flex-1">
                      <p className="font-medium">{user?.displayName || user?.email}</p>
                      <div className="flex items-center space-x-2 mt-1">
                        <Select
                          value={participant?.ruleType}
                          onValueChange={(value) => setValue(`participants.${index}.ruleType`, value as any)}
                        >
                          <SelectTrigger className="w-32">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {SPLIT_METHODS.map((method) => (
                              <SelectItem key={method.value} value={method.value}>
                                {method.label}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        
                        {participant?.ruleType !== 'EQUAL' && (
                          <Input
                            type="number"
                            step="0.01"
                            min="0"
                            placeholder="Value"
                            className="w-24"
                            {...register(`participants.${index}.ruleValue`, { valueAsNumber: true })}
                          />
                        )}
                      </div>
                    </div>
                    
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => removeParticipant(index)}
                    >
                      <X className="w-4 h-4" />
                    </Button>
                  </div>
                );
              })}
            </div>

            {errors.participants && (
              <p className="text-sm text-destructive">{errors.participants.message}</p>
            )}
          </div>

          <Separator />

          {/* Paid By */}
          <div className="space-y-2">
            <Label htmlFor="paidBy">Paid by (optional)</Label>
            <Select
              value={watch('paidBy') || ''}
              onValueChange={(value) => setValue('paidBy', value)}
            >
              <SelectTrigger>
                <SelectValue placeholder="Who paid for this expense?" />
              </SelectTrigger>
              <SelectContent>
                {watchedParticipants.map((participant) => {
                  const user = usersData?.content?.find(u => u.id === participant.userId);
                  return (
                    <SelectItem key={participant.userId} value={participant.userId}>
                      {user?.displayName || user?.email}
                    </SelectItem>
                  );
                })}
              </SelectContent>
            </Select>
          </div>

          <div className="flex justify-end space-x-4 pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate(-1)}
              disabled={createExpenseMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={createExpenseMutation.isPending || fields.length === 0}
            >
              {createExpenseMutation.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Create Expense
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
