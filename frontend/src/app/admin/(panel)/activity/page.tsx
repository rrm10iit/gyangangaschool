'use client';

import { dateTime } from '@/lib/api';
import CrudPage, { ColumnDef } from '@/components/admin/CrudPage';
import { Badge } from '@/components/admin/parts';

interface L { id: number; userEmail?: string; action: string; entityType?: string; summary?: string; createdAt: string }

const tone = (a: string) => (a === 'DELETE' || a === 'LOGIN_FAILED' ? 'red' : a === 'CREATE' ? 'green' : a === 'UPDATE' ? 'blue' : 'slate') as any;

const columns: ColumnDef<L>[] = [
  { header: 'When', render: (l) => <span className="whitespace-nowrap text-xs">{dateTime(l.createdAt)}</span> },
  { header: 'Who', render: (l) => l.userEmail },
  { header: 'Action', render: (l) => <Badge tone={tone(l.action)}>{l.action.replace('_', ' ')}</Badge> },
  { header: 'What', render: (l) => l.entityType },
  { header: 'Details', render: (l) => <span className="text-xs">{l.summary}</span> },
];

export default function ActivityPage() {
  return (
    <CrudPage<L>
      title="Activity log"
      sub="A record of who changed what, and when."
      endpoint="/api/admin/audit-log"
      noun="entry"
      columns={columns}
      fields={[]}
      canCreate={false}
      canEdit={false}
      canDelete={false}
      pageSize={30}
      searchPlaceholder="Search by person, action or details..."
    />
  );
}
