"use client";

import Link from "next/link";
import { useState } from "react";

type NavItem = { href: string; label: string; external?: boolean };

const itemCls =
  "block px-3 py-3 rounded-lg text-[15px] font-medium text-ink hover:bg-paper-deep transition-colors";

export default function MobileNav({
  links,
  cta,
  logoutAction,
}: {
  links: NavItem[];
  cta: NavItem | null;
  logoutAction?: () => Promise<void>;
}) {
  const [open, setOpen] = useState(false);

  return (
    <div className="md:hidden">
      <button
        aria-label={open ? "Close menu" : "Open menu"}
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
        className="flex h-10 w-10 items-center justify-center rounded-full border border-line text-ink cursor-pointer"
      >
        {open ? (
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M6 6l12 12M18 6 6 18" />
          </svg>
        ) : (
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M4 7h16M4 12h16M4 17h16" />
          </svg>
        )}
      </button>

      {open && (
        <div className="absolute left-0 right-0 top-full bg-surface border-b border-line shadow-lg z-30">
          <nav className="mx-auto max-w-6xl px-5 py-4 grid gap-0.5">
            {links.map((l) =>
              l.external ? (
                <a key={l.href} href={l.href} className={itemCls}>
                  {l.label}
                </a>
              ) : (
                <Link key={l.href} href={l.href} onClick={() => setOpen(false)} className={itemCls}>
                  {l.label}
                </Link>
              )
            )}
            {logoutAction && (
              <form action={logoutAction}>
                <button className={itemCls + " w-full text-left cursor-pointer"}>Log out</button>
              </form>
            )}
            {cta && (
              <Link href={cta.href} onClick={() => setOpen(false)} className="btn btn-primary mt-3">
                {cta.label}
              </Link>
            )}
          </nav>
        </div>
      )}
    </div>
  );
}
