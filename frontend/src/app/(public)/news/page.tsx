import type { Metadata } from 'next';
import { T, getAnnouncements, getLang } from '@/lib/server';
import { pick } from '@/lib/i18n';
import { Empty, PageHero, formatDate } from '@/components/ui';

export async function generateMetadata(): Promise<Metadata> {
  const t = T(await getLang());
  return { title: t('news.title') };
}

export default async function NewsPage() {
  const lang = await getLang();
  const t = T(lang);
  const news = await getAnnouncements();
  return (
    <>
      <PageHero title={t('news.title')} />
      <section className="section">
        <div className="container-page max-w-3xl space-y-5">
          {news.length === 0 && <Empty>{t('news.empty')}</Empty>}
          {news.map((n) => (
            <article key={n.id} className="card p-6">
              <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                <time dateTime={n.publishDate}>{formatDate(n.publishDate, lang)}</time>
                {n.pinned && <span className="rounded-full bg-gold-100 px-2 py-0.5 font-semibold text-gold-800 dark:bg-gold-500/20 dark:text-gold-300">{t('news.pinned')}</span>}
              </div>
              <h2 className="mt-3 text-xl font-semibold">{pick(lang, n.title, n.titleHi)}</h2>
              <p className="mt-2 whitespace-pre-line text-slate-600 dark:text-slate-400">{pick(lang, n.body, n.bodyHi)}</p>
            </article>
          ))}
        </div>
      </section>
    </>
  );
}
