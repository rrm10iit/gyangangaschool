'use client';

import CrudPage, { ColumnDef, FieldDef } from '@/components/admin/CrudPage';
import { Badge } from '@/components/admin/parts';

interface U { id: number; email: string; fullName: string; role: string; enabled: boolean }

const roles = [
  { value: 'ADMIN', label: 'Admin (everything)' },
  { value: 'ACCOUNTANT', label: 'Accountant (finance, view students)' },
  { value: 'TEACHER', label: 'Teacher (view students and teachers)' },
];

const fields: FieldDef[] = [
  { name: 'fullName', label: 'Full name', required: true },
  { name: 'email', label: 'Email (used to sign in)', type: 'email', required: true },
  { name: 'role', label: 'Role', type: 'select', options: roles, required: true },
  { name: 'enabled', label: 'Account is active', type: 'checkbox' },
  { name: 'password', label: 'Password', type: 'password', wide: true, help: 'At least 10 characters. When editing, leave blank to keep the current password.' },
];

const columns: ColumnDef<U>[] = [
  { header: 'Name', render: (u) => <span className="font-medium">{u.fullName}</span> },
  { header: 'Email', render: (u) => u.email },
  { header: 'Role', render: (u) => <Badge tone="blue">{u.role}</Badge> },
  { header: 'Status', render: (u) => <Badge tone={u.enabled ? 'green' : 'red'}>{u.enabled ? 'Active' : 'Disabled'}</Badge> },
];

export default function UsersPage() {
  return (
    <CrudPage<U>
      title="Staff accounts"
      sub="People who can sign in to this admin panel."
      endpoint="/api/admin/users"
      noun="account"
      columns={columns}
      fields={fields}
      defaults={{ enabled: true, role: 'TEACHER' }}
      describeRow={(u) => `account ${u.email}`}
      // The password is only sent when something was typed.
      transform={(p) => {
        if (!p.password) delete p.password;
        return p;
      }}
    />
  );
}
