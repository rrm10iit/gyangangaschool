import type { Metadata } from 'next';
import { Users } from 'lucide-react';
import { T, getClasses, getLang } from '@/lib/server';
import { pick } from '@/lib/i18n';
import { Empty, PageHero } from '@/components/ui';

export async function generateMetadata(): Promise<Metadata> {
  const t = T(await getLang());
  return { title: t('classes.title') };
}

export default async function ClassesPage() {
  const lang = await getLang();
  const t = T(lang);
  const classes = await getClasses();
  return (
    <>
      <PageHero title={t('classes.title')} sub={t('classes.sub')} />
      <section className="section">
        <div className="container-page">
          {classes.length === 0 && <Empty>{t('common.none')}</Empty>}
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {classes.map((c) => (
              <article key={c.id} className="card flex flex-col p-6">
                <div className="flex items-start justify-between gap-3">
                  <h2 className="text-xl font-bold">{c.name}</h2>
                  {c.ageGroup && <span className="rounded-full bg-navy-50 px-3 py-1 text-xs font-semibold text-navy-700 dark:bg-navy-800 dark:text-navy-100">{c.ageGroup}</span>}
                </div>
                <p className="mt-3 text-sm leading-relaxed text-slate-600 dark:text-slate-400">{pick(lang, c.description, c.descriptionHi)}</p>
                {c.subjects && (
                  <div className="mt-4">
                    <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500">{t('classes.subjects')}</h3>
                    <ul className="mt-2 flex flex-wrap gap-2">
                      {c.subjects.split(',').map((x) => (
                        <li key={x} className="rounded-md bg-gold-50 px-2 py-1 text-xs text-gold-900 dark:bg-gold-500/10 dark:text-gold-200">{x.trim()}</li>
                      ))}
                    </ul>
                  </div>
                )}
                <div className="mt-auto flex items-center gap-4 pt-5 text-sm text-slate-500 dark:text-slate-400">
                  {c.seats != null && <span className="inline-flex items-center gap-1.5"><Users size={16} aria-hidden /> {t('classes.seats')}: <b className="text-navy-900 dark:text-white">{c.seats}</b></span>}
                  <span>{t('classes.sections')}: <b className="text-navy-900 dark:text-white">{c.sections}</b></span>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
