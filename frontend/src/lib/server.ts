import 'server-only';
import { cookies } from 'next/headers';
import { LANG_COOKIE, Lang, pick, tr } from './i18n';
import type { AlbumDetail, AlbumView, Announcement, ClassItem, Facility, Management, SiteData, Testimonial } from './types';

const BACKEND = process.env.BACKEND_URL || 'http://localhost:8081';

export async function getLang(): Promise<Lang> {
  const c = (await cookies()).get(LANG_COOKIE)?.value;
  return c === 'hi' ? 'hi' : 'en';
}

async function get<T>(path: string, fallback: T): Promise<T> {
  try {
    // Not cached, so edits made in the admin panel show up on the very next page view.
    const res = await fetch(`${BACKEND}${path}`, { cache: 'no-store' });
    if (!res.ok) return fallback;
    return (await res.json()) as T;
  } catch {
    return fallback;
  }
}

const emptySite: SiteData = { settings: {}, stats: { students: 0, teachers: 0, classes: 0, years: 0 } };

export const getSite = () => get<SiteData>('/api/public/site', emptySite);
export const getClasses = () => get<ClassItem[]>('/api/public/classes', []);
export const getFacilities = () => get<Facility[]>('/api/public/facilities', []);
export const getAnnouncements = () => get<Announcement[]>('/api/public/announcements', []);
export const getTestimonials = () => get<Testimonial[]>('/api/public/testimonials', []);
export const getManagement = () => get<Management[]>('/api/public/management', []);
export const getAlbums = () => get<AlbumView[]>('/api/public/albums', []);
export const getAlbum = (id: string) => get<AlbumDetail | null>(`/api/public/albums/${encodeURIComponent(id)}`, null);

/** Convenience accessor for a localised site setting. */
export function setting(site: SiteData, lang: Lang, key: string, fallback = ''): string {
  const v = site.settings[key];
  if (!v) return fallback;
  return pick(lang, v.en, v.hi) || fallback;
}

export function T(lang: Lang) {
  return (key: string, vars?: Record<string, string>) => tr(lang, key, vars);
}

export const publicUrl = (process.env.PUBLIC_URL || 'http://localhost:3000').replace(/\/$/, '');
