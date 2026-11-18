import type { Metadata, Viewport } from 'next';
import './globals.css';
import { getLang, getSite, publicUrl, setting } from '@/lib/server';

export const viewport: Viewport = {
  themeColor: '#0b1a42',
  width: 'device-width',
  initialScale: 1,
};

export async function generateMetadata(): Promise<Metadata> {
  const lang = await getLang();
  const site = await getSite();
  const name = setting(site, lang, 'school.name', 'Gyan Ganga School');
  const desc = setting(site, lang, 'home.hero.subtitle', 'Gyan Ganga School, Prayagraj. Nursery to Class 8.');
  return {
    metadataBase: new URL(publicUrl),
    title: { default: `${name}, Prayagraj`, template: `%s | ${name}` },
    description: desc,
    openGraph: { title: name, description: desc, type: 'website', siteName: name, images: ['/images/logo-512.png'] },
    icons: { icon: '/images/logo-192.png', apple: '/images/logo-192.png' },
  };
}

const themeScript = `try{var t=localStorage.getItem('theme');if(t==='dark'||(!t&&window.matchMedia('(prefers-color-scheme: dark)').matches)){document.documentElement.classList.add('dark')}}catch(e){}`;

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const lang = await getLang();
  return (
    <html lang={lang} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body>{children}</body>
    </html>
  );
}
