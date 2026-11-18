import type { Metadata } from 'next';
import { Clock, Mail, MapPin, Phone } from 'lucide-react';
import { T, getClasses, getLang, getSite, setting } from '@/lib/server';
import { PageHero } from '@/components/ui';
import EnquiryForm from '@/components/EnquiryForm';

export async function generateMetadata(): Promise<Metadata> {
  const t = T(await getLang());
  return { title: t('contact.title') };
}

export default async function ContactPage() {
  const lang = await getLang();
  const t = T(lang);
  const [site, classes] = await Promise.all([getSite(), getClasses()]);
  const s = (k: string) => setting(site, lang, k);
  const phone = s('school.phone');
  const email = s('school.email');
  const map = s('school.map');

  const info = [
    { icon: MapPin, label: t('contact.address'), value: s('school.address'), href: undefined as string | undefined },
    { icon: Phone, label: t('contact.phone'), value: phone, href: phone ? `tel:${phone.replace(/[^+\d]/g, '')}` : undefined },
    { icon: Mail, label: t('contact.email'), value: email, href: email ? `mailto:${email}` : undefined },
    { icon: Clock, label: t('contact.hours'), value: s('school.hours'), href: undefined },
  ].filter((x) => x.value);

  return (
    <>
      <PageHero title={t('contact.title')} sub={t('contact.sub')} />
      <section className="section">
        <div className="container-page grid gap-10 lg:grid-cols-2">
          <div>
            <ul className="space-y-5">
              {info.map(({ icon: Icon, label, value, href }) => (
                <li key={label} className="flex gap-4">
                  <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-gold-100 text-gold-700 dark:bg-gold-500/15 dark:text-gold-300">
                    <Icon size={22} aria-hidden />
                  </span>
                  <div>
                    <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">{label}</h2>
                    {href ? <a href={href} className="text-base font-medium hover:underline">{value}</a> : <p className="text-base">{value}</p>}
                  </div>
                </li>
              ))}
            </ul>
            {map && (
              <iframe
                title={t('contact.map')}
                src={map}
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
                className="mt-8 h-72 w-full rounded-2xl border-0 shadow-soft"
                allowFullScreen
              />
            )}
          </div>
          <div id="enquiry" className="card scroll-mt-24 p-6 sm:p-8">
            <h2 className="mb-6 text-2xl font-bold">{t('contact.form.title')}</h2>
            <EnquiryForm
              classes={classes.map((c) => c.name)}
              labels={{
                name: t('contact.form.name'), phone: t('contact.form.phone'), email: t('contact.form.email'),
                cls: t('contact.form.class'), clsPlaceholder: t('contact.form.classPlaceholder'),
                message: t('contact.form.message'), send: t('contact.form.send'), sending: t('contact.form.sending'),
                ok: t('contact.form.ok'), err: t('contact.form.err'),
              }}
            />
          </div>
        </div>
      </section>
    </>
  );
}
