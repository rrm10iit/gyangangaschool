'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { api, money } from '@/lib/api';
import { useUser } from '@/components/admin/AuthContext';
import { Alert, PageTitle, Spinner, StatCard } from '@/components/admin/parts';

const COLORS = ['#f2b705', '#2f52a8', '#16a34a', '#dc2626', '#7f9ade', '#a37600'];
const compact = (n: number) => new Intl.NumberFormat('en-IN', { notation: 'compact', maximumFractionDigits: 1 }).format(n);

export default function DashboardPage() {
  const user = useUser();
  const [d, setD] = useState<any>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/api/admin/dashboard').then(setD).catch((e) => setError(e.message));
  }, []);

  if (error) return <Alert>{error}</Alert>;
  if (!d) return <Spinner />;
  const f = d.finance;
  const net = f ? Number(f.collected) - Number(f.expensesTotal) : 0;

  return (
    <div>
      <PageTitle title={`Welcome, ${user.fullName}`} sub={`Academic year ${d.academicYear}`} />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Students" value={d.students} hint={`${d.classes} classes`} />
        <StatCard label="Teachers" value={d.teachers} />
        {f && <StatCard label="Fees collected (this year)" value={money(f.collected)} tone="green" hint={`${money(f.collectedThisMonth)} this month`} />}
        {f && <StatCard label="Fees pending (this year)" value={money(f.pending)} tone="gold" hint={`${money(f.overdue)} overdue so far`} />}
        {!f && <StatCard label="Classes" value={d.classes} />}
      </div>

      {f && (
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard label="Salaries and expenses paid" value={money(f.expensesTotal)} tone="red" />
          <StatCard label="Collected minus spent" value={money(net)} tone={net >= 0 ? 'green' : 'red'} />
          {d.newEnquiries != null && (
            <Link href="/admin/content/enquiries" className="block">
              <StatCard label="New admission enquiries" value={d.newEnquiries} hint="Click to review" />
            </Link>
          )}
        </div>
      )}

      <div className="mt-8 grid gap-6 xl:grid-cols-3">
        {f && (
          <div className="card p-5 xl:col-span-2">
            <h2 className="mb-4 text-lg font-semibold">Money in and out, month by month</h2>
            <div className="h-72 text-slate-500 dark:text-slate-400">
              <ResponsiveContainer>
                <BarChart data={f.series} margin={{ left: -10 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="currentColor" strokeOpacity={0.2} />
                  <XAxis dataKey="label" tick={{ fill: 'currentColor', fontSize: 11 }} tickFormatter={(v) => String(v).slice(0, 3)} />
                  <YAxis tick={{ fill: 'currentColor', fontSize: 11 }} tickFormatter={compact} />
                  <Tooltip formatter={(v: any) => money(v)} />
                  <Legend />
                  <Bar dataKey="income" name="Fees collected" fill="#16a34a" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="salaries" name="Salaries" fill="#2f52a8" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="expenses" name="Expenses" fill="#f2b705" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}
        {f && (
          <div className="card p-5">
            <h2 className="mb-4 text-lg font-semibold">How fees were paid</h2>
            <div className="h-72">
              {f.byMode.length === 0 ? <p className="text-sm text-slate-500">No payments yet.</p> : (
                <ResponsiveContainer>
                  <PieChart>
                    <Pie data={f.byMode} dataKey="amount" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2}>
                      {f.byMode.map((_: any, i: number) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                    </Pie>
                    <Tooltip formatter={(v: any) => money(v)} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>
        )}
        <div className={`card p-5 ${f ? 'xl:col-span-3' : 'xl:col-span-3'}`}>
          <h2 className="mb-4 text-lg font-semibold">Students in each class</h2>
          <div className="h-64 text-slate-500 dark:text-slate-400">
            <ResponsiveContainer>
              <BarChart data={d.studentsPerClass} margin={{ left: -20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="currentColor" strokeOpacity={0.2} />
                <XAxis dataKey="name" tick={{ fill: 'currentColor', fontSize: 11 }} interval={0} />
                <YAxis allowDecimals={false} tick={{ fill: 'currentColor', fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="students" name="Students" fill="#2f52a8" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
