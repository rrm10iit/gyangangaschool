'use client';

import { shortDate } from '@/lib/api';
import CrudPage, { ColumnDef, FieldDef } from '@/components/admin/CrudPage';
import { Badge } from '@/components/admin/parts';

interface A { id: number; title: string; publishDate: string; pinned: boolean; published: boolean }

const today = () => new Date().toISOString().slice(0, 10);

const fields: FieldDef[] = [
  { name: 'title', label: 'Title (English)', required: true, wide: true },
  { name: 'titleHi', label: 'Title (Hindi)', wide: true },
  { name: 'body', label: 'Details (English)', type: 'textarea' },
  { name: 'bodyHi', label: 'Details (Hindi)', type: 'textarea', help: 'Optional. English is shown when empty.' },
  { name: 'publishDate', label: 'Publish date', type: 'date', required: true, help: 'Future dates stay hidden until that day.' },
  { name: 'pinned', label: 'Pin to the top as important', type: 'checkbox' },
  { name: 'published', label: 'Show on website', type: 'checkbox' },
];

const columns: ColumnDef<A>[] = [
  { header: 'Title', render: (a) => <span className="font-medium">{a.title}</span> },
  { header: 'Date', render: (a) => shortDate(a.publishDate) },
  { header: 'Pinned', render: (a) => (a.pinned ? <Badge tone="gold">Important</Badge> : '') },
  { header: 'Visible', render: (a) => <Badge tone={a.published ? 'green' : 'slate'}>{a.published ? 'Yes' : 'Hidden'}</Badge> },
];

export default function AnnouncementsPage() {
  return (
    <CrudPage<A>
      title="Announcements"
      sub="News and notices shown on the home page and the News page."
      endpoint="/api/admin/announcements"
      noun="announcement"
      columns={columns}
      fields={fields}
      defaults={{ published: true, pinned: false, publishDate: today() }}
      modalWide
      describeRow={(a) => `announcement "${a.title}"`}
    />
  );
}
