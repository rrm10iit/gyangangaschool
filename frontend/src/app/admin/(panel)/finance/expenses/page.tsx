'use client';

import { money, shortDate } from '@/lib/api';
import CrudPage, { ColumnDef, FieldDef, FilterDef } from '@/components/admin/CrudPage';
import { PAY_MODES } from '@/components/admin/RecordPayment';

interface E { id: number; category: string; description: string; amount: number; expenseDate: string; paidTo?: string; mode?: string }

const cats = ['Electricity', 'Water', 'Stationery', 'Maintenance', 'Cleaning', 'Events', 'Sports equipment', 'Transport', 'Rent', 'Internet and phone', 'Other']
  .map((c) => ({ value: c, label: c }));

const fields: FieldDef[] = [
  { name: 'expenseDate', label: 'Date', type: 'date', required: true },
  { name: 'category', label: 'Category', type: 'select', options: cats, required: true },
  { name: 'description', label: 'Description', required: true, wide: true },
  { name: 'amount', label: 'Amount (Rs.)', type: 'number', required: true, min: 0.01, step: '0.01' },
  { name: 'mode', label: 'Paid by', type: 'select', options: PAY_MODES.map((m) => ({ value: m, label: m })) },
  { name: 'paidTo', label: 'Paid to', wide: true },
];

const filters: FilterDef[] = [{ name: 'category', label: 'Category', options: cats }];

const columns: ColumnDef<E>[] = [
  { header: 'Date', render: (e) => <span className="whitespace-nowrap">{shortDate(e.expenseDate)}</span> },
  { header: 'Category', render: (e) => e.category },
  { header: 'Description', render: (e) => e.description },
  { header: 'Paid to', render: (e) => e.paidTo },
  { header: 'Amount', className: 'text-right', render: (e) => <span className="font-semibold">{money(e.amount)}</span> },
];

export default function ExpensesPage() {
  return (
    <CrudPage<E>
      title="Expenses"
      sub="Money spent by the school other than teacher salaries."
      endpoint="/api/admin/finance/expenses"
      noun="expense"
      columns={columns}
      fields={fields}
      filters={filters}
      defaults={{ expenseDate: new Date().toISOString().slice(0, 10), mode: 'CASH' }}
      searchPlaceholder="Search description, category or payee..."
      describeRow={(e) => `expense "${e.description}"`}
    />
  );
}
