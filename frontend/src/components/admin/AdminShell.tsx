'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ReactNode, useEffect, useState } from 'react';
import {
  BarChart3, BookOpen, Building2, CalendarClock, ClipboardList, Contact, FileText, GraduationCap, Images, KeyRound,
  LayoutDashboard, LogOut, Megaphone, Menu, MessageSquareQuote, Moon, Receipt, Settings, Sun, Users, UserCog, Wallet, X,
  Banknote, ScrollText, Landmark, Inbox,
} from 'lucide-react';
import { api } from '@/lib/api';
import { AuthContext, User, canSeeFinance, isAdmin } from './AuthContext';
import { Spinner } from './parts';

interface Item { href: string; label: string; icon: ReactNode; show?: (u: User) => boolean }
interface Group { title: string; items: Item[]; show?: (u: User) => boolean }

const groups: Group[] = [
  { title: 'Overview', items: [{ href: '/admin', label: 'Dashboard', icon: <LayoutDashboard size={18} /> }] },
  {
    title: 'School',
    items: [
      { href: '/admin/students', label: 'Students', icon: <GraduationCap size={18} /> },
      { href: '/admin/teachers', label: 'Teachers', icon: <Users size={18} /> },
      { href: '/admin/classes', label: 'Classes', icon: <BookOpen size={18} />, show: isAdmin },
    ],
  },
  {
    title: 'Finance',
    show: canSeeFinance,
    items: [
      { href: '/admin/finance/payments', label: 'Fee payments', icon: <Receipt size={18} /> },
      { href: '/admin/finance/dues', label: 'Pending & overdue', icon: <CalendarClock size={18} /> },
      { href: '/admin/finance/fee-structure', label: 'Fee structure', icon: <Landmark size={18} /> },
      { href: '/admin/finance/salaries', label: 'Teacher salaries', icon: <Banknote size={18} /> },
      { href: '/admin/finance/expenses', label: 'Expenses', icon: <Wallet size={18} /> },
      { href: '/admin/finance/reports', label: 'Reports', icon: <BarChart3 size={18} /> },
    ],
  },
  {
    title: 'Website',
    show: isAdmin,
    items: [
      { href: '/admin/content/settings', label: 'Page content', icon: <FileText size={18} /> },
      { href: '/admin/content/facilities', label: 'Facilities', icon: <Building2 size={18} /> },
      { href: '/admin/content/gallery', label: 'Photo gallery', icon: <Images size={18} /> },
      { href: '/admin/content/announcements', label: 'Announcements', icon: <Megaphone size={18} /> },
      { href: '/admin/content/testimonials', label: 'Testimonials', icon: <MessageSquareQuote size={18} /> },
      { href: '/admin/content/management', label: 'Management team', icon: <Contact size={18} /> },
      { href: '/admin/content/enquiries', label: 'Admission enquiries', icon: <Inbox size={18} /> },
    ],
  },
  {
    title: 'Administration',
    show: isAdmin,
    items: [
      { href: '/admin/users', label: 'Staff accounts', icon: <UserCog size={18} /> },
      { href: '/admin/activity', label: 'Activity log', icon: <ScrollText size={18} /> },
    ],
  },
  { title: 'Account', items: [{ href: '/admin/account', label: 'My account', icon: <KeyRound size={18} /> }] },
];

export default function AdminShell({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [open, setOpen] = useState(false);
  const [dark, setDark] = useState(false);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    api<User>('/api/auth/me').then(setUser).catch(() => undefined);
    setDark(document.documentElement.classList.contains('dark'));
  }, []);
  useEffect(() => setOpen(false), [pathname]);

  function toggleTheme() {
    const next = !dark;
    setDark(next);
    document.documentElement.classList.toggle('dark', next);
    try { localStorage.setItem('theme', next ? 'dark' : 'light'); } catch { /* ignore */ }
  }

  async function logout() {
    await api('/api/auth/logout', { method: 'POST' }).catch(() => undefined);
    router.replace('/admin/login');
  }

  if (!user) return <div className="flex min-h-screen items-center justify-center"><Spinner /></div>;

  const active = (href: string) => (href === '/admin' ? pathname === '/admin' : pathname.startsWith(href));

  const nav = (
    <nav aria-label="Admin" className="flex-1 space-y-5 overflow-y-auto px-3 py-4">
      {groups.filter((g) => !g.show || g.show(user)).map((g) => (
        <div key={g.title}>
          <p className="mb-1 px-3 text-[11px] font-semibold uppercase tracking-wider text-navy-300">{g.title}</p>
          <ul className="space-y-0.5">
            {g.items.filter((i) => !i.show || i.show(user)).map((i) => (
              <li key={i.href}>
                <Link href={i.href} aria-current={active(i.href) ? 'page' : undefined}
                  className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition ${
                    active(i.href) ? 'bg-gold-500 text-navy-900' : 'text-navy-100 hover:bg-white/10'
                  }`}>
                  {i.icon}{i.label}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );

  return (
    <AuthContext.Provider value={user}>
      <div className="min-h-screen bg-slate-50 dark:bg-navy-950">
        {/* desktop sidebar */}
        <aside className="no-print fixed inset-y-0 left-0 z-30 hidden w-64 flex-col bg-navy-900 lg:flex">
          <Brand />
          {nav}
        </aside>

        {/* mobile drawer */}
        {open && (
          <div className="no-print fixed inset-0 z-40 lg:hidden">
            <div className="absolute inset-0 bg-black/60" onClick={() => setOpen(false)} />
            <aside className="absolute inset-y-0 left-0 flex w-72 flex-col bg-navy-900">
              <Brand />
              {nav}
            </aside>
          </div>
        )}

        <div className="lg:pl-64">
          <header className="no-print sticky top-0 z-20 flex h-14 items-center justify-between border-b border-slate-200 bg-white/90 px-4 backdrop-blur dark:border-navy-800 dark:bg-navy-950/90">
            <button type="button" className="rounded-lg p-2 lg:hidden" onClick={() => setOpen(!open)} aria-label={open ? 'Close menu' : 'Open menu'} aria-expanded={open}>
              {open ? <X size={20} /> : <Menu size={20} />}
            </button>
            <Link href="/" target="_blank" className="hidden text-sm text-navy-700 hover:underline dark:text-gold-300 lg:block">View website</Link>
            <div className="flex items-center gap-3">
              <div className="hidden text-right text-sm leading-tight sm:block">
                <p className="font-semibold">{user.fullName}</p>
                <p className="text-xs text-slate-500">{user.role.charAt(0) + user.role.slice(1).toLowerCase()}</p>
              </div>
              <button type="button" onClick={toggleTheme} aria-label="Switch light or dark mode" className="rounded-lg border border-slate-200 p-2 dark:border-navy-700">
                {dark ? <Sun size={16} /> : <Moon size={16} />}
              </button>
              <button type="button" onClick={logout} className="btn-outline !px-3 !py-2"><LogOut size={16} aria-hidden /> <span className="hidden sm:inline">Sign out</span></button>
            </div>
          </header>
          <main className="p-4 sm:p-6 lg:p-8">{children}</main>
        </div>
      </div>
    </AuthContext.Provider>
  );
}

function Brand() {
  return (
    <div className="flex h-14 shrink-0 items-center gap-3 border-b border-white/10 px-5">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src="/images/logo-192.png" alt="" width={32} height={32} className="h-8 w-8 rounded-full" />
      <span className="font-bold text-white">Gyan Ganga Admin</span>
    </div>
  );
}
