'use client';

import CrudPage, { ColumnDef, FieldDef } from '@/components/admin/CrudPage';
import { Badge } from '@/components/admin/parts';

interface M { id: number; name: string; designation: string; photoUrl?: string; displayOrder: number; published: boolean }

const fields: FieldDef[] = [
  { name: 'name', label: 'Name', required: true },
  { name: 'designation', label: 'Designation (English)', required: true, placeholder: 'Manager' },
  { name: 'designationHi', label: 'Designation (Hindi)' },
  { name: 'displayOrder', label: 'Display order', type: 'number' },
  { name: 'published', label: 'Show on website', type: 'checkbox' },
  { name: 'photoUrl', label: 'Photo', type: 'image' },
];

const columns: ColumnDef<M>[] = [
  {
    header: 'Member',
    render: (m) => (
      <div className="flex items-center gap-3">
        {m.photoUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={m.photoUrl} alt="" className="h-9 w-9 rounded-full object-cover" />
        )}
        <span className="font-medium">{m.name}</span>
      </div>
    ),
  },
  { header: 'Designation', render: (m) => m.designation },
  { header: 'Order', render: (m) => m.displayOrder },
  { header: 'Visible', render: (m) => <Badge tone={m.published ? 'green' : 'slate'}>{m.published ? 'Yes' : 'Hidden'}</Badge> },
];

export default function ManagementPage() {
  return (
    <CrudPage<M>
      title="Management team"
      sub="Shown on the About page."
      endpoint="/api/admin/management"
      noun="member"
      columns={columns}
      fields={fields}
      defaults={{ published: true, displayOrder: 99 }}
      noSearch
      describeRow={(m) => `${m.name}`}
    />
  );
}
