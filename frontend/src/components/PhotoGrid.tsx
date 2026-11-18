'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, X } from 'lucide-react';

interface Photo { id: number; url: string; caption?: string }
interface Labels { close: string; prev: string; next: string }

export default function PhotoGrid({ photos, labels, alt }: { photos: Photo[]; labels: Labels; alt: string }) {
  const [index, setIndex] = useState<number | null>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  const openerRef = useRef<HTMLElement | null>(null);

  const close = useCallback(() => {
    setIndex(null);
    openerRef.current?.focus();
  }, []);
  const move = useCallback(
    (d: number) => setIndex((i) => (i === null ? i : (i + d + photos.length) % photos.length)),
    [photos.length],
  );

  useEffect(() => {
    if (index === null) return;
    closeRef.current?.focus();
    document.body.style.overflow = 'hidden';
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close();
      if (e.key === 'ArrowRight') move(1);
      if (e.key === 'ArrowLeft') move(-1);
    };
    window.addEventListener('keydown', onKey);
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [index, close, move]);

  const current = index === null ? null : photos[index];

  return (
    <>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {photos.map((p, i) => (
          <li key={p.id}>
            <button
              type="button"
              onClick={(e) => { openerRef.current = e.currentTarget; setIndex(i); }}
              className="group block w-full overflow-hidden rounded-xl bg-navy-900"
              aria-label={`${alt} ${i + 1}`}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={p.url} alt={p.caption || `${alt} ${i + 1}`} loading="lazy" className="aspect-square w-full object-cover transition duration-300 group-hover:scale-105" />
            </button>
          </li>
        ))}
      </ul>

      {current && (
        <div role="dialog" aria-modal="true" aria-label={alt} className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4">
          <button ref={closeRef} type="button" onClick={close} aria-label={labels.close} className="absolute right-4 top-4 rounded-full bg-white/10 p-2 text-white hover:bg-white/20">
            <X size={24} />
          </button>
          {photos.length > 1 && (
            <>
              <button type="button" onClick={() => move(-1)} aria-label={labels.prev} className="absolute left-3 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-3 text-white hover:bg-white/20">
                <ChevronLeft size={28} />
              </button>
              <button type="button" onClick={() => move(1)} aria-label={labels.next} className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-3 text-white hover:bg-white/20">
                <ChevronRight size={28} />
              </button>
            </>
          )}
          <figure className="flex max-h-full max-w-5xl flex-col items-center">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={current.url} alt={current.caption || `${alt} ${index! + 1}`} className="max-h-[80vh] w-auto rounded-lg object-contain" />
            <figcaption className="mt-3 text-center text-sm text-white/80">
              {current.caption ? `${current.caption} · ` : ''}{index! + 1} / {photos.length}
            </figcaption>
          </figure>
        </div>
      )}
    </>
  );
}
