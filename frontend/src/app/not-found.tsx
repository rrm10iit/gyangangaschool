import Link from 'next/link';

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
      <p className="text-6xl font-extrabold text-gold-500">404</p>
      <h1 className="text-2xl font-bold">Page not found / पृष्ठ नहीं मिला</h1>
      <Link href="/" className="btn-primary">Home / मुख्य पृष्ठ</Link>
    </main>
  );
}
