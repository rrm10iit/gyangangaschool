import { NextRequest, NextResponse } from 'next/server';

/**
 * Cheap gate for the admin area: if there is no session cookie, go to the login page.
 * The backend verifies the token on every API call, so this is only for navigation.
 */
export function proxy(req: NextRequest) {
  const { pathname } = req.nextUrl;
  if (pathname === '/admin/login') return NextResponse.next();
  if (!req.cookies.get('ggs_token')) {
    const url = req.nextUrl.clone();
    url.pathname = '/admin/login';
    url.search = '';
    return NextResponse.redirect(url);
  }
  const res = NextResponse.next();
  res.headers.set('Cache-Control', 'no-store');
  return res;
}

export const config = { matcher: ['/admin/:path*'] };
