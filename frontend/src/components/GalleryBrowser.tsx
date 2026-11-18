'use client';

import Link from 'next/link';
import { useMemo, useState } from 'react';
import { Images } from 'lucide-react';

interface Album {
  id: number; title: string; category: string; coverUrl?: string; photoCount: number; eventDate?: string;
}

export default function GalleryBrowser({
  albums, categories, labels,
}: {
  albums: Album[];
  categories: { key: string; label: string }[];
  labels: { all: string; photos: string; empty: string };
}) {
  const [active, setActive] = useState('ALL');
  const present = useMemo(() => new Set(albums.map((a) => a.category)), [albums]);
  const visible = albums.filter((a) => active === 'ALL' || a.category === active);
  const chips = [{ key: 'ALL', label: labels.all }, ...categories.filter((c) => present.has(c.key))];

  return (
    <div>
      <div role="group" aria-label="Filter" className="flex flex-wrap gap-2">
        {chips.map((c) => (
          <button
            key={c.key}
            type="button"
            aria-pressed={active === c.key}
            onClick={() => setActive(c.key)}
            className={`rounded-full px-4 py-2 text-sm font-medium transition ${
              active === c.key
                ? 'bg-navy-800 text-white dark:bg-gold-500 dark:text-navy-900'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200 dark:bg-navy-800 dark:text-slate-200 dark:hover:bg-navy-700'
            }`}
          >
            {c.label}
          </button>
        ))}
      </div>

      {visible.length === 0 ? (
        <p className="mt-10 text-center text-slate-500">{labels.empty}</p>
      ) : (
        <ul className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {visible.map((a) => (
            <li key={a.id}>
              <Link href={`/gallery/${a.id}`} className="card group block overflow-hidden">
                <div className="relative aspect-[4/3] overflow-hidden bg-navy-900">
                  {a.coverUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={a.coverUrl} alt="" loading="lazy" className="h-full w-full object-cover transition duration-300 group-hover:scale-105" />
                  ) : (
                    <div className="flex h-full items-center justify-center text-gold-400"><Images size={44} aria-hidden /></div>
                  )}
                </div>
                <div className="p-4">
                  <h2 className="font-semibold">{a.title}</h2>
                  <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{a.photoCount} {labels.photos}</p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
