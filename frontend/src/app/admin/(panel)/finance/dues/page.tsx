'use client';

import { useCallback, useEffect, useState } from 'react';
import { Download } from 'lucide-react';
import { api, currentAcademicYear, money, qs, yearLabel } from '@/lib/api';
import RecordPayment from '@/components/admin/RecordPayment';
import { Alert, PageTitle, Spinner, StatCard } from '@/components/admin/parts';

interface Row {
  studentId: number; admissionNo: string; fullName: string; classId: number; className: string; section?: string;
  guardianPhone?: string; totalFee: number; paid: number; pending: number; overdue: number;
}

export default function DuesPage() {
  const [rows, setRows] = useState<Row[] | null>(null);
  const [classes, setClasses] = useState<{ id: number; name: string }[]>([]);
  const [classId, setClassId] = useState('');
  const [year, setYear] = useState(currentAcademicYear());
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [collect, setCollect] = useState<Row | null>(null);

  useEffect(() => {
    api<{ content: any[] }>('/api/admin/classes?size=500').then((r) => setClasses(r.content)).catch(() => undefined);
  }, []);
  useEffect(() => {
    const id = setTimeout(() => setSearch(q), 300);
    return () => clearTimeout(id);
  }, [q]);

  const load = useCallback(() => {
    setRows(null);
    api<Row[]>(`/api/admin/finance/dues${qs({ classId, year, q: search, overdueOnly })}`)
      .then(setRows).catch((e) => setError(e.message));
  }, [classId, year, search, overdueOnly]);
  useEffect(load, [load]);

  const totalPending = (rows || []).reduce((a, r) => a + Number(r.pending), 0);
  const totalOverdue = (rows || []).reduce((a, r) => a + Number(r.overdue), 0);
  const thisYear = currentAcademicYear();

  function exportCsv() {
    const head = ['Admission no', 'Student', 'Class', 'Guardian phone', 'Yearly fee', 'Paid', 'Pending', 'Overdue'];
    const lines = (rows || []).map((r) => [r.admissionNo, r.fullName, `${r.className}${r.section ? ' ' + r.section : ''}`, r.guardianPhone ?? '', r.totalFee, r.paid, r.pending, r.overdue]
      .map((v) => `"${String(v).replace(/"/g, '""')}"`).join(','));
    const blob = new Blob([[head.join(','), ...lines].join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `pending-fees-${yearLabel(year)}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  return (
    <div>
      <PageTitle
        title="Pending and overdue fees"
        sub="Pending is what is left to pay for the whole year. Overdue is what should already have been paid up to today."
        actions={<button type="button" className="btn-outline" onClick={exportCsv} disabled={!rows?.length}><Download size={16} aria-hidden /> Download list</button>}
      />
      <Alert onClose={() => setError('')}>{error}</Alert>

      <div className="mb-4 grid gap-4 sm:grid-cols-2">
        <StatCard label="Total pending" value={money(totalPending)} tone="gold" />
        <StatCard label="Total overdue so far" value={money(totalOverdue)} tone="red" />
      </div>

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <input aria-label="Search student" className="input max-w-xs" placeholder="Search student..." value={q} onChange={(e) => setQ(e.target.value)} />
        <select aria-label="Class" className="input w-auto" value={classId} onChange={(e) => setClassId(e.target.value)}>
          <option value="">All classes</option>
          {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select aria-label="Academic year" className="input w-auto" value={year} onChange={(e) => setYear(Number(e.target.value))}>
          {[thisYear + 1, thisYear, thisYear - 1, thisYear - 2].map((y) => <option key={y} value={y}>Year {yearLabel(y)}</option>)}
        </select>
        <label className="flex items-center gap-2 pb-2 text-sm"><input type="checkbox" checked={overdueOnly} onChange={(e) => setOverdueOnly(e.target.checked)} /> Overdue only</label>
      </div>

      <div className="card overflow-x-auto">
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wider text-slate-500 dark:border-navy-800 dark:bg-navy-800/50">
            <tr>
              <th className="px-4 py-3">Student</th><th className="px-4 py-3">Class</th><th className="px-4 py-3">Guardian phone</th>
              <th className="px-4 py-3 text-right">Yearly fee</th><th className="px-4 py-3 text-right">Paid</th>
              <th className="px-4 py-3 text-right">Pending</th><th className="px-4 py-3 text-right">Overdue</th><th className="px-4 py-3"><span className="sr-only">Action</span></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
            {!rows && <tr><td colSpan={8}><Spinner /></td></tr>}
            {rows?.length === 0 && <tr><td colSpan={8} className="px-4 py-10 text-center text-slate-500">Nothing pending. All fees are paid up.</td></tr>}
            {rows?.map((r) => (
              <tr key={r.studentId} className="hover:bg-slate-50 dark:hover:bg-navy-800/40">
                <td className="px-4 py-3"><p className="font-medium">{r.fullName}</p><p className="text-xs text-slate-500">{r.admissionNo}</p></td>
                <td className="px-4 py-3">{r.className}{r.section ? ` - ${r.section}` : ''}</td>
                <td className="px-4 py-3">{r.guardianPhone && <a className="hover:underline" href={`tel:${r.guardianPhone}`}>{r.guardianPhone}</a>}</td>
                <td className="px-4 py-3 text-right">{money(r.totalFee)}</td>
                <td className="px-4 py-3 text-right text-green-700 dark:text-green-400">{money(r.paid)}</td>
                <td className="px-4 py-3 text-right font-semibold text-gold-700 dark:text-gold-400">{money(r.pending)}</td>
                <td className={`px-4 py-3 text-right font-semibold ${Number(r.overdue) > 0 ? 'text-red-600' : ''}`}>{money(r.overdue)}</td>
                <td className="px-4 py-3 text-right"><button type="button" className="btn-outline !px-3 !py-1.5 text-xs" onClick={() => setCollect(r)}>Collect</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {collect && (
        <RecordPayment
          classes={classes}
          initialStudent={{ id: collect.studentId, fullName: collect.fullName, admissionNo: collect.admissionNo, classId: collect.classId, section: collect.section }}
          onClose={() => setCollect(null)}
          onSaved={load}
        />
      )}
    </div>
  );
}
