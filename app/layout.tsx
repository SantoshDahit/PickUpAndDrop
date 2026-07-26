import type { Metadata } from "next";
import { Geologica, Roboto, Caveat } from "next/font/google";
import Link from "next/link";
import "./globals.css";
import { getSession } from "@/lib/session";
import { logout } from "@/lib/actions";
import MobileNav from "@/components/MobileNav";

const geologica = Geologica({
  variable: "--font-geologica",
  subsets: ["latin"],
});

const roboto = Roboto({
  variable: "--font-roboto",
  weight: ["400", "500", "700"],
  subsets: ["latin"],
});

const caveat = Caveat({
  variable: "--font-caveat",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Pickup & Drop — Airport pickup anywhere in Korea",
  description:
    "Land in Korea, get picked up. Group airport rides — book together, pay less, pay cash on arrival.",
};

function LogoMark({ light = false }: { light?: boolean }) {
  return (
    <span className="flex items-center gap-2">
      <span className="inline-flex h-9 w-9 items-center justify-center rounded-full bg-accent text-white">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 21s-6-5.3-6-10a6 6 0 1 1 12 0c0 4.7-6 10-6 10Z" />
          <circle cx="12" cy="11" r="2.2" />
        </svg>
      </span>
      <span className={`font-display text-[19px] ${light ? "!text-white" : ""}`}>
        Pickup<span className="text-accent">&amp;</span>Drop
      </span>
    </span>
  );
}

const ADMIN_URL = process.env.NEXT_PUBLIC_ADMIN_URL ?? "http://localhost:5173";

const navLink =
  "px-3.5 py-2 rounded-full text-[14.5px] font-medium text-ink hover:text-accent-deep transition-colors";

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const session = await getSession();

  return (
    <html lang="en" className={`${geologica.variable} ${roboto.variable} ${caveat.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">
        {/* Top strip */}
        <div className="bg-navy text-white/75 text-[13px]">
          <div className="mx-auto max-w-6xl px-5 h-9 flex items-center justify-between">
            <p className="flex items-center gap-2">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 21s-6-5.3-6-10a6 6 0 1 1 12 0c0 4.7-6 10-6 10Z" />
              </svg>
              Airport pickup → anywhere in Korea
            </p>
            <p className="hidden sm:block">Open 24/7 for arrivals · Pay cash, no card needed</p>
          </div>
        </div>

        <header className="relative border-b border-line bg-surface/90 backdrop-blur-md sticky top-0 z-20">
          <div className="mx-auto max-w-6xl px-5 h-[74px] flex items-center justify-between">
            <Link href="/">
              <LogoMark />
            </Link>
            <nav className="hidden md:flex items-center gap-0.5">
              {session?.isAdmin ? (
                <>
                  <a href={ADMIN_URL} className={navLink}>
                    Admin console
                  </a>
                  <Link href="/account" className={navLink}>Account</Link>
                  <form action={logout} className="ml-2">
                    <button className="btn btn-ghost btn-sm">Log out</button>
                  </form>
                </>
              ) : session ? (
                <>
                  <Link href="/trips" className={navLink}>My trips</Link>
                  <Link href="/account" className={navLink}>Account</Link>
                  <form action={logout}>
                    <button className={navLink + " cursor-pointer"}>Log out</button>
                  </form>
                  <Link href="/book" className="btn btn-primary btn-sm ml-3 !h-[42px] !px-6">
                    Book a pickup
                  </Link>
                </>
              ) : (
                <>
                  <Link href="/login" className={navLink}>Log in</Link>
                  <Link href="/signup" className="btn btn-primary btn-sm ml-3 !h-[42px] !px-6">
                    Sign up
                  </Link>
                </>
              )}
            </nav>
            <MobileNav
              links={
                session?.isAdmin
                  ? [
                      { href: ADMIN_URL, label: "Admin console", external: true },
                      { href: "/account", label: "Account" },
                    ]
                  : session
                    ? [
                        { href: "/trips", label: "My trips" },
                        { href: "/account", label: "Account" },
                      ]
                    : [{ href: "/login", label: "Log in" }]
              }
              cta={
                session?.isAdmin
                  ? null
                  : session
                    ? { href: "/book", label: "Book a pickup" }
                    : { href: "/signup", label: "Sign up" }
              }
              logoutAction={session ? logout : undefined}
            />
          </div>
        </header>

        <main className="flex-1">{children}</main>

        <footer className="bg-navy text-white mt-24">
          <div className="mx-auto max-w-6xl px-5 py-14 grid gap-10 sm:grid-cols-2 lg:grid-cols-[1.6fr_1fr_1fr_1fr]">
            <div>
              <div className="mb-5">
                <LogoMark light />
              </div>
              <p className="text-[14px] text-white/55 max-w-xs leading-relaxed">
                Airport pickups anywhere in Korea. Book with your group, split
                the fare, and pay the driver in cash when you land.
              </p>
            </div>
            <div>
              <p className="font-display text-[15px] !text-white mb-4">Services</p>
              <ul className="grid gap-2.5 text-[14px] text-white/60">
                <li><Link href="/book" className="hover:text-accent transition-colors">Airport pickup</Link></li>
                <li><Link href="/book" className="hover:text-accent transition-colors">Group rides</Link></li>
                <li><Link href="/#fares" className="hover:text-accent transition-colors">Fares</Link></li>
              </ul>
            </div>
            <div>
              <p className="font-display text-[15px] !text-white mb-4">Company</p>
              <ul className="grid gap-2.5 text-[14px] text-white/60">
                <li><Link href="/#how-it-works" className="hover:text-accent transition-colors">How it works</Link></li>
                <li><Link href="/#why-us" className="hover:text-accent transition-colors">Why book with us</Link></li>
                <li><Link href="/#faq" className="hover:text-accent transition-colors">FAQ</Link></li>
              </ul>
            </div>
            <div>
              <p className="font-display text-[15px] !text-white mb-4">Account</p>
              <ul className="grid gap-2.5 text-[14px] text-white/60">
                {session ? (
                  <>
                    <li><Link href="/trips" className="hover:text-accent transition-colors">My trips</Link></li>
                    <li><Link href="/account" className="hover:text-accent transition-colors">Settings</Link></li>
                  </>
                ) : (
                  <>
                    <li><Link href="/login" className="hover:text-accent transition-colors">Log in</Link></li>
                    <li><Link href="/signup" className="hover:text-accent transition-colors">Sign up</Link></li>
                  </>
                )}
              </ul>
            </div>
          </div>
          <div className="border-t border-white/10">
            <div className="mx-auto max-w-6xl px-5 py-5 flex flex-col sm:flex-row items-center justify-between gap-2 text-[13px] text-white/40">
              <p>© {new Date().getFullYear()} Pickup &amp; Drop. All rights reserved.</p>
              <p>Pay cash on arrival · No card needed</p>
            </div>
          </div>
        </footer>
      </body>
    </html>
  );
}
