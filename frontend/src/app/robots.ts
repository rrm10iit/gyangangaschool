import type { MetadataRoute } from 'next';
import { publicUrl } from '@/lib/server';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [{ userAgent: '*', allow: '/', disallow: ['/admin', '/api'] }],
    sitemap: `${publicUrl}/sitemap.xml`,
  };
}
