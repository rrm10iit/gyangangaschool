'use client';

import CrudPage, { ColumnDef, FieldDef } from '@/components/admin/CrudPage';

interface Cls { id: number; name: string; displayOrder: number; ageGroup?: string; sections: string; seats?: number; subjects?: string }

const fields: FieldDef[] = [
  { name: 'name', label: 'Class name', required: true, placeholder: 'Class 1' },
  { name: 'displayOrder', label: 'Display order', type: 'number', required: true, help: 'Smaller numbers appear first.' },
  { name: 'ageGroup', label: 'Age group', placeholder: '6+ years' },
  { name: 'sections', label: 'Sections', required: true, placeholder: 'A, B', help: 'Comma separated.' },
  { name: 'seats', label: 'Seats per section', type: 'number', min: 0 },
  { name: 'subjects', label: 'Subjects', wide: true, help: 'Comma separated. Shown on the public Classes page.' },
  { name: 'description', label: 'Description (English)', type: 'textarea' },
  { name: 'descriptionHi', label: 'Description (Hindi)', type: 'textarea', help: 'Optional. English is shown when empty.' },
];

const columns: ColumnDef<Cls>[] = [
  { header: 'Order', render: (c) => c.displayOrder },
  { header: 'Class', render: (c) => <span className="font-medium">{c.name}</span> },
  { header: 'Age', render: (c) => c.ageGroup },
  { header: 'Sections', render: (c) => c.sections },
  { header: 'Seats', render: (c) => c.seats },
  { header: 'Subjects', render: (c) => <span className="line-clamp-2 max-w-xs text-xs">{c.subjects}</span> },
];

export default function ClassesPage() {
  return (
    <CrudPage<Cls>
      title="Classes"
      sub="Classes appear on the public website and in student and fee records."
      endpoint="/api/admin/classes"
      noun="class"
      columns={columns}
      fields={fields}
      defaults={{ sections: 'A', displayOrder: 99 }}
      pageSize={50}
      describeRow={(c) => `class ${c.name}`}
    />
  );
}
