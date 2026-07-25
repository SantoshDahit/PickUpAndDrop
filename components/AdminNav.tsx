import Link from "next/link";

export default function AdminNav({ active }: { active: "requests" | "routes" | "drivers" }) {
  const tabs = [
    { key: "requests", href: "/admin", label: "Requests" },
    { key: "routes", href: "/admin/routes", label: "Routes & pricing" },
    { key: "drivers", href: "/admin/drivers", label: "Drivers" },
  ] as const;

  return (
    <nav className="flex gap-1 border-b border-line mb-8">
      {tabs.map((t) => (
        <Link
          key={t.key}
          href={t.href}
          className={
            "px-4 py-2.5 text-[14.5px] font-semibold border-b-2 -mb-px transition-colors " +
            (active === t.key
              ? "border-ink text-ink"
              : "border-transparent text-muted hover:text-ink")
          }
        >
          {t.label}
        </Link>
      ))}
    </nav>
  );
}
