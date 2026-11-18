import type { MetadataRoute } from 'next';
import { getAlbums, publicUrl } from '@/lib/server';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const pages = ['', '/about', '/classes', '/facilities', '/gallery', '/news', '/contact'];
  const albums = await getAlbums();
  return [
    ...pages.map((p) => ({ url: `${publicUrl}${p}`, changeFrequency: 'weekly' as const, priority: p === '' ? 1 : 0.7 })),
    ...albums.map((a) => ({ url: `${publicUrl}/gallery/${a.id}`, changeFrequency: 'monthly' as const, priority: 0.5 })),
  ];
}
