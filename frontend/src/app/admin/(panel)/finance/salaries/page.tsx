'use client';

import { money, shortDate } from '@/lib/api';
import CrudPage, { ColumnDef, FieldDef, FilterDef } from '@/components/admin/CrudPage';
import { PAY_MODES } from '@/components/admin/RecordPayment';
import { Badge } from '@/components/admin/parts';

interface S {
  id: number; teacherId: number; month: string; baseAmount: number; allowances: number; deductions: number;
  netAmount: number; status: string; paidDate?: string; mode?: string;
}

const LOOKUPS = { teachers: '/api/admin/teachers' };
const teacherOptions = (l: Record<string, any[]>) => (l.teachers || []).map((t) => ({ value: t.id, label: `${t.fullName} (${t.employeeNo})` }));
const statuses = [{ value: 'PENDING', label: 'Not paid yet' }, { value: 'PAID', label: 'Paid' }];

const fields: FieldDef[] = [
  { name: 'teacherId', label: 'Teacher', type: 'select', options: teacherOptions, required: true },
  { name: 'month', label: 'Salary month', type: 'month', required: true },
  { name: 'baseAmount', label: 'Basic salary (Rs.)', type: 'number', required: true, min: 0, step: '0.01', help: "See the teacher's monthly salary on the Teachers page." },
  { name: 'allowances', label: 'Allowances (Rs.)', type: 'number', min: 0, step: '0.01' },
  { name: 'deductions', label: 'Deductions (Rs.)', type: 'number', min: 0, step: '0.01' },
  { name: 'status', label: 'Status', type: 'select', options: statuses, required: true },
  { name: 'paidDate', label: 'Paid on', type: 'date', help: 'Needed when marked as paid. Defaults to today.' },
  { name: 'mode', label: 'Paid by', type: 'select', options: PAY_MODES.map((m) => ({ value: m, label: m })) },
  { name: 'remarks', label: 'Remarks', wide: true },
];

const filters: FilterDef[] = [
  { name: 'teacherId', label: 'Teacher', options: teacherOptions },
  { name: 'status', label: 'Status', options: statuses },
];

const columns: ColumnDef<S>[] = [
  { header: 'Month', render: (s) => s.month },
  { header: 'Teacher', render: (s, l) => <span className="font-medium">{(l.teachers || []).find((t) => t.id === s.teacherId)?.fullName}</span> },
  { header: 'Basic', className: 'text-right', render: (s) => money(s.baseAmount) },
  { header: 'Allowances', className: 'text-right', render: (s) => money(s.allowances) },
  { header: 'Deductions', className: 'text-right', render: (s) => money(s.deductions) },
  { header: 'Net', className: 'text-right', render: (s) => <span className="font-semibold">{money(s.netAmount)}</span> },
  { header: 'Status', render: (s) => (<div><Badge tone={s.status === 'PAID' ? 'green' : 'gold'}>{s.status === 'PAID' ? 'Paid' : 'Pending'}</Badge>{s.paidDate && <p className="mt-1 text-xs text-slate-500">{shortDate(s.paidDate)}</p>}</div>) },
];

export default function SalariesPage() {
  const now = new Date();
  return (
    <CrudPage<S>
      title="Teacher salaries"
      sub="One record per teacher per month. The net amount is worked out for you."
      endpoint="/api/admin/finance/salaries"
      noun="salary record"
      columns={columns}
      fields={fields}
      lookups={LOOKUPS}
      filters={filters}
      noSearch
      modalWide
      defaults={{ month: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`, status: 'PENDING', allowances: 0, deductions: 0 }}
      describeRow={(s) => `salary record for ${s.month}`}
    />
  );
}
