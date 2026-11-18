'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { Menu, Moon, Sun, X } from 'lucide-react';
import { LANG_COOKIE, Lang } from '@/lib/i18n';

interface Props {
  lang: Lang;
  name: string;
  links: { href: string; label: string }[];
  labels: { menu: string; close: string; theme: string; langToggle: string; langLabel: string; enquire: string };
}

export default function Navbar({ lang, name, links, labels }: Props) {
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [dark, setDark] = useState(false);

  useEffect(() => setDark(document.documentElement.classList.contains('dark')), []);
  useEffect(() => setOpen(false), [pathname]);
  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : '';
    return () => { document.body.style.overflow = ''; };
  }, [open]);

  function toggleTheme() {
    const next = !dark;
    setDark(next);
    document.documentElement.classList.toggle('dark', next);
    try { localStorage.setItem('theme', next ? 'dark' : 'light'); } catch { /* ignore */ }
  }

  function toggleLang() {
    const next: Lang = lang === 'en' ? 'hi' : 'en';
    document.cookie = `${LANG_COOKIE}=${next}; path=/; max-age=31536000; samesite=lax`;
    router.refresh();
  }

  const isActive = (href: string) => (href === '/' ? pathname === '/' : pathname.startsWith(href));

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200/70 bg-white/90 backdrop-blur dark:border-navy-800 dark:bg-navy-950/90">
      <div className="container-page flex h-16 items-center justify-between gap-4">
        <Link href="/" className="flex items-center gap-3" aria-label={name}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/images/logo-192.png" alt="" width={40} height={40} className="h-10 w-10 rounded-full" />
          <span className="text-base font-bold leading-tight text-navy-900 dark:text-white sm:text-lg">{name}</span>
        </Link>

        <nav aria-label="Main" className="hidden items-center gap-1 lg:flex">
          {links.map((l) => (
            <Link
              key={l.href}
              href={l.href}
              aria-current={isActive(l.href) ? 'page' : undefined}
              className={`rounded-lg px-3 py-2 text-sm font-medium transition ${
                isActive(l.href)
                  ? 'bg-navy-50 text-navy-900 dark:bg-navy-800 dark:text-gold-300'
                  : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-navy-800'
              }`}
            >
              {l.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={toggleLang}
            aria-label={labels.langLabel}
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-navy-800 hover:bg-slate-100 dark:border-navy-700 dark:text-white dark:hover:bg-navy-800"
          >
            {labels.langToggle}
          </button>
          <button
            type="button"
            onClick={toggleTheme}
            aria-label={labels.theme}
            className="rounded-lg border border-slate-200 p-2 text-navy-800 hover:bg-slate-100 dark:border-navy-700 dark:text-white dark:hover:bg-navy-800"
          >
            {dark ? <Sun size={18} /> : <Moon size={18} />}
          </button>
          <Link href="/contact#enquiry" className="btn-primary hidden !py-2 xl:inline-flex">
            {labels.enquire}
          </Link>
          <button
            type="button"
            className="rounded-lg border border-slate-200 p-2 text-navy-800 dark:border-navy-700 dark:text-white lg:hidden"
            onClick={() => setOpen(!open)}
            aria-expanded={open}
            aria-controls="mobile-menu"
            aria-label={open ? labels.close : labels.menu}
          >
            {open ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </div>

      {open && (
        <nav id="mobile-menu" aria-label="Mobile" className="border-t border-slate-200 bg-white px-4 pb-6 pt-2 dark:border-navy-800 dark:bg-navy-950 lg:hidden">
          <ul className="space-y-1">
            {links.map((l) => (
              <li key={l.href}>
                <Link
                  href={l.href}
                  aria-current={isActive(l.href) ? 'page' : undefined}
                  className={`block rounded-lg px-3 py-3 text-base font-medium ${
                    isActive(l.href) ? 'bg-navy-50 text-navy-900 dark:bg-navy-800 dark:text-gold-300' : 'text-slate-700 dark:text-slate-200'
                  }`}
                >
                  {l.label}
                </Link>
              </li>
            ))}
          </ul>
          <Link href="/contact#enquiry" className="btn-primary mt-4 w-full">{labels.enquire}</Link>
        </nav>
      )}
    </header>
  );
}
