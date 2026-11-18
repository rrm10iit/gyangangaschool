import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import { T, getAlbum, getLang } from '@/lib/server';
import { pick } from '@/lib/i18n';
import { Empty, PageHero } from '@/components/ui';
import PhotoGrid from '@/components/PhotoGrid';

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const lang = await getLang();
  const data = await getAlbum(id);
  return { title: data ? pick(lang, data.album.title, data.album.titleHi) : T(lang)('gallery.title') };
}

export default async function AlbumPage({ params }: Props) {
  const { id } = await params;
  const lang = await getLang();
  const t = T(lang);
  const data = await getAlbum(id);
  if (!data) notFound();
  const title = pick(lang, data.album.title, data.album.titleHi);
  return (
    <>
      <PageHero title={title} sub={`${data.photos.length} ${t('gallery.photos')}`} />
      <section className="section">
        <div className="container-page">
          <Link href="/gallery" className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-navy-700 hover:underline dark:text-gold-300">
            <ArrowLeft size={16} aria-hidden /> {t('gallery.back')}
          </Link>
          {data.photos.length === 0 ? (
            <Empty>{t('gallery.emptyAlbum')}</Empty>
          ) : (
            <PhotoGrid
              photos={data.photos}
              alt={title}
              labels={{ close: t('lightbox.close'), prev: t('lightbox.prev'), next: t('lightbox.next') }}
            />
          )}
        </div>
      </section>
    </>
  );
}
