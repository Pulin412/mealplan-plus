import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// Paths that don't require authentication
const PUBLIC_PATHS = ["/login", "/api/icons", "/__/auth", "/api/auth"];

// Paths that the middleware should ignore entirely (Next.js internals, static assets)
const BYPASS_PATTERN =
  /^(\/_next\/|\/favicon\.ico|\/sw\.js|\/manifest\.json|\/apple-icon|\/apple-icon\.png|\/icon|\/icon\.png|.*\.png$|.*\.svg$|.*\.ico$|^\/__\/)/;

export function middleware(request: NextRequest) {
  try {
    const { pathname } = request.nextUrl;

    if (BYPASS_PATTERN.test(pathname)) return NextResponse.next();

    const isPublic = PUBLIC_PATHS.some((p) => pathname.startsWith(p));
    const hasSession = request.cookies.has("mp_session");

    // Unauthenticated user hitting a protected route → /login
    if (!isPublic && !hasSession) {
      const loginUrl = new URL("/login", request.url);
      if (pathname !== "/") loginUrl.searchParams.set("from", pathname);
      return NextResponse.redirect(loginUrl);
    }

    // Authenticated user hitting /login → /dashboard
    if (pathname.startsWith("/login") && hasSession) {
      return NextResponse.redirect(new URL("/dashboard", request.url));
    }

    return NextResponse.next();
  } catch (err) {
    // Surface the real error in Vercel logs instead of generic MIDDLEWARE_INVOCATION_FAILED
    console.error("[middleware] uncaught error:", err);
    return NextResponse.next();
  }
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|__/auth|icon|apple-icon).*)"],
};
