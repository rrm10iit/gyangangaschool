'use client';

import { ReactNode, useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, ImagePlus, Loader2, X } from 'lucide-react';
import { uploadImage } from '@/lib/api';

export function PageTitle({ title, sub, actions }: { title: string; sub?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold">{title}</h1>
        {sub && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{sub}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function Alert({ kind = 'error', children, onClose }: { kind?: 'error' | 'success'; children: ReactNode; onClose?: () => void }) {
  if (!children) return null;
  const cls = kind === 'error'
    ? 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-200'
    : 'bg-green-50 text-green-800 dark:bg-green-900/20 dark:text-green-200';
  return (
    <div role={kind === 'error' ? 'alert' : 'status'} className={`mb-4 flex items-start justify-between gap-3 rounded-lg p-3 text-sm ${cls}`}>
      <span>{children}</span>
      {onClose && <button type="button" onClick={onClose} aria-label="Dismiss"><X size={16} /></button>}
    </div>
  );
}

export function Modal({ title, onClose, children, wide = false }: { title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const prev = document.activeElement as HTMLElement | null;
    ref.current?.focus();
    document.body.style.overflow = 'hidden';
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
      prev?.focus();
    };
  }, [onClose]);
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/60 p-4 sm:p-8" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div ref={ref} tabIndex={-1} role="dialog" aria-modal="true" aria-label={title}
        className={`card w-full ${wide ? 'max-w-4xl' : 'max-w-2xl'} p-5 outline-none sm:p-7`}>
        <div className="mb-5 flex items-center justify-between gap-4">
          <h2 className="text-xl font-bold">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Close" className="rounded-lg p-2 hover:bg-slate-100 dark:hover:bg-navy-800"><X size={20} /></button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Pager({ page, size, total, onPage }: { page: number; size: number; total: number; onPage: (p: number) => void }) {
  const pages = Math.max(1, Math.ceil(total / size));
  if (total <= size) return <p className="mt-3 text-xs text-slate-500">{total} record{total === 1 ? '' : 's'}</p>;
  return (
    <div className="mt-4 flex items-center justify-between text-sm">
      <span className="text-slate-500">{total} records</span>
      <div className="flex items-center gap-2">
        <button type="button" className="btn-outline !px-3 !py-2" disabled={page === 0} onClick={() => onPage(page - 1)} aria-label="Previous page"><ChevronLeft size={16} /></button>
        <span>Page {page + 1} of {pages}</span>
        <button type="button" className="btn-outline !px-3 !py-2" disabled={page + 1 >= pages} onClick={() => onPage(page + 1)} aria-label="Next page"><ChevronRight size={16} /></button>
      </div>
    </div>
  );
}

export function ImageUpload({ value, onChange, label }: { value?: string | null; onChange: (url: string | null) => void; label: string }) {
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const input = useRef<HTMLInputElement>(null);

  async function pick(f?: File) {
    if (!f) return;
    setBusy(true);
    setErr('');
    try {
      onChange(await uploadImage(f));
    } catch (e: any) {
      setErr(e.message || 'Upload failed');
    } finally {
      setBusy(false);
      if (input.current) input.current.value = '';
    }
  }

  return (
    <div>
      <div className="flex items-center gap-4">
        {value ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={value} alt="" className="h-20 w-20 rounded-lg border border-slate-200 object-cover dark:border-navy-700" />
        ) : (
          <div className="flex h-20 w-20 items-center justify-center rounded-lg border border-dashed border-slate-300 text-slate-400 dark:border-navy-600"><ImagePlus size={24} /></div>
        )}
        <div className="flex flex-wrap gap-2">
          <button type="button" className="btn-outline !py-2" onClick={() => input.current?.click()} disabled={busy}>
            {busy ? <Loader2 size={16} className="animate-spin" /> : <ImagePlus size={16} />} {value ? 'Replace' : 'Upload'} photo
          </button>
          {value && <button type="button" className="btn-outline !py-2" onClick={() => onChange(null)}>Remove</button>}
        </div>
        <input ref={input} type="file" accept="image/jpeg,image/png" className="sr-only" aria-label={label} onChange={(e) => pick(e.target.files?.[0])} />
      </div>
      {err && <p className="mt-1 text-xs text-red-600">{err}</p>}
    </div>
  );
}

export function Spinner() {
  return <div className="flex justify-center p-10 text-slate-400"><Loader2 className="animate-spin" /></div>;
}

export function StatCard({ label, value, hint, tone = 'navy' }: { label: string; value: ReactNode; hint?: string; tone?: 'navy' | 'gold' | 'green' | 'red' }) {
  const tones = {
    navy: 'text-navy-800 dark:text-white',
    gold: 'text-gold-700 dark:text-gold-400',
    green: 'text-green-700 dark:text-green-400',
    red: 'text-red-600 dark:text-red-400',
  };
  return (
    <div className="card p-5">
      <p className="text-sm text-slate-500 dark:text-slate-400">{label}</p>
      <p className={`mt-1 text-2xl font-extrabold sm:text-3xl ${tones[tone]}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </div>
  );
}

export function Badge({ children, tone = 'slate' }: { children: ReactNode; tone?: 'slate' | 'green' | 'red' | 'gold' | 'blue' }) {
  const tones = {
    slate: 'bg-slate-100 text-slate-700 dark:bg-navy-800 dark:text-slate-300',
    green: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
    red: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300',
    gold: 'bg-gold-100 text-gold-800 dark:bg-gold-500/20 dark:text-gold-300',
    blue: 'bg-navy-100 text-navy-800 dark:bg-navy-700 dark:text-navy-100',
  };
  return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${tones[tone]}`}>{children}</span>;
}
