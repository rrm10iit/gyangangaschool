export class ApiError extends Error {
  status: number;
  fields?: Record<string, string>;
  constructor(status: number, message: string, fields?: Record<string, string>) {
    super(message);
    this.status = status;
    this.fields = fields;
  }
}

interface Options {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  form?: FormData;
}

/** Calls the backend with the session cookie. Sends the user to the login page on 401. */
export async function api<T = any>(path: string, opts: Options = {}): Promise<T> {
  const { method = 'GET', body, form } = opts;
  const res = await fetch(path, {
    method,
    credentials: 'same-origin',
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: form ?? (body !== undefined ? JSON.stringify(body) : undefined),
    cache: 'no-store',
  });
  const isJson = res.headers.get('content-type')?.includes('application/json');
  const data = isJson ? await res.json().catch(() => null) : null;
  if (res.status === 401 && !path.startsWith('/api/auth/login')) {
    if (typeof window !== 'undefined') window.location.href = '/admin/login';
    throw new ApiError(401, 'Please sign in again');
  }
  if (!res.ok) throw new ApiError(res.status, data?.message || `Request failed (${res.status})`, data?.fields);
  return data as T;
}

export async function uploadImage(file: File): Promise<string> {
  const form = new FormData();
  form.append('file', file);
  const r = await api<{ url: string }>('/api/admin/uploads', { method: 'POST', form });
  return r.url;
}

export function qs(params: Record<string, string | number | boolean | null | undefined>): string {
  const p = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') p.set(k, String(v));
  }
  const s = p.toString();
  return s ? `?${s}` : '';
}

export const money = (n: number | string | null | undefined) =>
  '₹' + new Intl.NumberFormat('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 2 }).format(Number(n ?? 0));

export const shortDate = (d?: string | null) =>
  d ? new Date(d.length <= 10 ? d + 'T00:00:00' : d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '';

export const dateTime = (d?: string | null) =>
  d ? new Date(d).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '';

export function currentAcademicYear(): number {
  const d = new Date();
  return d.getMonth() >= 3 ? d.getFullYear() : d.getFullYear() - 1;
}

export const yearLabel = (y: number) => `${y}-${String((y + 1) % 100).padStart(2, '0')}`;
