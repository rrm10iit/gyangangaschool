'use client';

import CrudPage, { ColumnDef, FieldDef } from '@/components/admin/CrudPage';
import { Badge } from '@/components/admin/parts';

interface Facility { id: number; title: string; icon?: string; imageUrl?: string; displayOrder: number; published: boolean }

const icons = [
  ['library', 'Library'], ['labs', 'Science laboratory'], ['computer', 'Computer lab'], ['sports', 'Sports'], ['transport', 'Transport'],
  ['smart', 'Smart classroom'], ['canteen', 'Canteen'],
].map(([value, label]) => ({ value, label }));

const fields: FieldDef[] = [
  { name: 'title', label: 'Title (English)', required: true },
  { name: 'titleHi', label: 'Title (Hindi)' },
  { name: 'description', label: 'Description (English)', type: 'textarea' },
  { name: 'descriptionHi', label: 'Description (Hindi)', type: 'textarea', help: 'Optional. English is shown when empty.' },
  { name: 'icon', label: 'Icon', type: 'select', options: icons, help: 'Shown when there is no photo. Leave blank for a default icon.' },
  { name: 'displayOrder', label: 'Display order', type: 'number' },
  { name: 'published', label: 'Show on website', type: 'checkbox' },
  { name: 'imageUrl', label: 'Photo', type: 'image' },
];

const columns: ColumnDef<Facility>[] = [
  {
    header: 'Facility',
    render: (f) => (
      <div className="flex items-center gap-3">
        {f.imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={f.imageUrl} alt="" className="h-10 w-14 rounded object-cover" />
        ) : <span className="flex h-10 w-14 items-center justify-center rounded bg-navy-100 text-xs text-navy-600">No photo</span>}
        <span className="font-medium">{f.title}</span>
      </div>
    ),
  },
  { header: 'Order', render: (f) => f.displayOrder },
  { header: 'Visible', render: (f) => <Badge tone={f.published ? 'green' : 'slate'}>{f.published ? 'Yes' : 'Hidden'}</Badge> },
];

export default function FacilitiesPage() {
  return (
    <CrudPage<Facility>
      title="Facilities"
      sub="These appear on the public Facilities page. Upload a photo for each one."
      endpoint="/api/admin/facilities"
      noun="facility"
      columns={columns}
      fields={fields}
      defaults={{ published: true, displayOrder: 99 }}
      noSearch
      modalWide
      describeRow={(f) => `facility ${f.title}`}
    />
  );
}
