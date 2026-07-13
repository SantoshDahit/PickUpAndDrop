import { redirect } from "next/navigation";
import { getAllRoutes, getTiersForRoute } from "@/lib/db";
import { getSession } from "@/lib/session";
import { addRoute, toggleRoute, setTier, deleteTier } from "@/lib/actions";
import AdminNav from "@/components/AdminNav";

export default async function AdminRoutesPage() {
  const session = await getSession();
  if (!session?.isAdmin) redirect("/login");

  const routes = await getAllRoutes();
  const routesWithTiers = await Promise.all(
    routes.map(async (route) => ({ route, tiers: await getTiersForRoute(route.id) }))
  );

  return (
    <div className="mx-auto max-w-6xl px-5 py-12">
      <p className="eyebrow mb-3">Admin</p>
      <h1 className="text-3xl mb-2">Routes &amp; pricing</h1>
      <p className="text-ink-soft mb-8">
        Each route has its own price ladder. For group sizes without an exact tier,
        the nearest smaller tier applies.
      </p>

      <AdminNav active="routes" />

      {/* Add route */}
      <form action={addRoute} className="card p-5 mb-10 grid gap-4 sm:grid-cols-[1fr_1fr_auto] items-end">
        <div>
          <label className="field-label" htmlFor="from_location">From</label>
          <input className="field-input" id="from_location" name="from_location" placeholder="Incheon Airport (ICN)" required />
        </div>
        <div>
          <label className="field-label" htmlFor="to_location">To</label>
          <input className="field-input" id="to_location" name="to_location" placeholder="Busan" required />
        </div>
        <button className="btn btn-primary">Add route</button>
      </form>

      <div className="grid gap-8 lg:grid-cols-2">
        {routesWithTiers.map(({ route, tiers }) => {
          return (
            <section key={route.id} className={"card p-6" + (route.active ? "" : " opacity-60")}>
              <div className="flex items-center justify-between gap-3 mb-4">
                <h2 className="text-lg">
                  {route.from_location} → {route.to_location}
                </h2>
                <form action={toggleRoute}>
                  <input type="hidden" name="id" value={route.id} />
                  <button className="btn btn-ghost btn-sm">
                    {route.active ? "Deactivate" : "Activate"}
                  </button>
                </form>
              </div>

              {tiers.length === 0 ? (
                <p className="text-muted text-[14.5px] mb-4">
                  No pricing yet — this route can&rsquo;t be booked until you add a tier.
                </p>
              ) : (
                <table className="w-full text-[15px] mb-4">
                  <thead>
                    <tr className="text-left text-muted text-[12.5px] uppercase tracking-wider">
                      <th className="py-1.5 font-semibold">Group size</th>
                      <th className="py-1.5 font-semibold text-right">Per person</th>
                      <th className="py-1.5 font-semibold text-right">Trip total</th>
                      <th className="py-1.5" />
                    </tr>
                  </thead>
                  <tbody>
                    {tiers.map((t) => (
                      <tr key={t.id} className="border-t border-line">
                        <td className="py-2">{t.group_size}</td>
                        <td className="py-2 text-right tabular">₩{t.price_per_person.toLocaleString()}</td>
                        <td className="py-2 text-right tabular text-muted">
                          ₩{(t.price_per_person * t.group_size).toLocaleString()}
                        </td>
                        <td className="py-2 text-right">
                          <form action={deleteTier}>
                            <input type="hidden" name="id" value={t.id} />
                            <button className="text-muted hover:text-brick text-[13px] font-semibold" aria-label="Delete tier">
                              remove
                            </button>
                          </form>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              <form action={setTier} className="rule-dashed pt-4 grid grid-cols-[1fr_1fr_auto] gap-3 items-end">
                <input type="hidden" name="route_id" value={route.id} />
                <div>
                  <label className="field-label">Group size</label>
                  <input name="group_size" type="number" min={1} max={20} className="field-input h-10 text-[14px]" placeholder="4" required />
                </div>
                <div>
                  <label className="field-label">Per person (₩)</label>
                  <input name="price_per_person" type="number" min={1} className="field-input h-10 text-[14px]" placeholder="16000" required />
                </div>
                <button className="btn btn-dark btn-sm h-10">Set tier</button>
              </form>
              <p className="text-[13px] text-muted mt-2">
                Setting an existing group size updates its price.
              </p>
            </section>
          );
        })}
      </div>
    </div>
  );
}
