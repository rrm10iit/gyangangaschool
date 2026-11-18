'use client';

import { shortDate, money } from '@/lib/api';
import { useUser } from '@/components/admin/AuthContext';
import CrudPage, { ColumnDef, FieldDef, FilterDef } from '@/components/admin/CrudPage';
import { Badge } from '@/components/admin/parts';

interface Teacher {
  id: number; employeeNo: string; fullName: string; subjects?: string; classesAssigned?: string; qualification?: string;
  phone?: string; email?: string; joiningDate?: string; photoUrl?: string; monthlySalary: number; active: boolean;
}

const fields: FieldDef[] = [
  { name: 'employeeNo', label: 'Employee number', required: true },
  { name: 'fullName', label: 'Teacher name', required: true },
  { name: 'subjects', label: 'Subjects', placeholder: 'Mathematics, Science' },
  { name: 'classesAssigned', label: 'Classes assigned', placeholder: 'Class 5, Class 6' },
  { name: 'qualification', label: 'Qualification' },
  { name: 'joiningDate', label: 'Joining date', type: 'date' },
  { name: 'phone', label: 'Phone', type: 'tel' },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'address', label: 'Address', wide: true },
  { name: 'monthlySalary', label: 'Monthly salary (Rs.)', type: 'number', min: 0, step: '0.01', help: 'Used as the default when creating salary records.' },
  { name: 'active', label: 'Currently working at the school', type: 'checkbox' },
  { name: 'photoUrl', label: 'Photo', type: 'image' },
];

const filters: FilterDef[] = [
  { name: 'active', label: 'Status', options: [{ value: 'true', label: 'Working' }, { value: 'false', label: 'Left' }] },
];

export default function TeachersPage() {
  const user = useUser();
  const canWrite = user.role === 'ADMIN';
  const showSalary = user.role !== 'TEACHER';

  const columns: ColumnDef<Teacher>[] = [
    {
      header: 'Teacher',
      render: (t) => (
        <div className="flex items-center gap-3">
          {t.photoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={t.photoUrl} alt="" className="h-9 w-9 rounded-full object-cover" />
          ) : (
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-navy-100 text-xs font-bold text-navy-700">{t.fullName.slice(0, 1)}</span>
          )}
          <div><p className="font-medium">{t.fullName}</p><p className="text-xs text-slate-500">{t.qualification}</p></div>
        </div>
      ),
    },
    { header: 'Employee no.', render: (t) => t.employeeNo },
    { header: 'Subjects', render: (t) => t.subjects },
    { header: 'Classes', render: (t) => t.classesAssigned },
    { header: 'Contact', render: (t) => (<div><p>{t.phone}</p><p className="text-xs text-slate-500">{t.email}</p></div>) },
    { header: 'Joined', render: (t) => shortDate(t.joiningDate) },
    ...(showSalary ? [{ header: 'Salary', render: (t: Teacher) => money(t.monthlySalary) }] : []),
    { header: 'Status', render: (t) => <Badge tone={t.active ? 'green' : 'slate'}>{t.active ? 'Working' : 'Left'}</Badge> },
  ];

  const activeFields = showSalary ? fields : fields.filter((f) => f.name !== 'monthlySalary');

  return (
    <CrudPage<Teacher>
      title="Teachers"
      sub={canWrite ? 'Add, edit and search teacher records.' : 'You can view teacher records.'}
      endpoint="/api/admin/teachers"
      noun="teacher"
      columns={columns}
      fields={activeFields}
      filters={filters}
      searchPlaceholder="Search by name, subject, class or phone..."
      canCreate={canWrite}
      canEdit={canWrite}
      canDelete={canWrite}
      defaults={{ active: true, monthlySalary: 0 }}
      modalWide
      describeRow={(t) => `teacher ${t.fullName}`}
    />
  );
}
