'use client';

import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { Images, Trash2, Upload } from 'lucide-react';
import { api, shortDate } from '@/lib/api';
import CrudPage, { ColumnDef, FieldDef } from '@/components/admin/CrudPage';
import { Alert, Badge, Modal, Spinner } from '@/components/admin/parts';

interface Album { id: number; title: string; category: string; eventDate?: string; published: boolean }
interface Photo { id: number; url: string; caption?: string }

const categories = [
  { value: 'EVENTS', label: 'Events' }, { value: 'SPORTS', label: 'Sports' },
  { value: 'ANNUAL_DAY', label: 'Annual Day' }, { value: 'OTHER', label: 'Other' },
];

const fields: FieldDef[] = [
  { name: 'title', label: 'Album title (English)', required: true },
  { name: 'titleHi', label: 'Album title (Hindi)' },
  { name: 'category', label: 'Category', type: 'select', options: categories, required: true },
  { name: 'eventDate', label: 'Event date', type: 'date' },
  { name: 'published', label: 'Show on website', type: 'checkbox' },
];

const columns: ColumnDef<Album>[] = [
  { header: 'Album', render: (a) => <span className="font-medium">{a.title}</span> },
  { header: 'Category', render: (a) => categories.find((c) => c.value === a.category)?.label ?? a.category },
  { header: 'Date', render: (a) => shortDate(a.eventDate) },
  { header: 'Visible', render: (a) => <Badge tone={a.published ? 'green' : 'slate'}>{a.published ? 'Yes' : 'Hidden'}</Badge> },
];

export default function GalleryAdminPage() {
  const [managing, setManaging] = useState<Album | null>(null);
  return (
    <>
      <CrudPage<Album>
        title="Photo gallery"
        sub="Create albums, then use the pictures button to upload photos into an album."
        endpoint="/api/admin/gallery/albums"
        noun="album"
        columns={columns}
        fields={fields}
        defaults={{ published: true, category: 'EVENTS' }}
        newLabel="New album"
        describeRow={(a) => `album "${a.title}" and all its photos`}
        rowActions={(a) => (
          <button type="button" className="rounded-lg p-2 text-navy-700 hover:bg-navy-50 dark:text-gold-300 dark:hover:bg-navy-800" aria-label={`Manage photos in ${a.title}`} onClick={() => setManaging(a)}>
            <Images size={16} />
          </button>
        )}
      />
      {managing && <PhotoManager album={managing} onClose={() => setManaging(null)} />}
    </>
  );
}

function PhotoManager({ album, onClose }: { album: Album; onClose: () => void }) {
  const [photos, setPhotos] = useState<Photo[] | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const input = useRef<HTMLInputElement>(null);

  const load = useCallback(() => {
    api<Photo[]>(`/api/admin/gallery/albums/${album.id}/photos`).then(setPhotos).catch((e) => setError(e.message));
  }, [album.id]);
  useEffect(load, [load]);

  async function upload(e: FormEvent) {
    e.preventDefault();
    const files = input.current?.files;
    if (!files || files.length === 0) return;
    const form = new FormData();
    Array.from(files).forEach((f) => form.append('files', f));
    setBusy(true);
    setError('');
    try {
      await api(`/api/admin/gallery/albums/${album.id}/photos`, { method: 'POST', form });
      if (input.current) input.current.value = '';
      load();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function remove(p: Photo) {
    if (!window.confirm('Delete this photo?')) return;
    try {
      await api(`/api/admin/gallery/albums/${album.id}/photos/${p.id}`, { method: 'DELETE' });
      load();
    } catch (err: any) {
      setError(err.message);
    }
  }

  async function caption(p: Photo, value: string) {
    if ((p.caption || '') === value) return;
    try {
      await api(`/api/admin/gallery/albums/${album.id}/photos/${p.id}`, { method: 'PUT', body: { caption: value } });
      setPhotos((list) => list!.map((x) => (x.id === p.id ? { ...x, caption: value } : x)));
    } catch (err: any) {
      setError(err.message);
    }
  }

  return (
    <Modal title={`Photos: ${album.title}`} onClose={onClose} wide>
      <Alert>{error}</Alert>
      <form onSubmit={upload} className="mb-5 flex flex-wrap items-end gap-3 rounded-xl bg-slate-50 p-4 dark:bg-navy-800/50">
        <div className="min-w-[220px] flex-1">
          <label htmlFor="photos" className="label">Choose photos (JPG or PNG, up to 30 at a time)</label>
          <input ref={input} id="photos" type="file" multiple accept="image/jpeg,image/png" required className="input !p-1.5" />
        </div>
        <button type="submit" className="btn-navy" disabled={busy}><Upload size={16} aria-hidden /> {busy ? 'Uploading...' : 'Upload'}</button>
      </form>
      {!photos ? <Spinner /> : photos.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-500">No photos in this album yet.</p>
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
          {photos.map((p) => (
            <li key={p.id} className="overflow-hidden rounded-xl border border-slate-200 dark:border-navy-700">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={p.url} alt={p.caption || ''} loading="lazy" className="aspect-[4/3] w-full object-cover" />
              <div className="flex items-center gap-2 p-2">
                <input defaultValue={p.caption || ''} placeholder="Caption (optional)" maxLength={250} aria-label="Caption" className="input !py-1.5 text-xs"
                  onBlur={(e) => caption(p, e.target.value.trim())} />
                <button type="button" onClick={() => remove(p)} className="rounded-lg p-2 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20" aria-label="Delete photo"><Trash2 size={16} /></button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Modal>
  );
}
