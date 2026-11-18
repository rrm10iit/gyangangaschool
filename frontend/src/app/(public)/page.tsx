import Link from 'next/link';
import { ArrowRight, BookOpenText, GraduationCap, Languages, Palette, Phone, Trophy, Users2, Wallet, Music2, Quote } from 'lucide-react';
import {
  T, getAnnouncements, getFacilities, getLang, getSite, getTestimonials, publicUrl, setting,
} from '@/lib/server';
import { pick } from '@/lib/i18n';
import { Empty, FacilityIcon, Placeholder, SectionHeader, formatDate } from '@/components/ui';

export default async function HomePage() {
  const lang = await getLang();
  const t = T(lang);
  const [site, news, facilities, testimonials] = await Promise.all([
    getSite(), getAnnouncements(), getFacilities(), getTestimonials(),
  ]);
  const s = (k: string, f = '') => setting(site, lang, k, f);
  const name = s('school.name', 'Gyan Ganga School');
  const phone = s('school.phone');
  const board = s('school.board', 'UP Board');
  const heroImage = s('home.hero.image');

  const why = [
    { icon: BookOpenText, k: 1 }, { icon: Wallet, k: 2 }, { icon: Trophy, k: 3 }, { icon: Palette, k: 4 }, { icon: Users2, k: 5 },
  ];
  const curr = [
    { icon: Languages, k: 1 }, { icon: GraduationCap, k: 2 }, { icon: Users2, k: 3 }, { icon: Music2, k: 4 },
  ];
  const stats = [
    { label: t('home.stats.students'), value: site.stats.students },
    { label: t('home.stats.teachers'), value: site.stats.teachers },
    { label: t('home.stats.years'), value: site.stats.years },
    { label: t('home.stats.classes'), value: site.stats.classes },
  ];

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'School',
    name,
    url: publicUrl,
    logo: `${publicUrl}/images/logo-512.png`,
    telephone: phone,
    email: s('school.email'),
    address: { '@type': 'PostalAddress', streetAddress: s('school.address'), addressLocality: 'Prayagraj', addressRegion: 'Uttar Pradesh', addressCountry: 'IN' },
  };

  return (
    <>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />

      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-br from-navy-950 via-navy-900 to-navy-700 text-white">
        <div className="absolute -right-24 -top-24 h-96 w-96 rounded-full bg-gold-500/10 blur-3xl" aria-hidden />
        <div className="container-page relative grid items-center gap-10 py-14 sm:py-20 lg:grid-cols-2">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1.5 text-sm font-medium text-gold-300">
              {s('school.motto')}
            </span>
            <h1 className="mt-5 text-4xl font-extrabold leading-tight tracking-tight !text-white sm:text-5xl lg:text-6xl">
              {s('home.hero.title')}
            </h1>
            <p className="mt-5 max-w-xl text-lg text-navy-100">{s('home.hero.subtitle')}</p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link href="/contact#enquiry" className="btn-primary">{t('cta.enquire')} <ArrowRight size={16} aria-hidden /></Link>
              <Link href="/classes" className="btn border border-white/30 text-white hover:bg-white/10">{t('cta.explore')}</Link>
            </div>
            <p className="mt-8 text-sm text-navy-200">{board} &middot; {s('school.medium')}</p>
          </div>
          <div className="relative mx-auto w-full max-w-md lg:max-w-none">
            <div className="absolute -bottom-4 -left-4 h-full w-full rounded-3xl bg-gold-500" aria-hidden />
            {heroImage ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={heroImage} alt={name} className="relative aspect-[4/3] w-full rounded-3xl object-cover shadow-2xl" fetchPriority="high" />
            ) : (
              <Placeholder className="relative aspect-[4/3] w-full rounded-3xl" />
            )}
          </div>
        </div>
      </section>

      {/* Stats */}
      <section aria-label="School in numbers" className="relative z-10 -mt-8 sm:-mt-10">
        <div className="container-page">
          <dl className="card grid grid-cols-2 gap-6 p-6 sm:p-8 lg:grid-cols-4">
            {stats.map((x) => (
              <div key={x.label} className="text-center">
                <dd className="text-3xl font-extrabold text-navy-800 dark:text-gold-400 sm:text-4xl">{x.value}{x.value > 0 && x.label === t('home.stats.years') ? '+' : ''}</dd>
                <dt className="mt-1 text-sm text-slate-500 dark:text-slate-400">{x.label}</dt>
              </div>
            ))}
          </dl>
        </div>
      </section>

      {/* Intro + why */}
      <section className="section">
        <div className="container-page">
          <SectionHeader kicker={t('home.why.kicker')} title={s('home.hero.title')} sub={s('home.intro')} center />
          <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {why.map(({ icon: Icon, k }) => (
              <div key={k} className="card p-6 transition hover:-translate-y-1">
                <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-gold-100 text-gold-700 dark:bg-gold-500/15 dark:text-gold-300">
                  <Icon size={24} aria-hidden />
                </span>
                <h3 className="mt-4 text-lg font-semibold">{t(`home.why.${k}.t`)}</h3>
                <p className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-slate-400">{t(`home.why.${k}.d`)}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Curriculum */}
      <section className="bg-slate-50 py-14 dark:bg-navy-900/50 sm:py-20">
        <div className="container-page">
          <SectionHeader title={t('home.curr.title')} />
          <div className="mt-10 grid gap-6 md:grid-cols-2">
            {curr.map(({ icon: Icon, k }) => (
              <div key={k} className="flex gap-4">
                <span className="mt-1 flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-navy-800 text-gold-400">
                  <Icon size={20} aria-hidden />
                </span>
                <div>
                  <h3 className="font-semibold">{t(`home.curr.${k}.t`)}</h3>
                  <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{t(`home.curr.${k}.d`, { board })}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* News */}
      <section className="section">
        <div className="container-page">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <SectionHeader title={t('home.news.title')} />
            <Link href="/news" className="btn-outline">{t('cta.viewAll')}</Link>
          </div>
          <div className="mt-8 grid gap-5 md:grid-cols-3">
            {news.length === 0 && <div className="md:col-span-3"><Empty>{t('news.empty')}</Empty></div>}
            {news.slice(0, 3).map((n) => (
              <article key={n.id} className="card p-6">
                <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                  <time dateTime={n.publishDate}>{formatDate(n.publishDate, lang)}</time>
                  {n.pinned && <span className="rounded-full bg-gold-100 px-2 py-0.5 font-semibold text-gold-800 dark:bg-gold-500/20 dark:text-gold-300">{t('news.pinned')}</span>}
                </div>
                <h3 className="mt-3 text-lg font-semibold">{pick(lang, n.title, n.titleHi)}</h3>
                <p className="mt-2 line-clamp-4 text-sm text-slate-600 dark:text-slate-400">{pick(lang, n.body, n.bodyHi)}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* Facilities */}
      <section className="bg-slate-50 py-14 dark:bg-navy-900/50 sm:py-20">
        <div className="container-page">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <SectionHeader title={t('home.fac.title')} sub={t('home.fac.sub')} />
            <Link href="/facilities" className="btn-outline">{t('cta.viewAll')}</Link>
          </div>
          <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {facilities.slice(0, 6).map((f) => (
              <div key={f.id} className="card overflow-hidden">
                {f.imageUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={f.imageUrl} alt={pick(lang, f.title, f.titleHi)} loading="lazy" className="h-44 w-full object-cover" />
                ) : (
                  <Placeholder icon={f.icon} className="h-44 w-full" />
                )}
                <div className="flex items-start gap-3 p-5">
                  <FacilityIcon name={f.icon} size={22} className="mt-0.5 shrink-0 text-gold-600" />
                  <h3 className="font-semibold">{pick(lang, f.title, f.titleHi)}</h3>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Campus life */}
      <section className="section">
        <div className="container-page">
          <SectionHeader title={t('home.campus.title')} center />
          <div className="mt-10 grid gap-6 md:grid-cols-2">
            {['/videos/campus-1.mp4', '/videos/campus-2.mp4'].map((src, i) => (
              <video
                key={src}
                controls
                muted
                playsInline
                preload="none"
                aria-label={`${t('home.campus.title')} ${i + 1}`}
                className="aspect-video w-full rounded-2xl bg-navy-900 shadow-soft"
              >
                <source src={src} type="video/mp4" />
              </video>
            ))}
          </div>
        </div>
      </section>

      {/* Testimonials */}
      {testimonials.length > 0 && (
        <section className="bg-slate-50 py-14 dark:bg-navy-900/50 sm:py-20">
          <div className="container-page">
            <SectionHeader title={t('home.testi.title')} center />
            <ul className="mt-10 flex snap-x snap-mandatory gap-5 overflow-x-auto pb-4" tabIndex={0} aria-label={t('home.testi.title')}>
              {testimonials.map((x) => (
                <li key={x.id} className="card w-[85%] shrink-0 snap-start p-6 sm:w-[420px]">
                  <Quote className="text-gold-500" size={28} aria-hidden />
                  <blockquote className="mt-3 text-sm leading-relaxed">{pick(lang, x.quote, x.quoteHi)}</blockquote>
                  <div className="mt-5 flex items-center gap-3">
                    {x.photoUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={x.photoUrl} alt="" loading="lazy" className="h-12 w-12 rounded-full object-cover" />
                    ) : (
                      <Placeholder className="h-12 w-12 rounded-full [&_svg]:h-5 [&_svg]:w-5" />
                    )}
                    <div>
                      <p className="font-semibold text-navy-900 dark:text-white">{x.name}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">{x.role}</p>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </section>
      )}

      {/* Admission CTA */}
      <section className="section">
        <div className="container-page">
          <div className="overflow-hidden rounded-3xl bg-gradient-to-r from-navy-800 to-navy-600 p-8 text-white sm:p-12">
            <h2 className="text-3xl font-bold !text-white">{t('home.admission.title')}</h2>
            <p className="mt-3 max-w-2xl text-navy-100">{s('admission.cta')}</p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link href="/contact#enquiry" className="btn-primary">{t('cta.enquire')} <ArrowRight size={16} aria-hidden /></Link>
              {phone && (
                <a href={`tel:${phone.replace(/[^+\d]/g, '')}`} className="btn border border-white/30 text-white hover:bg-white/10">
                  <Phone size={16} aria-hidden /> {phone}
                </a>
              )}
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
