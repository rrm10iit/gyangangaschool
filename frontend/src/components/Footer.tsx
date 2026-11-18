import Link from 'next/link';
import { MapPin, Phone, Mail } from 'lucide-react';
import { Lang } from '@/lib/i18n';
import { SiteData } from '@/lib/types';
import { T, setting } from '@/lib/server';

export default function Footer({ lang, site }: { lang: Lang; site: SiteData }) {
  const t = T(lang);
  const name = setting(site, lang, 'school.name', 'Gyan Ganga School');
  const phone = setting(site, lang, 'school.phone');
  const email = setting(site, lang, 'school.email');
  const address = setting(site, lang, 'school.address');
  const motto = setting(site, lang, 'school.motto');
  const links = [
    ['/', 'nav.home'], ['/about', 'nav.about'], ['/classes', 'nav.classes'], ['/facilities', 'nav.facilities'],
    ['/gallery', 'nav.gallery'], ['/news', 'nav.news'], ['/contact', 'nav.contact'],
  ];
  return (
    <footer className="mt-10 bg-navy-900 text-slate-300">
      <div className="container-page grid gap-10 py-12 md:grid-cols-3">
        <div>
          <div className="flex items-center gap-3">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/images/logo-192.png" alt="" width={48} height={48} className="h-12 w-12 rounded-full" loading="lazy" />
            <span className="text-lg font-bold text-white">{name}</span>
          </div>
          {motto && <p className="mt-4 text-sm text-gold-300">{motto}</p>}
        </div>
        <nav aria-label="Footer">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider !text-white">{t('footer.links')}</h2>
          <ul className="grid grid-cols-2 gap-2 text-sm">
            {links.map(([href, key]) => (
              <li key={href}><Link href={href} className="hover:text-gold-300">{t(key)}</Link></li>
            ))}
          </ul>
        </nav>
        <div>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider !text-white">{t('footer.contact')}</h2>
          <ul className="space-y-3 text-sm">
            {address && <li className="flex gap-2"><MapPin size={16} className="mt-0.5 shrink-0 text-gold-400" aria-hidden />{address}</li>}
            {phone && <li className="flex gap-2"><Phone size={16} className="mt-0.5 shrink-0 text-gold-400" aria-hidden /><a href={`tel:${phone.replace(/[^+\d]/g, '')}`} className="hover:text-gold-300">{phone}</a></li>}
            {email && <li className="flex gap-2"><Mail size={16} className="mt-0.5 shrink-0 text-gold-400" aria-hidden /><a href={`mailto:${email}`} className="hover:text-gold-300">{email}</a></li>}
          </ul>
        </div>
      </div>
      <div className="border-t border-white/10">
        <div className="container-page flex flex-col items-center justify-between gap-2 py-5 text-xs text-slate-400 sm:flex-row">
          <p>&copy; {new Date().getFullYear()} {name}. {t('footer.rights')}</p>
          <Link href="/admin/login" className="hover:text-gold-300">{t('footer.staff')}</Link>
        </div>
      </div>
    </footer>
  );
}
