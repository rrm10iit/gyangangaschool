import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import { T, getLang, getSite, setting } from '@/lib/server';

export default async function PublicLayout({ children }: { children: React.ReactNode }) {
  const lang = await getLang();
  const site = await getSite();
  const t = T(lang);
  const links = [
    ['/', 'nav.home'], ['/about', 'nav.about'], ['/classes', 'nav.classes'], ['/facilities', 'nav.facilities'],
    ['/gallery', 'nav.gallery'], ['/news', 'nav.news'], ['/contact', 'nav.contact'],
  ].map(([href, key]) => ({ href, label: t(key) }));

  return (
    <>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-gold-500 focus:px-4 focus:py-2 focus:font-semibold focus:text-navy-900"
      >
        {t('skip')}
      </a>
      <Navbar
        lang={lang}
        name={setting(site, lang, 'school.name', 'Gyan Ganga School')}
        links={links}
        labels={{
          menu: t('nav.menu'), close: t('nav.close'), theme: t('theme.toggle'), langToggle: t('lang.toggle'),
          langLabel: t('lang.label'), enquire: t('cta.enquire'),
        }}
      />
      <main id="main">{children}</main>
      <Footer lang={lang} site={site} />
    </>
  );
}
