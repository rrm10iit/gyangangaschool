'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import { FileText, Trash2, Upload } from 'lucide-react';
import { ApiError, api, shortDate } from '@/lib/api';
import { useUser } from '@/components/admin/AuthContext';
import CrudPage, { ColumnDef, FieldDef, FilterDef } from '@/components/admin/CrudPage';
import { Alert, Badge, Modal, Spinner } from '@/components/admin/parts';

interface Student {
  id: number; admissionNo: string; fullName: string; classId: number; section?: string; guardianName?: string;
  guardianPhone?: string; photoUrl?: string; status: string; dateOfBirth?: string;
}

const LOOKUPS = { classes: '/api/admin/classes' };
const classOptions = (l: Record<string, any[]>) => (l.classes || []).map((c) => ({ value: c.id, label: c.name }));

const fields: FieldDef[] = [
  { name: 'admissionNo', label: 'Admission number', required: true },
  { name: 'fullName', label: 'Student name', required: true },
  { name: 'classId', label: 'Class', type: 'select', options: classOptions, required: true },
  { name: 'section', label: 'Section', placeholder: 'A' },
  { name: 'rollNo', label: 'Roll number', type: 'number', min: 1 },
  { name: 'gender', label: 'Gender', type: 'select', options: ['Male', 'Female', 'Other'].map((v) => ({ value: v, label: v })) },
  { name: 'dateOfBirth', label: 'Date of birth', type: 'date' },
  { name: 'admissionDate', label: 'Admission date', type: 'date' },
  { name: 'guardianName', label: 'Guardian name' },
  { name: 'guardianPhone', label: 'Guardian phone', type: 'tel' },
  { name: 'guardianEmail', label: 'Guardian email', type: 'email' },
  { name: 'contactPhone', label: 'Student contact phone', type: 'tel' },
  { name: 'address', label: 'Address', wide: true },
  { name: 'feeDiscount', label: 'Fee concession per year (Rs.)', type: 'number', min: 0, step: '0.01', help: 'Deducted from the yearly fee total. Leave 0 for none.' },
  { name: 'status', label: 'Status', type: 'select', options: [{ value: 'ACTIVE', label: 'Active' }, { value: 'LEFT', label: 'Left school' }], required: true },
  { name: 'photoUrl', label: 'Photo', type: 'image' },
];

const filters: FilterDef[] = [
  { name: 'classId', label: 'Class', options: classOptions },
  { name: 'status', label: 'Status', options: [{ value: 'ACTIVE', label: 'Active' }, { value: 'LEFT', label: 'Left' }] },
];

const columns: ColumnDef<Student>[] = [
  {
    header: 'Student',
    render: (s) => (
      <div className="flex items-center gap-3">
        {s.photoUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={s.photoUrl} alt="" className="h-9 w-9 rounded-full object-cover" />
        ) : (
          <span className="flex h-9 w-9 items-center justify-center rounded-full bg-navy-100 text-xs font-bold text-navy-700">{s.fullName.slice(0, 1)}</span>
        )}
        <div>
          <p className="font-medium">{s.fullName}</p>
          <p className="text-xs text-slate-500">{s.dateOfBirth ? `Born ${shortDate(s.dateOfBirth)}` : ''}</p>
        </div>
      </div>
    ),
  },
  { header: 'Admission no.', render: (s) => s.admissionNo },
  { header: 'Class', render: (s, l) => `${(l.classes || []).find((c) => c.id === s.classId)?.name ?? ''}${s.section ? ' - ' + s.section : ''}` },
  { header: 'Guardian', render: (s) => (<div><p>{s.guardianName}</p><p className="text-xs text-slate-500">{s.guardianPhone}</p></div>) },
  { header: 'Status', render: (s) => <Badge tone={s.status === 'ACTIVE' ? 'green' : 'slate'}>{s.status === 'ACTIVE' ? 'Active' : 'Left'}</Badge> },
];

