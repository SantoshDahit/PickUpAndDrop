import type { Metadata } from "next";
import { Inter } from "next/font/google";
import Link from "next/link";
import "./globals.css";
import { getSession } from "@/lib/session";
import { logout } from "@/lib/actions";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Pickup & Drop — Airport pickup anywhere in Korea",
  description:
    "Land in Korea, get picked up. Group airport rides — book together, pay less, pay cash on arrival.",
};

function Wordmark({ light = false }: { light?: boolean }) {
  return (
    <span
      className={`font-semibold text-[17px] tracking-[-0.03em] ${light ? "text-white" : "text-ink"}`}
    >
      Pickup&amp;Drop
    </span>
  );
}

const navLink =
  "px-3 py-2 rounded-md text-[14px] font-medium text-ink-soft hover:text-ink hover:bg-paper-deep transition-colors";

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const session = await getSession();

  return (
    <html lang="en" className={`${inter.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">
        <header className="border-b border-line bg-surface sticky top-0 z-20">
          <div className="mx-auto max-w-6xl px-5 h-16 flex items-center justify-between">
            <Link href="/">
              <Wordmark />
            </Link>
            <nav className="flex items-center gap-1 text-[14px]">
              {session?.isAdmin ? (
                <>
                  <Link href="/admin" className={navLink}>Requests</Link>
                  <Link href="/admin/routes" className={navLink}>Routes &amp; pricing</Link>
                  <Link href="/admin/drivers" className={navLink}>Drivers</Link>
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
                  <Link href="/book" className="btn btn-primary btn-sm ml-2 !h-[38px] !px-4">
                    Book a pickup
                  </Link>
                </>
              ) : (
                <>
                  <Link href="/login" className={navLink}>Log in</Link>
                  <Link href="/signup" className="btn btn-primary btn-sm ml-2 !h-[38px] !px-4">
                    Sign up
                  </Link>
                </>
              )}
            </nav>
          </div>
        </header>

        <main className="flex-1">{children}</main>

        <footer className="bg-navy text-white mt-24">
          <div className="mx-auto max-w-6xl px-5 py-14 grid gap-10 sm:grid-cols-2 lg:grid-cols-[1.6fr_1fr_1fr_1fr]">
            <div>
              <div className="mb-4">
                <Wordmark light />
              </div>
              <p className="text-[14px] text-white/55 max-w-xs leading-relaxed">
                Airport pickups anywhere in Korea. Book with your group, split
                the fare, and pay the driver in cash when you land.
              </p>
            </div>
            <div>
              <p className="text-[12.5px] font-medium uppercase tracking-[0.08em] text-white/40 mb-4">Services</p>
              <ul className="grid gap-2.5 text-[14px] text-white/70">
                <li><Link href="/book" className="hover:text-white transition-colors">Airport pickup</Link></li>
                <li><Link href="/book" className="hover:text-white transition-colors">Group rides</Link></li>
                <li><Link href="/#fares" className="hover:text-white transition-colors">Fares</Link></li>
              </ul>
            </div>
            <div>
              <p className="text-[12.5px] font-medium uppercase tracking-[0.08em] text-white/40 mb-4">Company</p>
              <ul className="grid gap-2.5 text-[14px] text-white/70">
                <li><Link href="/#how-it-works" className="hover:text-white transition-colors">How it works</Link></li>
                <li><Link href="/#why-us" className="hover:text-white transition-colors">Why book with us</Link></li>
                <li><Link href="/#faq" className="hover:text-white transition-colors">FAQ</Link></li>
              </ul>
            </div>
            <div>
              <p className="text-[12.5px] font-medium uppercase tracking-[0.08em] text-white/40 mb-4">Account</p>
              <ul className="grid gap-2.5 text-[14px] text-white/70">
                {session ? (
                  <>
                    <li><Link href="/trips" className="hover:text-white transition-colors">My trips</Link></li>
                    <li><Link href="/account" className="hover:text-white transition-colors">Settings</Link></li>
                  </>
                ) : (
                  <>
                    <li><Link href="/login" className="hover:text-white transition-colors">Log in</Link></li>
                    <li><Link href="/signup" className="hover:text-white transition-colors">Sign up</Link></li>
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
