import type { Metadata } from "next";
import { Fraunces, Instrument_Sans } from "next/font/google";
import Link from "next/link";
import "./globals.css";
import { getSession } from "@/lib/session";
import { logout } from "@/lib/actions";

const fraunces = Fraunces({
  variable: "--font-fraunces",
  subsets: ["latin"],
  axes: ["SOFT", "WONK", "opsz"],
});

const instrument = Instrument_Sans({
  variable: "--font-instrument",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Pickup & Drop — Airport pickup anywhere in Korea",
  description:
    "Land in Korea, get picked up. Group airport rides — book together, pay less, pay cash on arrival.",
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const session = await getSession();

  return (
    <html lang="en" className={`${fraunces.variable} ${instrument.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">
        <header className="border-b border-line bg-surface/80 backdrop-blur-sm sticky top-0 z-20">
          <div className="mx-auto max-w-5xl px-5 h-16 flex items-center justify-between">
            <Link href="/" className="flex items-center gap-2.5 group">
              <span className="inline-flex h-8 w-8 items-center justify-center rounded-lg bg-brick text-white font-display text-lg leading-none pb-0.5">
                p
              </span>
              <span className="font-display text-lg text-ink">
                Pickup <span className="text-brick">&amp;</span> Drop
              </span>
            </Link>
            <nav className="flex items-center gap-1.5 text-[14.5px]">
              {session?.isAdmin ? (
                <>
                  <Link href="/admin" className="btn btn-ghost btn-sm border-transparent">
                    Requested bookings
                  </Link>
                  <Link href="/admin/routes" className="btn btn-ghost btn-sm border-transparent">
                    Routes &amp; pricing
                  </Link>
                  <Link href="/admin/drivers" className="btn btn-ghost btn-sm border-transparent">
                    Drivers
                  </Link>
                  <Link href="/account" className="btn btn-ghost btn-sm border-transparent">
                    Account
                  </Link>
                  <form action={logout}>
                    <button className="btn btn-ghost btn-sm">Log out</button>
                  </form>
                </>
              ) : session ? (
                <>
                  <Link href="/book" className="btn btn-ghost btn-sm border-transparent">
                    Book a pickup
                  </Link>
                  <Link href="/trips" className="btn btn-ghost btn-sm border-transparent">
                    My trips
                  </Link>
                  <Link href="/account" className="btn btn-ghost btn-sm border-transparent">
                    Account
                  </Link>
                  <form action={logout}>
                    <button className="btn btn-ghost btn-sm">Log out</button>
                  </form>
                </>
              ) : (
                <>
                  <Link href="/login" className="btn btn-ghost btn-sm border-transparent">
                    Log in
                  </Link>
                  <Link href="/signup" className="btn btn-primary btn-sm">
                    Sign up
                  </Link>
                </>
              )}
            </nav>
          </div>
        </header>

        <main className="flex-1">{children}</main>

        <footer className="border-t border-line mt-16">
          <div className="mx-auto max-w-5xl px-5 py-8 flex flex-col sm:flex-row items-center justify-between gap-3 text-sm text-muted">
            <p>Pickup &amp; Drop — airport rides, anywhere in Korea.</p>
            <p>
              Pay cash on arrival · <span className="text-ink-soft">Every request welcome</span>
            </p>
          </div>
        </footer>
      </body>
    </html>
  );
}
