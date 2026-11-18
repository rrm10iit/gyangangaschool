'use client';

import { FormEvent, useEffect, useState } from 'react';
import { Printer, Search } from 'lucide-react';
import { api, money, qs } from '@/lib/api';
import { Alert, Modal } from './parts';

export const PAY_MODES = ['CASH', 'UPI', 'CARD', 'BANK', 'CHEQUE'];

interface StudentLite { id: number; fullName: string; admissionNo: string; classId: number; section?: string }

/** Modal to record a fee payment. Optionally starts with a student already chosen. */
export default function RecordPayment({
  initialStudent, classes, onClose, onSaved,
}: {
  initialStudent?: StudentLite | null;
  classes: { id: number; name: string }[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [student, setStudent] = useState<StudentLite | null>(initialStudent ?? null);
  const [search, setSearch] = useState('');
  const [results, setResults] = useState<StudentLite[]>([]);
  const [ledger, setLedger] = useState<any>(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [receipt, setReceipt] = useState<{ id: number; no: string } | null>(null);

  useEffect(() => {
    if (student || search.trim().length < 2) { setResults([]); return; }
    const id = setTimeout(() => {
      api<{ content: StudentLite[] }>(`/api/admin/students${qs({ q: search.trim(), size: 8, status: 'ACTIVE' })}`)
        .then((r) => setResults(r.content)).catch(() => setResults([]));
    }, 250);
    return () => clearTimeout(id);
  }, [search, student]);

  useEffect(() => {
    setLedger(null);
    if (student) api(`/api/admin/finance/students/${student.id}/ledger`).then(setLedger).catch(() => undefined);
  }, [student]);

  const className = (s: StudentLite) => classes.find((c) => c.id === s.classId)?.name ?? '';

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!student) { setError('Please choose a student.'); return; }
    const f = new FormData(e.currentTarget);
    setSaving(true);
    setError('');
    try {
      const p = await api<{ id: number; receiptNo: string }>('/api/admin/finance/payments', {
        method: 'POST',
        body: {
          studentId: student.id,
          amount: Number(f.get('amount')),
          paymentDate: f.get('paymentDate'),
          mode: f.get('mode'),
          forPeriod: f.get('forPeriod') || null,
          reference: f.get('reference') || null,
          remarks: f.get('remarks') || null,
        },
      });
      setReceipt({ id: p.id, no: p.receiptNo });
      onSaved();
    } catch (err: any) {
      setError(err.fields ? Object.values(err.fields).join(' ') : err.message);
    } finally {
      setSaving(false);
    }
  }

  if (receipt) {
    return (
      <Modal title="Payment recorded" onClose={onClose}>
        <p className="text-sm">Receipt <b>{receipt.no}</b> has been created.</p>
        <div className="mt-5 flex flex-wrap justify-end gap-3">
          <button type="button" className="btn-outline" onClick={onClose}>Close</button>
          <a className="btn-primary" href={`/api/admin/finance/payments/${receipt.id}/receipt.pdf`} target="_blank" rel="noopener noreferrer">
            <Printer size={16} aria-hidden /> Open printable receipt
          </a>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="Record fee payment" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <Alert>{error}</Alert>

        {student ? (
          <div className="rounded-xl bg-slate-50 p-4 dark:bg-navy-800/50">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="font-semibold">{student.fullName}</p>
                <p className="text-xs text-slate-500">{student.admissionNo} &middot; {className(student)}{student.section ? ` - ${student.section}` : ''}</p>
              </div>
              {!initialStudent && <button type="button" className="text-sm text-navy-700 hover:underline dark:text-gold-300" onClick={() => { setStudent(null); setSearch(''); }}>Change</button>}
            </div>
            {ledger && (
              <dl className="mt-3 grid grid-cols-2 gap-2 text-sm sm:grid-cols-4">
                <div><dt className="text-xs text-slate-500">Yearly fee</dt><dd className="font-semibold">{money(ledger.totalFee)}</dd></div>
                <div><dt className="text-xs text-slate-500">Paid</dt><dd className="font-semibold text-green-700 dark:text-green-400">{money(ledger.paid)}</dd></div>
                <div><dt className="text-xs text-slate-500">Pending</dt><dd className="font-semibold text-gold-700 dark:text-gold-400">{money(ledger.pending)}</dd></div>
                <div><dt className="text-xs text-slate-500">Overdue</dt><dd className="font-semibold text-red-600">{money(ledger.overdue)}</dd></div>
              </dl>
            )}
          </div>
        ) : (
          <div>
            <label htmlFor="stu-search" className="label">Student *</label>
            <div className="relative">
              <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" aria-hidden />
              <input id="stu-search" className="input !pl-9" placeholder="Type a name or admission number" value={search} onChange={(e) => setSearch(e.target.value)} autoComplete="off" />
            </div>
            {results.length > 0 && (
              <ul className="mt-1 max-h-56 overflow-y-auto rounded-lg border border-slate-200 bg-white dark:border-navy-700 dark:bg-navy-900">
                {results.map((s) => (
                  <li key={s.id}>
                    <button type="button" className="block w-full px-3 py-2 text-left text-sm hover:bg-slate-50 dark:hover:bg-navy-800" onClick={() => setStudent(s)}>
                      <span className="font-medium">{s.fullName}</span>
                      <span className="ml-2 text-xs text-slate-500">{s.admissionNo} &middot; {className(s)}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="amount" className="label">Amount received (Rs.) *</label>
            <input id="amount" name="amount" type="number" min="0.01" step="0.01" required className="input" />
          </div>
          <div>
            <label htmlFor="paymentDate" className="label">Date *</label>
            <input id="paymentDate" name="paymentDate" type="date" required defaultValue={new Date().toISOString().slice(0, 10)} className="input" />
          </div>
          <div>
            <label htmlFor="mode" className="label">Payment mode *</label>
            <select id="mode" name="mode" className="input" defaultValue="CASH">{PAY_MODES.map((m) => <option key={m}>{m}</option>)}</select>
          </div>
          <div>
            <label htmlFor="forPeriod" className="label">Paid for</label>
            <input id="forPeriod" name="forPeriod" className="input" placeholder="Tuition November" maxLength={100} />
          </div>
          <div>
            <label htmlFor="reference" className="label">Reference / transaction id</label>
            <input id="reference" name="reference" className="input" maxLength={100} />
          </div>
          <div>
            <label htmlFor="remarks" className="label">Remarks</label>
            <input id="remarks" name="remarks" className="input" maxLength={300} />
          </div>
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <button type="button" className="btn-outline" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-navy" disabled={saving || !student}>{saving ? 'Saving...' : 'Save and create receipt'}</button>
        </div>
      </form>
    </Modal>
  );
}
