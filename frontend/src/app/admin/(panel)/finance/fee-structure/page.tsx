'use client';

import { money } from '@/lib/api';
import CrudPage, { ColumnDef, FieldDef, FilterDef } from '@/components/admin/CrudPage';

interface F { id: number; classId: number; headName: string; amount: number; frequency: string }

const LOOKUPS = { classes: '/api/admin/classes' };
const classOptions = (l: Record<string, any[]>) => (l.classes || []).map((c) => ({ value: c.id, label: c.name }));
const freq = [
  { value: 'MONTHLY', label: 'Every month (12 times a year)' },
  { value: 'QUARTERLY', label: 'Every quarter (4 times a year)' },
  { value: 'ANNUAL', label: 'Once a year' },
  { value: 'ONCE', label: 'One time charge' },
];

const fields: FieldDef[] = [
  { name: 'classId', label: 'Class', type: 'select', options: classOptions, required: true },
  { name: 'headName', label: 'Fee head', required: true, placeholder: 'Tuition fee' },
  { name: 'amount', label: 'Amount (Rs.)', type: 'number', required: true, min: 0, step: '0.01' },
  { name: 'frequency', label: 'How often', type: 'select', options: freq, required: true },
];

const filters: FilterDef[] = [{ name: 'classId', label: 'Class', options: classOptions }];

const columns: ColumnDef<F>[] = [
  { header: 'Class', render: (f, l) => (l.classes || []).find((c) => c.id === f.classId)?.name },
  { header: 'Fee head', render: (f) => <span className="font-medium">{f.headName}</span> },
  { header: 'Amount', className: 'text-right', render: (f) => money(f.amount) },
  { header: 'How often', render: (f) => freq.find((x) => x.value === f.frequency)?.label },
];

export default function FeeStructurePage() {
  return (
    <CrudPage<F>
      title="Fee structure"
      sub="Fee heads for each class. The yearly fee for a student is worked out from these (Apr to Mar)."
      endpoint="/api/admin/finance/fee-structure"
      noun="fee head"
      columns={columns}
      fields={fields}
      lookups={LOOKUPS}
      filters={filters}
      noSearch
      pageSize={50}
      defaults={{ frequency: 'MONTHLY' }}
      describeRow={(f) => `fee head ${f.headName}`}
    />
  );
}
