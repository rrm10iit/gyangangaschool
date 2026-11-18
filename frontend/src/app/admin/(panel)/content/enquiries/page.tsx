'use client';

import { dateTime } from '@/lib/api';
import CrudPage, { ColumnDef, FieldDef, FilterDef } from '@/components/admin/CrudPage';
import { Badge } from '@/components/admin/parts';

interface E { id: number; name: string; phone: string; email?: string; classInterested?: string; message?: string; status: string; createdAt: string }

const statuses = [
  { value: 'NEW', label: 'New' }, { value: 'CONTACTED', label: 'Contacted' }, { value: 'CLOSED', label: 'Closed' },
];

const fields: FieldDef[] = [{ name: 'status', label: 'Status', type: 'select', options: statuses, required: true }];
const filters: FilterDef[] = [{ name: 'status', label: 'Status', options: statuses }];

const columns: ColumnDef<E>[] = [
  { header: 'Received', render: (e) => <span className="whitespace-nowrap text-xs">{dateTime(e.createdAt)}</span> },
  {
    header: 'Parent',
    render: (e) => (
      <div>
        <p className="font-medium">{e.name}</p>
        <a className="text-xs text-navy-700 hover:underline dark:text-gold-300" href={`tel:${e.phone}`}>{e.phone}</a>
        {e.email && <p className="text-xs"><a href={`mailto:${e.email}`} className="hover:underline">{e.email}</a></p>}
      </div>
    ),
  },
  { header: 'Class', render: (e) => e.classInterested },
  { header: 'Message', render: (e) => <span className="line-clamp-3 max-w-sm text-xs">{e.message}</span> },
  { header: 'Status', render: (e) => <Badge tone={e.status === 'NEW' ? 'gold' : e.status === 'CONTACTED' ? 'blue' : 'slate'}>{e.status}</Badge> },
];

export default function EnquiriesPage() {
  return (
    <CrudPage<E>
      title="Admission enquiries"
      sub="Messages sent from the public Contact page. Update the status as you follow up."
      endpoint="/api/admin/enquiries"
      noun="enquiry"
      columns={columns}
      fields={fields}
      filters={filters}
      canCreate={false}
      searchPlaceholder="Search by name, phone, email or message..."
      describeRow={(e) => `enquiry from ${e.name}`}
    />
  );
}
