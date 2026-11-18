'use client';

import { FormEvent, useState } from 'react';
import { CheckCircle2, Send } from 'lucide-react';

interface Props {
  classes: string[];
  labels: {
    name: string; phone: string; email: string; cls: string; clsPlaceholder: string; message: string;
    send: string; sending: string; ok: string; err: string;
  };
}

export default function EnquiryForm({ classes, labels }: Props) {
  const [state, setState] = useState<'idle' | 'sending' | 'ok' | 'error'>('idle');
  const [message, setMessage] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const data = Object.fromEntries(new FormData(form).entries()) as Record<string, string>;
    setState('sending');
    setFieldErrors({});
    try {
      const res = await fetch('/api/public/enquiries', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: data.name,
          phone: data.phone,
          email: data.email || null,
          classInterested: data.classInterested || null,
          message: data.message || null,
          website: data.website || null,
        }),
      });
      if (res.ok) {
        form.reset();
        setState('ok');
        return;
      }
      const body = await res.json().catch(() => ({}));
      if (body.fields) setFieldErrors(body.fields);
      setMessage(res.status === 429 && body.message ? body.message : labels.err);
      setState('error');
    } catch {
      setMessage(labels.err);
      setState('error');
    }
  }

  if (state === 'ok') {
    return (
      <div role="status" className="flex flex-col items-center gap-3 rounded-2xl bg-green-50 p-10 text-center text-green-800 dark:bg-green-900/20 dark:text-green-200">
        <CheckCircle2 size={40} aria-hidden />
        <p className="text-lg font-medium">{labels.ok}</p>
      </div>
    );
  }

  const err = (k: string) => fieldErrors[k] && <p className="mt-1 text-xs text-red-600">{fieldErrors[k]}</p>;

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <div>
        <label htmlFor="name" className="label">{labels.name} *</label>
        <input id="name" name="name" required maxLength={150} autoComplete="name" className="input" />
        {err('name')}
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor="phone" className="label">{labels.phone} *</label>
          <input id="phone" name="phone" type="tel" required maxLength={20} autoComplete="tel" inputMode="tel" className="input" />
          {err('phone')}
        </div>
        <div>
          <label htmlFor="email" className="label">{labels.email}</label>
          <input id="email" name="email" type="email" maxLength={190} autoComplete="email" className="input" />
          {err('email')}
        </div>
      </div>
      <div>
        <label htmlFor="classInterested" className="label">{labels.cls}</label>
        <select id="classInterested" name="classInterested" className="input" defaultValue="">
          <option value="">{labels.clsPlaceholder}</option>
          {classes.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
      </div>
      <div>
        <label htmlFor="message" className="label">{labels.message}</label>
        <textarea id="message" name="message" rows={4} maxLength={2000} className="input" />
      </div>
      {/* Honeypot: hidden from people, filled in by bots */}
      <div aria-hidden className="absolute -left-[9999px] h-0 w-0 overflow-hidden">
        <label htmlFor="website">Website</label>
        <input id="website" name="website" tabIndex={-1} autoComplete="off" />
      </div>
      {state === 'error' && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-200">{message}</p>}
      <button type="submit" disabled={state === 'sending'} className="btn-primary w-full sm:w-auto">
        <Send size={16} aria-hidden /> {state === 'sending' ? labels.sending : labels.send}
      </button>
    </form>
  );
}
