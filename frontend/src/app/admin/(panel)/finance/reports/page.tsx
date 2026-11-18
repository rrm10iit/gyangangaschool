'use client';

import { useEffect, useState } from 'react';
import { FileSpreadsheet, FileText, Printer } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, currentAcademicYear, money, qs, yearLabel } from '@/lib/api';
import { Alert, PageTitle, Spinner, StatCard } from '@/components/admin/parts';

const months = [
  [4, 'April'], [5, 'May'], [6, 'June'], [7, 'July'], [8, 'August'], [9, 'September'],
  [10, 'October'], [11, 'November'], [12, 'December'], [1, 'January'], [2, 'February'], [3, 'March'],
] as const;

const compact = (n: number) => new Intl.NumberFormat('en-IN', { notation: 'compact', maximumFractionDigits: 1 }).format(n);

export default function ReportsPage() {
  const thisYear = currentAcademicYear();
  const [year, setYear] = useState(thisYear);
  const [month, setMonth] = useState('');
  const [data, setData] = useState<any>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setData(null);
    api(`/api/admin/finance/reports/summary${qs({ year, month })}`).then(setData).catch((e) => setError(e.message));
  }, [year, month]);

  const q = qs({ year, month });

  return (
    <div>
      <PageTitle
        title="Financial reports"
        sub="Fees collected, salaries paid and expenses, for a whole academic year or a single month."
        actions={
          <>
            <a className="btn-outline no-print" href={`/api/admin/finance/reports/export.xlsx${q}`}><FileSpreadsheet size={16} aria-hidden /> Excel</a>
            <a className="btn-outline no-print" href={`/api/admin/finance/reports/export.pdf${q}`}><FileText size={16} aria-hidden /> PDF</a>
            <button type="button" className="btn-outline no-print" onClick={() => window.print()}><Printer size={16} aria-hidden /> Print</button>
          </>
        }
      />
      <Alert onClose={() => setError('')}>{error}</Alert>

      <div className="no-print mb-6 flex flex-wrap gap-3">
        <div>
          <label htmlFor="year" className="label">Academic year</label>
          <select id="year" className="input w-auto" value={year} onChange={(e) => setYear(Number(e.target.value))}>
            {[thisYear + 1, thisYear, thisYear - 1, thisYear - 2, thisYear - 3].map((y) => <option key={y} value={y}>{yearLabel(y)} (Apr {y} to Mar {y + 1})</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="month" className="label">Period</label>
          <select id="month" className="input w-auto" value={month} onChange={(e) => setMonth(e.target.value)}>
            <option value="">Whole year</option>
            {months.map(([n, name]) => <option key={n} value={n}>{name}</option>)}
          </select>
        </div>
      </div>

      {!data ? <Spinner /> : (
        <>
          <h2 className="mb-4 text-lg font-semibold">{data.period}</h2>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Fees collected" value={money(data.income)} tone="green" hint={`${data.paymentCount} receipts`} />
            <StatCard label="Salaries paid" value={money(data.salaries)} />
            <StatCard label="Other expenses" value={money(data.expenses)} tone="gold" />
            <StatCard label="Surplus / (deficit)" value={money(data.net)} tone={Number(data.net) >= 0 ? 'green' : 'red'} />
          </div>

          <div className="card mt-6 p-5">
            <h3 className="mb-4 font-semibold">{data.month ? 'Day by day' : 'Month by month'}</h3>
            <div className="h-72 text-slate-500 dark:text-slate-400">
              <ResponsiveContainer>
                <BarChart data={data.series} margin={{ left: -10 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="currentColor" strokeOpacity={0.2} />
                  <XAxis dataKey="label" tick={{ fill: 'currentColor', fontSize: 11 }} tickFormatter={(v) => (data.month ? v : String(v).slice(0, 3))} />
                  <YAxis tick={{ fill: 'currentColor', fontSize: 11 }} tickFormatter={compact} />
                  <Tooltip formatter={(v: any) => money(v)} />
                  <Legend />
                  <Bar dataKey="income" name="Fees" fill="#16a34a" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="salaries" name="Salaries" fill="#2f52a8" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="expenses" name="Expenses" fill="#f2b705" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="mt-6 grid gap-6 md:grid-cols-2">
            <Breakdown title="Fees by payment mode" rows={data.byMode} />
            <Breakdown title="Expenses by category" rows={data.expenseByCategory} />
          </div>
        </>
      )}
    </div>
  );
}

function Breakdown({ title, rows }: { title: string; rows: { name: string; amount: number }[] }) {
  return (
    <div className="card p-5">
      <h3 className="mb-3 font-semibold">{title}</h3>
      {rows.length === 0 ? <p className="text-sm text-slate-500">Nothing recorded.</p> : (
        <table className="w-full text-sm">
          <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
            {rows.map((r) => (
              <tr key={r.name}><td className="py-2">{r.name}</td><td className="py-2 text-right font-medium">{money(r.amount)}</td></tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
