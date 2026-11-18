import {
  BookOpen, FlaskConical, Trophy, Bus, MonitorPlay, UtensilsCrossed, School, Image as ImageIcon, Sparkles,
} from 'lucide-react';

const icons: Record<string, React.ComponentType<{ size?: number; className?: string }>> = {
  library: BookOpen, labs: FlaskConical, sports: Trophy, transport: Bus, smart: MonitorPlay, canteen: UtensilsCrossed,
};

export function FacilityIcon({ name, size = 28, className }: { name?: string; size?: number; className?: string }) {
  const Icon = (name && icons[name]) || Sparkles;
  return <Icon size={size} className={className} />;
}

/** Shown when a photo has not been uploaded yet. */
export function Placeholder({ icon, className = '' }: { icon?: string; className?: string }) {
  return (
    <div
      aria-hidden
      className={`flex items-center justify-center bg-gradient-to-br from-navy-700 to-navy-900 text-gold-400 ${className}`}
    >
      {icon ? <FacilityIcon name={icon} size={44} /> : <School size={44} />}
    </div>
  );
}

export function SectionHeader({ kicker, title, sub, center = false }: { kicker?: string; title: string; sub?: string; center?: boolean }) {
  return (
    <div className={center ? 'mx-auto max-w-3xl text-center' : ''}>
      {kicker && <span className="kicker">{kicker}</span>}
      <h2 className="h2">{title}</h2>
      {sub && <p className={`lead ${center ? 'mx-auto' : ''}`}>{sub}</p>}
    </div>
  );
}

export function PageHero({ title, sub }: { title: string; sub?: string }) {
  return (
    <div className="bg-gradient-to-br from-navy-900 via-navy-800 to-navy-700 text-white">
      <div className="container-page py-14 sm:py-20">
        <h1 className="text-3xl font-bold tracking-tight !text-white sm:text-5xl">{title}</h1>
        {sub && <p className="mt-4 max-w-2xl text-base text-navy-100 sm:text-lg">{sub}</p>}
        <div className="mt-6 h-1 w-20 rounded bg-gold-500" />
      </div>
    </div>
  );
}

export function Empty({ children }: { children: React.ReactNode }) {
  return (
    <div className="card flex flex-col items-center gap-3 p-10 text-center text-slate-500 dark:text-slate-400">
      <ImageIcon size={32} aria-hidden />
      <p>{children}</p>
    </div>
  );
}

export function formatDate(iso: string | undefined, lang: 'en' | 'hi') {
  if (!iso) return '';
  const d = new Date(iso + 'T00:00:00');
  return d.toLocaleDateString(lang === 'hi' ? 'hi-IN' : 'en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}
