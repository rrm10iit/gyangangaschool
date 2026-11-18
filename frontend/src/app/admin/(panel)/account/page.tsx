'use client';

import { FormEvent, useState } from 'react';
import { api } from '@/lib/api';
import { useUser } from '@/components/admin/AuthContext';
import { Alert, PageTitle } from '@/components/admin/parts';

export default function AccountPage() {
  const user = useUser();
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const f = new FormData(form);
    setError('');
    setOk('');
    if (f.get('newPassword') !== f.get('confirm')) {
      setError('The new passwords do not match.');
      return;
    }
    setBusy(true);
    try {
      await api('/api/auth/change-password', {
        method: 'POST',
        body: { currentPassword: f.get('currentPassword'), newPassword: f.get('newPassword') },
      });
      form.reset();
      setOk('Your password has been changed.');
    } catch (err: any) {
      setError(err.fields?.newPassword || err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="max-w-xl">
      <PageTitle title="My account" />
      <div className="card mb-6 p-5 text-sm">
        <p><span className="text-slate-500">Name:</span> <b>{user.fullName}</b></p>
        <p><span className="text-slate-500">Email:</span> <b>{user.email}</b></p>
        <p><span className="text-slate-500">Role:</span> <b>{user.role}</b></p>
      </div>
      <form onSubmit={submit} className="card space-y-4 p-5">
        <h2 className="text-lg font-semibold">Change password</h2>
        <Alert>{error}</Alert>
        <Alert kind="success">{ok}</Alert>
        <div>
          <label htmlFor="currentPassword" className="label">Current password</label>
          <input id="currentPassword" name="currentPassword" type="password" required autoComplete="current-password" className="input" />
        </div>
        <div>
          <label htmlFor="newPassword" className="label">New password</label>
          <input id="newPassword" name="newPassword" type="password" required minLength={10} autoComplete="new-password" className="input" />
          <p className="mt-1 text-xs text-slate-500">At least 10 characters.</p>
        </div>
        <div>
          <label htmlFor="confirm" className="label">Confirm new password</label>
          <input id="confirm" name="confirm" type="password" required minLength={10} autoComplete="new-password" className="input" />
        </div>
        <button type="submit" className="btn-navy" disabled={busy}>{busy ? 'Saving...' : 'Change password'}</button>
      </form>
    </div>
  );
}
