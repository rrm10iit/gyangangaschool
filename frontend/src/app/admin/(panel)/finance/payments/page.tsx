'use client';

import { useEffect, useState } from 'react';
import { Printer } from 'lucide-react';
import { api, money, shortDate } from '@/lib/api';
import { useUser } from '@/components/admin/AuthContext';
import CrudPage, { ColumnDef, FilterDef } from '@/components/admin/CrudPage';
import RecordPayment, { PAY_MODES } from '@/components/admin/RecordPayment';
import { Badge } from '@/components/admin/parts';

interface P {
  id: number; receiptNo: string; studentName: string; admissionNo: string; className: string; section?: string;
  amount: number; paymentDate: string; mode: string; forPeriod?: string; collectedBy?: string;
}

const filters: FilterDef[] = [{ name: 'mode', label: 'Mode', options: PAY_MODES.map((m) => ({ value: m, label: m })) }];

export default function PaymentsPage() {
  const user = useUser();
  const [adding, setAdding] = useState(false);
  const [classes, setClasses] = useState<{ id: number; name: string }[]>([]);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    api<{ content: any[] }>('/api/admin/classes?size=500').then((r) => setClasses(r.content)).catch(() => undefined);
  }, []);

  const columns: ColumnDef<P>[] = [
    { header: 'Receipt', render: (p) => <span className="whitespace-nowrap font-mono text-xs">{p.receiptNo}</span> },
    { header: 'Date', render: (p) => <span className="whitespace-nowrap">{shortDate(p.paymentDate)}</span> },
    { header: 'Student', render: (p) => (<div><p className="font-medium">{p.studentName}</p><p className="text-xs text-slate-500">{p.admissionNo} &middot; {p.className}{p.section ? ` - ${p.section}` : ''}</p></div>) },
    { header: 'Amount', className: 'text-right', render: (p) => <span className="font-semibold">{money(p.amount)}</span> },
    { header: 'Mode', render: (p) => <Badge tone="blue">{p.mode}</Badge> },
    { header: 'For', render: (p) => p.forPeriod },
    { header: 'Received by', render: (p) => <span className="text-xs">{p.collectedBy}</span> },
  ];

  return (
    <>
      {/* key forces the list to reload after a payment is recorded */}
      <CrudPage<P>
        key={version}
        title="Fee payments"
        sub="Record fees received and print receipts."
        endpoint="/api/admin/finance/payments"
        noun="payment"
        columns={columns}
        fields={[]}
        filters={filters}
        canCreate={false}
        canEdit={false}
        canDelete={user.role === 'ADMIN'}
        searchPlaceholder="Search by student name, admission number or receipt number..."
        describeRow={(p) => `receipt ${p.receiptNo} (cancelling a receipt removes the money from the books)`}
        headerActions={<button type="button" className="btn-primary" onClick={() => setAdding(true)}>Record payment</button>}
        rowActions={(p) => (
          <a className="rounded-lg p-2 text-navy-700 hover:bg-navy-50 dark:text-gold-300 dark:hover:bg-navy-800" href={`/api/admin/finance/payments/${p.id}/receipt.pdf`} target="_blank" rel="noopener noreferrer" aria-label={`Print receipt ${p.receiptNo}`}>
            <Printer size={16} />
          </a>
        )}
      />
      {adding && <RecordPayment classes={classes} onClose={() => setAdding(false)} onSaved={() => setVersion((v) => v + 1)} />}
    </>
  );
}
