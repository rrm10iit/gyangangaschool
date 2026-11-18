'use client';

import CrudPage, { ColumnDef, FieldDef } from '@/components/admin/CrudPage';
import { Badge } from '@/components/admin/parts';

interface T { id: number; name: string; role?: string; quote: string; photoUrl?: string; displayOrder: number; published: boolean }

const fields: FieldDef[] = [
  { name: 'name', label: 'Name', required: true },
  { name: 'role', label: 'Role or relation', placeholder: 'Parent of Class 6 student' },
  { name: 'quote', label: 'What they said (English)', type: 'textarea', required: true },
  { name: 'quoteHi', label: 'What they said (Hindi)', type: 'textarea', help: 'Optional. English is shown when empty.' },
  { name: 'displayOrder', label: 'Display order', type: 'number' },
  { name: 'published', label: 'Show on website', type: 'checkbox' },
  { name: 'photoUrl', label: 'Photo', type: 'image' },
];

const columns: ColumnDef<T>[] = [
  {
    header: 'Person',
    render: (t) => (
      <div className="flex items-center gap-3">
        {t.photoUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={t.photoUrl} alt="" className="h-9 w-9 rounded-full object-cover" />
        )}
        <div><p className="font-medium">{t.name}</p><p className="text-xs text-slate-500">{t.role}</p></div>
      </div>
    ),
  },
  { header: 'Quote', render: (t) => <span className="line-clamp-2 max-w-md text-xs">{t.quote}</span> },
  { header: 'Visible', render: (t) => <Badge tone={t.published ? 'green' : 'slate'}>{t.published ? 'Yes' : 'Hidden'}</Badge> },
];

export default function TestimonialsPage() {
  return (
    <CrudPage<T>
      title="Testimonials"
      sub="Words from parents and visitors, shown on the home page."
      endpoint="/api/admin/testimonials"
      noun="testimonial"
      columns={columns}
      fields={fields}
      defaults={{ published: true, displayOrder: 99 }}
      noSearch
      modalWide
      describeRow={(t) => `testimonial by ${t.name}`}
    />
  );
}
