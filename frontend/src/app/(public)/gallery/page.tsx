import type { Metadata } from 'next';
import { T, getAlbums, getLang } from '@/lib/server';
import { pick } from '@/lib/i18n';
import { PageHero } from '@/components/ui';
import GalleryBrowser from '@/components/GalleryBrowser';

export async function generateMetadata(): Promise<Metadata> {
  const t = T(await getLang());
  return { title: t('gallery.title') };
}

export default async function GalleryPage() {
  const lang = await getLang();
  const t = T(lang);
  const albums = (await getAlbums()).map((a) => ({ ...a, title: pick(lang, a.title, a.titleHi) }));
  const categories = ['EVENTS', 'SPORTS', 'ANNUAL_DAY', 'OTHER'].map((k) => ({ key: k, label: t(`gallery.${k}`) }));
  return (
    <>
      <PageHero title={t('gallery.title')} sub={t('gallery.sub')} />
      <section className="section">
        <div className="container-page">
          <GalleryBrowser
            albums={albums}
            categories={categories}
            labels={{ all: t('gallery.all'), photos: t('gallery.photos'), empty: t('gallery.empty') }}
          />
        </div>
      </section>
    </>
  );
}
