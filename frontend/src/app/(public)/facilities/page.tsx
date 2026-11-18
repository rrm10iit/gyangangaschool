import type { Metadata } from 'next';
import { T, getFacilities, getLang } from '@/lib/server';
import { pick } from '@/lib/i18n';
import { Empty, FacilityIcon, PageHero, Placeholder } from '@/components/ui';

export async function generateMetadata(): Promise<Metadata> {
  const t = T(await getLang());
  return { title: t('fac.title') };
}

export default async function FacilitiesPage() {
  const lang = await getLang();
  const t = T(lang);
  const facilities = await getFacilities();
  return (
    <>
      <PageHero title={t('fac.title')} sub={t('fac.sub')} />
      <section className="section">
        <div className="container-page space-y-14">
          {facilities.length === 0 && <Empty>{t('common.none')}</Empty>}
          {facilities.map((f, i) => (
            <article key={f.id} className="grid items-center gap-8 md:grid-cols-2">
              <div className={i % 2 === 1 ? 'md:order-2' : ''}>
                {f.imageUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={f.imageUrl} alt={pick(lang, f.title, f.titleHi)} loading={i < 2 ? 'eager' : 'lazy'} className="aspect-[4/3] w-full rounded-2xl object-cover shadow-soft" />
                ) : (
                  <Placeholder icon={f.icon} className="aspect-[4/3] w-full rounded-2xl" />
                )}
              </div>
              <div>
                <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-gold-100 text-gold-700 dark:bg-gold-500/15 dark:text-gold-300">
                  <FacilityIcon name={f.icon} size={24} />
                </span>
                <h2 className="mt-4 text-2xl font-bold">{pick(lang, f.title, f.titleHi)}</h2>
                <p className="mt-3 whitespace-pre-line leading-relaxed text-slate-600 dark:text-slate-400">{pick(lang, f.description, f.descriptionHi)}</p>
              </div>
            </article>
          ))}
        </div>
      </section>
    </>
  );
}
