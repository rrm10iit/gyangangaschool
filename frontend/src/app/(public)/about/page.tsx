import type { Metadata } from 'next';
import { Eye, Target } from 'lucide-react';
import { T, getLang, getManagement, getSite, setting } from '@/lib/server';
import { pick } from '@/lib/i18n';
import { PageHero, Placeholder, SectionHeader } from '@/components/ui';

export async function generateMetadata(): Promise<Metadata> {
  const t = T(await getLang());
  return { title: t('about.title') };
}

export default async function AboutPage() {
  const lang = await getLang();
  const t = T(lang);
  const [site, management] = await Promise.all([getSite(), getManagement()]);
  const s = (k: string, f = '') => setting(site, lang, k, f);
  const principalPhoto = s('about.principal.photo');
  const founderStory = s('about.founder.story');
  const founderPhoto = s('about.founder.photo');
  const founderCaption = s('about.founder.caption');
  const founderParagraphs = founderStory.split(/\n\s*\n/).map((p) => p.trim()).filter(Boolean);

  return (
    <>
      <PageHero title={t('about.title')} sub={s('school.motto')} />

      <section className="section">
        <div className="container-page grid gap-10 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <SectionHeader title={t('about.story')} />
            <p className="mt-5 whitespace-pre-line text-base leading-relaxed">{s('about.history')}</p>
          </div>
          <aside className="card h-fit p-6" aria-label={t('about.facts')}>
            <h3 className="text-lg font-semibold">{t('about.facts')}</h3>
            <dl className="mt-4 space-y-3 text-sm">
              <div><dt className="text-slate-500 dark:text-slate-400">{t('about.board')}</dt><dd className="font-semibold">{s('school.board')}</dd></div>
              <div><dt className="text-slate-500 dark:text-slate-400">{t('about.medium')}</dt><dd className="font-semibold">{s('school.medium')}</dd></div>
              <div><dt className="text-slate-500 dark:text-slate-400">{t('about.classes')}</dt><dd className="font-semibold">{t('about.classesValue')}</dd></div>
              <div><dt className="text-slate-500 dark:text-slate-400">{t('home.stats.students')}</dt><dd className="font-semibold">{site.stats.students}</dd></div>
            </dl>
          </aside>
        </div>
      </section>

      {founderParagraphs.length > 0 && (
        <section className="bg-gold-50 py-14 dark:bg-navy-900 sm:py-20" aria-labelledby="founder-heading">
          <div className="container-page">
            <span className="kicker">{t('about.founder')}</span>
            <div className={`mt-4 grid items-start gap-10 ${founderPhoto ? 'lg:grid-cols-5' : ''}`}>
              {founderPhoto && (
                <figure className="lg:sticky lg:top-24 lg:col-span-2">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={founderPhoto}
                    alt={t('about.founder.photoAlt')}
                    loading="lazy"
                    className="aspect-[4/3] w-full rounded-2xl object-cover shadow-soft"
                  />
                  {founderCaption && (
                    <figcaption className="mt-3 text-sm text-slate-600 dark:text-slate-400">{founderCaption}</figcaption>
                  )}
                </figure>
              )}
              <article className={founderPhoto ? 'lg:col-span-3' : 'max-w-3xl'}>
                <h2 id="founder-heading" className="text-3xl font-bold leading-tight tracking-tight sm:text-4xl">
                  {s('about.founder.headline')}
                </h2>
                <p className="mt-4 border-y border-gold-300/60 py-2 text-sm font-semibold uppercase tracking-wider text-gold-800 dark:border-navy-700 dark:text-gold-300">
                  {s('about.founder.name')} &middot; {t('about.founder.role')}
                </p>
                {founderParagraphs.map((p, i) => (
                  <p
                    key={i}
                    className={i === 0 ? 'mt-6 text-lg font-medium leading-relaxed' : 'mt-4 text-base leading-relaxed'}
                  >
                    {p}
                  </p>
                ))}
              </article>
            </div>
          </div>
        </section>
      )}

      <section className="bg-slate-50 py-14 dark:bg-navy-900/50 sm:py-20">
        <div className="container-page grid gap-6 md:grid-cols-2">
          <div className="card p-8">
            <Eye className="text-gold-600" size={30} aria-hidden />
            <h2 className="mt-4 text-2xl font-bold">{t('about.vision')}</h2>
            <p className="mt-3 leading-relaxed">{s('about.vision')}</p>
          </div>
          <div className="card p-8">
            <Target className="text-gold-600" size={30} aria-hidden />
            <h2 className="mt-4 text-2xl font-bold">{t('about.mission')}</h2>
            <p className="mt-3 leading-relaxed">{s('about.mission')}</p>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container-page grid items-start gap-10 md:grid-cols-3">
          <div>
            {principalPhoto ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={principalPhoto} alt={s('about.principal.name')} loading="lazy" className="aspect-[4/5] w-full rounded-2xl object-cover shadow-soft" />
            ) : (
              <Placeholder className="aspect-[4/5] w-full rounded-2xl" />
            )}
            <p className="mt-4 text-lg font-semibold">{s('about.principal.name')}</p>
          </div>
          <div className="md:col-span-2">
            <SectionHeader title={t('about.principal')} />
            <blockquote className="mt-5 whitespace-pre-line border-l-4 border-gold-500 pl-5 text-base leading-relaxed">
              {s('about.principal.message')}
            </blockquote>
          </div>
        </div>
      </section>

      {management.length > 0 && (
        <section className="bg-slate-50 py-14 dark:bg-navy-900/50 sm:py-20">
          <div className="container-page">
            <SectionHeader title={t('about.management')} center />
            <ul className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {management.map((m) => (
                <li key={m.id} className="card overflow-hidden text-center">
                  {m.photoUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={m.photoUrl} alt={m.name} loading="lazy" className="aspect-square w-full object-cover" />
                  ) : (
                    <Placeholder className="aspect-square w-full" />
                  )}
                  <div className="p-4">
                    <p className="font-semibold text-navy-900 dark:text-white">{m.name}</p>
                    <p className="text-sm text-slate-500 dark:text-slate-400">{pick(lang, m.designation, m.designationHi)}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </section>
      )}
    </>
  );
}
