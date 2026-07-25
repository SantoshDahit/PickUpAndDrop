import { redirect } from "next/navigation";
import { getAllDrivers } from "@/lib/db";
import { getSession } from "@/lib/session";
import { addDriver, toggleDriver } from "@/lib/actions";
import AdminNav from "@/components/AdminNav";

export default async function AdminDriversPage() {
  const session = await getSession();
  if (!session?.isAdmin) redirect("/login");

  const drivers = await getAllDrivers();

  return (
    <div className="mx-auto max-w-6xl px-5 py-12">
      <p className="eyebrow mb-3">Admin</p>
      <h1 className="text-3xl mb-2">Drivers</h1>
      <p className="text-ink-soft mb-8">
        Keep license numbers on file — only licensed drivers keep this business legal.
        Own-car drivers make small groups profitable; rentals need fuller vans.
      </p>

      <AdminNav active="drivers" />

      {/* Add driver */}
      <form action={addDriver} className="card p-5 mb-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-[1.2fr_1fr_1fr_1fr_auto_auto] items-end">
        <div>
          <label className="field-label" htmlFor="name">Name</label>
          <input className="field-input" id="name" name="name" placeholder="Driver name" required />
        </div>
        <div>
          <label className="field-label" htmlFor="phone">Phone</label>
          <input className="field-input" id="phone" name="phone" placeholder="010-…" />
        </div>
        <div>
          <label className="field-label" htmlFor="license_no">License no.</label>
          <input className="field-input" id="license_no" name="license_no" placeholder="12-345678-90" />
        </div>
        <div>
          <label className="field-label" htmlFor="vehicle">Vehicle</label>
          <input className="field-input" id="vehicle" name="vehicle" placeholder="Starex 11-seat" />
        </div>
        <div>
          <label className="field-label" htmlFor="seats">Seats</label>
          <input className="field-input w-20" id="seats" name="seats" type="number" min={1} max={15} defaultValue={4} />
        </div>
        <div className="flex items-center gap-4 pb-1">
          <label className="flex items-center gap-2 text-[14.5px] font-medium whitespace-nowrap">
            <input type="checkbox" name="owns_vehicle" className="h-4 w-4 accent-[var(--ink)]" />
            Own car
          </label>
          <button className="btn btn-primary">Add</button>
        </div>
      </form>

      {drivers.length === 0 ? (
        <div className="card p-10 text-center text-ink-soft">
          No drivers yet — add your first driver above.
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {drivers.map((d) => (
            <div key={d.id} className={"card p-5" + (d.active ? "" : " opacity-60")}>
              <div className="flex items-center justify-between gap-3 mb-2">
                <h2 className="text-lg">{d.name}</h2>
                <span className={d.owns_vehicle ? "stamp stamp-confirmed" : "stamp stamp-pending"}>
                  {d.owns_vehicle ? "own car" : "needs rental"}
                </span>
              </div>
              <p className="text-[14.5px] text-ink-soft">
                {d.phone || "no phone"} · {d.vehicle || "vehicle TBD"} · {d.seats} seats
              </p>
              <p className="text-[14px] text-muted mb-4">
                License: {d.license_no || <span style={{ color: "var(--brick)" }}>missing — add before assigning!</span>}
              </p>
              <form action={toggleDriver}>
                <input type="hidden" name="id" value={d.id} />
                <button className="btn btn-ghost btn-sm">
                  {d.active ? "Deactivate" : "Reactivate"}
                </button>
              </form>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
