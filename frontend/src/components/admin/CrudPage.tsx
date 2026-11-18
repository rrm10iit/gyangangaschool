'use client';

import { FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import { Pencil, Plus, Search, Trash2 } from 'lucide-react';
import { ApiError, api, qs } from '@/lib/api';
import { Alert, ImageUpload, Modal, Pager, Spinner } from './parts';

export type Option = { value: string | number | boolean; label: string };
type Lookups = Record<string, any[]>;

export interface FieldDef {
  name: string;
  label: string;
  type?: 'text' | 'email' | 'tel' | 'textarea' | 'number' | 'date' | 'month' | 'select' | 'checkbox' | 'image' | 'password';
  options?: Option[] | ((lookups: Lookups) => Option[]);
  required?: boolean;
  wide?: boolean;
  help?: string;
  placeholder?: string;
  step?: string;
  min?: number;
  /** Hide the field when editing an existing record (e.g. password on create only). */
  createOnly?: boolean;
  /** Show the field as read-only text when editing an existing record. */
  readOnlyOnEdit?: boolean;
}

export interface ColumnDef<T> {
  header: string;
  render: (row: T, lookups: Lookups) => ReactNode;
  className?: string;
}

export interface FilterDef {
  name: string;
  label: string;
  options: Option[] | ((lookups: Lookups) => Option[]);
}

export interface CrudProps<T> {
  title: string;
  sub?: string;
  endpoint: string;
  columns: ColumnDef<T>[];
  fields: FieldDef[];
  /** Other endpoints to load as lookup lists, e.g. { classes: '/api/admin/classes' }. */
  lookups?: Record<string, string>;
  filters?: FilterDef[];
  searchPlaceholder?: string;
  canCreate?: boolean;
  canEdit?: boolean;
  canDelete?: boolean;
  newLabel?: string;
  defaults?: Record<string, unknown>;
  rowActions?: (row: T, reload: () => void, lookups: Lookups) => ReactNode;
  headerActions?: ReactNode;
  pageSize?: number;
  noun?: string;
  modalWide?: boolean;
  /** Called before saving to adjust the payload. */
  transform?: (payload: Record<string, any>) => Record<string, any>;
  describeRow?: (row: T) => string;
  noSearch?: boolean;
}

const resolve = (o: FieldDef['options'] | FilterDef['options'] | undefined, l: Lookups): Option[] =>
  !o ? [] : typeof o === 'function' ? o(l) : o;

export default function CrudPage<T extends { id: number }>(props: CrudProps<T>) {
  const {
    title, sub, endpoint, columns, fields, lookups: lookupMap, filters = [], canCreate = true, canEdit = true,
    canDelete = true, pageSize = 20, noun = 'record', noSearch,
  } = props;

  const [rows, setRows] = useState<T[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [filterValues, setFilterValues] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [lookups, setLookups] = useState<Lookups>({});
  const [editing, setEditing] = useState<{ row: T | null } | null>(null);

  const lookupKey = JSON.stringify(lookupMap || {});
  useEffect(() => {
    const map: Record<string, string> = JSON.parse(lookupKey);
    Object.entries(map).forEach(async ([key, url]) => {
      try {
        const r = await api<any>(`${url}${url.includes('?') ? '&' : '?'}size=500`);
        setLookups((l) => ({ ...l, [key]: Array.isArray(r) ? r : r.content }));
      } catch { /* the page itself will surface errors */ }
    });
  }, [lookupKey]);

  // debounce search
  useEffect(() => {
    const id = setTimeout(() => { setSearch(q); setPage(0); }, 300);
    return () => clearTimeout(id);
  }, [q]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const r = await api<{ content: T[]; total: number }>(`${endpoint}${qs({ q: search, page, size: pageSize, ...filterValues })}`);
      setRows(r.content);
      setTotal(r.total);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [endpoint, search, page, pageSize, filterValues]);

  useEffect(() => { load(); }, [load]);

  async function remove(row: T) {
    const label = props.describeRow ? props.describeRow(row) : `this ${noun}`;
    if (!window.confirm(`Delete ${label}? This cannot be undone.`)) return;
    try {
      await api(`${endpoint}/${row.id}`, { method: 'DELETE' });
      setNotice(`Deleted ${label}.`);
      load();
    } catch (e: any) {
      setError(e.message);
    }
  }

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">{title}</h1>
          {sub && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{sub}</p>}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {props.headerActions}
          {canCreate && (
            <button type="button" className="btn-primary" onClick={() => setEditing({ row: null })}>
              <Plus size={16} aria-hidden /> {props.newLabel || `Add ${noun}`}
            </button>
          )}
        </div>
      </div>

      <Alert onClose={() => setError('')}>{error}</Alert>
      <Alert kind="success" onClose={() => setNotice('')}>{notice}</Alert>

      {(!noSearch || filters.length > 0) && (
        <div className="mb-4 flex flex-wrap items-end gap-3">
          {!noSearch && (
            <div className="relative min-w-[220px] flex-1">
              <label htmlFor="crud-search" className="sr-only">Search</label>
              <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" aria-hidden />
              <input id="crud-search" className="input !pl-9" placeholder={props.searchPlaceholder || 'Search...'} value={q} onChange={(e) => setQ(e.target.value)} />
            </div>
          )}
          {filters.map((f) => (
            <div key={f.name}>
              <label htmlFor={`f-${f.name}`} className="sr-only">{f.label}</label>
              <select id={`f-${f.name}`} className="input" value={filterValues[f.name] ?? ''}
                onChange={(e) => { setFilterValues((v) => ({ ...v, [f.name]: e.target.value })); setPage(0); }}>
                <option value="">{f.label}: all</option>
                {resolve(f.options, lookups).map((o) => <option key={String(o.value)} value={String(o.value)}>{o.label}</option>)}
              </select>
            </div>
          ))}
        </div>
      )}

      <div className="card relative overflow-x-auto">
        <table className="w-full min-w-[640px] text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wider text-slate-500 dark:border-navy-800 dark:bg-navy-800/50 dark:text-slate-400">
            <tr>
              {columns.map((c) => <th key={c.header} scope="col" className={`px-4 py-3 font-semibold ${c.className || ''}`}>{c.header}</th>)}
              <th scope="col" className="px-4 py-3 text-right font-semibold"><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
            {loading && <tr><td colSpan={columns.length + 1}><Spinner /></td></tr>}
            {!loading && rows.length === 0 && (
              <tr><td colSpan={columns.length + 1} className="px-4 py-10 text-center text-slate-500">No {noun}s found.</td></tr>
            )}
            {!loading && rows.map((row) => (
              <tr key={row.id} className="hover:bg-slate-50 dark:hover:bg-navy-800/40">
                {columns.map((c) => <td key={c.header} className={`px-4 py-3 align-top ${c.className || ''}`}>{c.render(row, lookups)}</td>)}
                <td className="whitespace-nowrap px-4 py-3 text-right">
                  <div className="flex justify-end gap-1">
                    {props.rowActions?.(row, load, lookups)}
                    {canEdit && (
                      <button type="button" className="rounded-lg p-2 text-navy-700 hover:bg-navy-50 dark:text-gold-300 dark:hover:bg-navy-800" aria-label="Edit" onClick={() => setEditing({ row })}>
                        <Pencil size={16} />
                      </button>
                    )}
                    {canDelete && (
                      <button type="button" className="rounded-lg p-2 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20" aria-label="Delete" onClick={() => remove(row)}>
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={page} size={pageSize} total={total} onPage={setPage} />

      {editing && (
        <RecordForm
          {...props}
          lookupData={lookups}
          row={editing.row}
          onClose={() => setEditing(null)}
          onSaved={(msg) => { setEditing(null); setNotice(msg); load(); }}
        />
      )}
    </div>
  );
}

function RecordForm<T extends { id: number }>(props: CrudProps<T> & { lookupData: Lookups; row: T | null; onClose: () => void; onSaved: (msg: string) => void }) {
  const { fields, row, endpoint, noun = 'record' } = props;
  const lookups = props.lookupData;
  const isNew = row === null;
  const initial = useMemo(() => {
    const v: Record<string, any> = {};
    for (const f of fields) {
      const existing = row ? (row as any)[f.name] : props.defaults?.[f.name];
      v[f.name] = existing ?? (f.type === 'checkbox' ? false : '');
    }
    return v;
  }, [fields, row, props.defaults]);
  const [values, setValues] = useState<Record<string, any>>(initial);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const set = (name: string, value: any) => setValues((v) => ({ ...v, [name]: value }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError('');
    setFieldErrors({});
    let payload: Record<string, any> = { ...(row || {}) };
    for (const f of fields) {
      if (f.createOnly && !isNew) continue;
      let v = values[f.name];
      if (f.type === 'number') v = v === '' || v === null ? null : Number(v);
      else if (f.type === 'checkbox') v = Boolean(v);
      else if (v === '') v = null;
      payload[f.name] = v;
    }
    if (props.transform) payload = props.transform(payload);
    try {
      await api(isNew ? endpoint : `${endpoint}/${row!.id}`, { method: isNew ? 'POST' : 'PUT', body: payload });
      props.onSaved(isNew ? `Added ${noun}.` : `Saved ${noun}.`);
    } catch (err: any) {
      setError(err.message);
      if (err instanceof ApiError && err.fields) setFieldErrors(err.fields);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={isNew ? props.newLabel || `Add ${noun}` : `Edit ${noun}`} onClose={props.onClose} wide={props.modalWide}>
      <form onSubmit={submit} className="space-y-4">
        <Alert>{error}</Alert>
        <div className="grid gap-4 sm:grid-cols-2">
          {fields.filter((f) => !(f.createOnly && !isNew)).map((f) => {
            const id = `fld-${f.name}`;
            const wide = f.wide || f.type === 'textarea' || f.type === 'image';
            const err = fieldErrors[f.name];
            return (
              <div key={f.name} className={wide ? 'sm:col-span-2' : ''}>
                {f.type !== 'checkbox' && <label htmlFor={id} className="label">{f.label}{f.required && ' *'}</label>}
                {f.type === 'textarea' ? (
                  <textarea id={id} className="input" rows={5} value={values[f.name] ?? ''} onChange={(e) => set(f.name, e.target.value)} required={f.required} placeholder={f.placeholder} />
                ) : f.type === 'select' ? (
                  <select id={id} className="input" required={f.required} disabled={f.readOnlyOnEdit && !isNew}
                    value={String(values[f.name] ?? '')}
                    onChange={(e) => {
                      const opt = resolve(f.options, lookups).find((o) => String(o.value) === e.target.value);
                      set(f.name, opt ? opt.value : '');
                    }}>
                    <option value="">{f.placeholder || 'Select...'}</option>
                    {resolve(f.options, lookups).map((o) => <option key={String(o.value)} value={String(o.value)}>{o.label}</option>)}
                  </select>
                ) : f.type === 'checkbox' ? (
                  <label className="flex items-center gap-2 pt-6 text-sm font-medium">
                    <input id={id} type="checkbox" className="h-4 w-4 rounded border-slate-300" checked={Boolean(values[f.name])} onChange={(e) => set(f.name, e.target.checked)} />
                    {f.label}
                  </label>
                ) : f.type === 'image' ? (
                  <ImageUpload value={values[f.name]} onChange={(u) => set(f.name, u)} label={f.label} />
                ) : (
                  <input id={id} className="input" type={f.type || 'text'} required={f.required} placeholder={f.placeholder}
                    step={f.step} min={f.min} readOnly={f.readOnlyOnEdit && !isNew}
                    autoComplete={f.type === 'password' ? 'new-password' : 'off'}
                    value={values[f.name] ?? ''} onChange={(e) => set(f.name, e.target.value)} />
                )}
                {f.help && <p className="mt-1 text-xs text-slate-500">{f.help}</p>}
                {err && <p className="mt-1 text-xs text-red-600">{err}</p>}
              </div>
            );
          })}
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <button type="button" className="btn-outline" onClick={props.onClose}>Cancel</button>
          <button type="submit" className="btn-navy" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
        </div>
      </form>
    </Modal>
  );
}
