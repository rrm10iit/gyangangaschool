'use client';

import Link from 'next/link';
import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import { LogIn } from 'lucide-react';
import { api } from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setBusy(true);
    setError('');
    try {
      await api('/api/auth/login', { method: 'POST', body: { email: f.get('email'), password: f.get('password') } });
      router.replace('/admin');
      router.refresh();
    } catch (err: any) {
      setError(err.message || 'Sign in failed');
      setBusy(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-navy-950 via-navy-900 to-navy-700 p-4">
      <div className="card w-full max-w-md p-8">
        <div className="flex flex-col items-center text-center">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/images/logo-192.png" alt="Gyan Ganga School" width={80} height={80} className="h-20 w-20 rounded-full" />
          <h1 className="mt-4 text-2xl font-bold">Staff sign in</h1>
          <p className="mt-1 text-sm text-slate-500">Gyan Ganga School administration</p>
        </div>
        <form onSubmit={submit} className="mt-6 space-y-4">
          {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-200">{error}</p>}
          <div>
            <label htmlFor="email" className="label">Email</label>
            <input id="email" name="email" type="email" required autoComplete="username" className="input" />
          </div>
          <div>
            <label htmlFor="password" className="label">Password</label>
            <input id="password" name="password" type="password" required autoComplete="current-password" className="input" />
          </div>
          <button type="submit" className="btn-navy w-full" disabled={busy}>
            <LogIn size={16} aria-hidden /> {busy ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
        <p className="mt-6 text-center text-sm"><Link href="/" className="text-navy-700 hover:underline dark:text-gold-300">Back to website</Link></p>
      </div>
    </div>
  );
}
