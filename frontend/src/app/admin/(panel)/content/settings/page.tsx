'use client';

import { useEffect, useMemo, useState } from 'react';
import { Save } from 'lucide-react';
import { api } from '@/lib/api';
import { Alert, ImageUpload, PageTitle, Spinner } from '@/components/admin/parts';

interface Setting {
  id: number; key: string; groupName: string; label: string; kind: 'TEXT' | 'LONG' | 'IMAGE' | 'URL';
  valueEn?: string | null; valueHi?: string | null; sortOrder: number;
}

export default function SettingsPage() {
  const [items, setItems] = useState<Setting[] | null>(null);
  const [dirty, setDirty] = useState<Set<number>>(new Set());
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api<{ content: Setting[] }>('/api/admin/settings?size=500').then((r) => setItems(r.content)).catch((e) => setError(e.message));
  }, []);

  const groups = useMemo(() => {
    const m = new Map<string, Setting[]>();
    (items || []).forEach((s) => m.set(s.groupName, [...(m.get(s.groupName) || []), s]));
    return Array.from(m.entries());
  }, [items]);

  function change(id: number, patch: Partial<Setting>) {
    setItems((list) => list!.map((s) => (s.id === id ? { ...s, ...patch } : s)));
    setDirty((d) => new Set(d).add(id));
    setOk('');
  }

  async function save() {
    setSaving(true);
    setError('');
    setOk('');
    try {
      for (const id of Array.from(dirty)) {
        const s = items!.find((x) => x.id === id)!;
        await api(`/api/admin/settings/${id}`, {
          method: 'PUT',
          body: { ...s, valueEn: s.valueEn || null, valueHi: s.valueHi || null },
        });
      }
      setDirty(new Set());
      setOk('Saved. The public website now shows your changes.');
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  if (!items) return error ? <Alert>{error}</Alert> : <Spinner />;

  return (
    <div>
      <PageTitle
        title="Page content"
        sub="Edit the text and pictures on the public website. Hindi is optional; English is shown when a Hindi version is empty."
        actions={<button type="button" className="btn-primary" onClick={save} disabled={saving || dirty.size === 0}><Save size={16} aria-hidden /> {saving ? 'Saving...' : `Save changes${dirty.size ? ` (${dirty.size})` : ''}`}</button>}
      />
      <Alert onClose={() => setError('')}>{error}</Alert>
      <Alert kind="success" onClose={() => setOk('')}>{ok}</Alert>

      <div className="space-y-8">
        {groups.map(([group, list]) => (
          <section key={group} className="card p-5 sm:p-6">
            <h2 className="mb-5 text-lg font-bold">{group}</h2>
            <div className="space-y-6">
              {list.map((s) => (
                <div key={s.id}>
                  <p className="label">{s.label}</p>
                  {s.kind === 'IMAGE' ? (
                    <ImageUpload value={s.valueEn} onChange={(u) => change(s.id, { valueEn: u })} label={s.label} />
                  ) : (
                    <div className={s.kind === 'URL' ? '' : 'grid gap-3 md:grid-cols-2'}>
                      <Field s={s} lang="valueEn" placeholder="English" onChange={change} />
                      {s.kind !== 'URL' && <Field s={s} lang="valueHi" placeholder="हिन्दी (optional)" onChange={change} />}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}

function Field({ s, lang, placeholder, onChange }: { s: Setting; lang: 'valueEn' | 'valueHi'; placeholder: string; onChange: (id: number, p: Partial<Setting>) => void }) {
  const common = {
    'aria-label': `${s.label} (${lang === 'valueEn' ? 'English' : 'Hindi'})`,
    className: 'input',
    placeholder,
    value: s[lang] ?? '',
    onChange: (e: any) => onChange(s.id, { [lang]: e.target.value } as any),
  };
  return s.kind === 'LONG' ? <textarea rows={5} {...common} /> : <input type={s.kind === 'URL' ? 'url' : 'text'} {...common} />;
}