export default function StudentsPage() {
  const user = useUser();
  const canWrite = user.role === 'ADMIN';
  const [docsFor, setDocsFor] = useState<Student | null>(null);

  return (
    <>
      <CrudPage<Student>
        title="Students"
        sub={canWrite ? 'Add, edit and search student records.' : 'You can view student records.'}
        endpoint="/api/admin/students"
        noun="student"
        columns={columns}
        fields={fields}
        lookups={LOOKUPS}
        filters={filters}
        searchPlaceholder="Search by name, admission number or guardian..."
        canCreate={canWrite}
        canEdit={canWrite}
        canDelete={canWrite}
        defaults={{ status: 'ACTIVE', feeDiscount: 0, section: 'A' }}
        modalWide
        describeRow={(s) => `student ${s.fullName}`}
        rowActions={(s) => (
          <button type="button" className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-navy-800" aria-label={`Documents for ${s.fullName}`} onClick={() => setDocsFor(s)}>
            <FileText size={16} />
          </button>
        )}
      />
      {docsFor && <StudentDocs student={docsFor} canWrite={canWrite} onClose={() => setDocsFor(null)} />}
    </>
  );
}

interface Doc { id: number; name: string; fileUrl: string; createdAt: string }

function StudentDocs({ student, canWrite, onClose }: { student: Student; canWrite: boolean; onClose: () => void }) {
  const [docs, setDocs] = useState<Doc[] | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    api<Doc[]>(`/api/admin/students/${student.id}/documents`).then(setDocs).catch((e) => setError(e.message));
  }, [student.id]);
  useEffect(load, [load]);

  async function upload(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const data = new FormData(form);
    setBusy(true);
    setError('');
    try {
      await api(`/api/admin/students/${student.id}/documents`, { method: 'POST', form: data });
      form.reset();
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Upload failed');
    } finally {
      setBusy(false);
    }
  }

  async function remove(d: Doc) {
    if (!window.confirm(`Delete "${d.name}"?`)) return;
    try {
      await api(`/api/admin/students/${student.id}/documents/${d.id}`, { method: 'DELETE' });
      load();
    } catch (err: any) {
      setError(err.message);
    }
  }

  return (
    <Modal title={`Documents: ${student.fullName}`} onClose={onClose}>
      <Alert>{error}</Alert>
      {!docs ? <Spinner /> : (
        <ul className="divide-y divide-slate-100 dark:divide-navy-800">
          {docs.length === 0 && <li className="py-6 text-center text-sm text-slate-500">No documents uploaded.</li>}
          {docs.map((d) => (
            <li key={d.id} className="flex items-center justify-between gap-3 py-3">
              <div>
                <a href={d.fileUrl} target="_blank" rel="noopener noreferrer" className="font-medium text-navy-700 hover:underline dark:text-gold-300">{d.name}</a>
                <p className="text-xs text-slate-500">Added {shortDate(d.createdAt)}</p>
              </div>
              {canWrite && <button type="button" onClick={() => remove(d)} className="rounded-lg p-2 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20" aria-label={`Delete ${d.name}`}><Trash2 size={16} /></button>}
            </li>
          ))}
        </ul>
      )}
      {canWrite && (
        <form onSubmit={upload} className="mt-5 grid gap-3 border-t border-slate-200 pt-5 dark:border-navy-800 sm:grid-cols-[1fr_1fr_auto]">
          <div>
            <label htmlFor="doc-name" className="label">Document name</label>
            <input id="doc-name" name="name" required maxLength={150} placeholder="Birth certificate" className="input" />
          </div>
          <div>
            <label htmlFor="doc-file" className="label">File (PDF, JPG or PNG, up to 10 MB)</label>
            <input id="doc-file" name="file" type="file" required accept="application/pdf,image/jpeg,image/png" className="input !p-1.5" />
          </div>
          <div className="flex items-end">
            <button type="submit" className="btn-navy" disabled={busy}><Upload size={16} aria-hidden /> {busy ? 'Uploading...' : 'Upload'}</button>
          </div>
        </form>
      )}
    </Modal>
  );
}
