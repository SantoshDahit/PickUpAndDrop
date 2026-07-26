import { redirect } from "next/navigation";
import Link from "next/link";
import { api, getRoutesWithTiers, type OpenRide } from "@/lib/api";
import { getSession } from "@/lib/session";
import BookingForm from "@/components/BookingForm";
import PageHeader from "@/components/PageHeader";

export default async function BookPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; route?: string; people?: string; date?: string; ride?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { error, route, people, date, ride } = await searchParams;
  const [{ routes, tiers }, openRides] = await Promise.all([
    getRoutesWithTiers(),
    api<OpenRide[]>("/v1/groups/open").catch(() => [] as OpenRide[]),
  ]);

  const joining = ride ? openRides.find((r) => r.id === ride) : undefined;
  const initialRouteId = joining
    ? joining.route.id
    : routes.some((r) => r.id === route) ? route : undefined;
  const initialPeople = Math.min(6, Math.max(1, Number(people) || 0)) || undefined;
  const initialDate = joining
    ? joining.targetDate
    : date && /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : undefined;

  return (
    <div>
      <PageHeader
        script="Book a pickup"
        title={`Where are you landing, ${session.name.split(" ")[0]}?`}
        subtitle="Fill this in once for your whole group. We'll confirm your van and driver over your contact below."
      />
      <div className="mx-auto max-w-5xl px-5 py-12">
        <BookingForm
          routes={routes}
          tiers={tiers}
          error={error}
          initialRouteId={initialRouteId}
          initialPeople={initialPeople}
          initialDate={initialDate}
          joinRide={joining ? {
            id: joining.id,
            targetDate: joining.targetDate,
            toLocation: joining.route.toLocation,
          } : undefined}
        />

        {openRides.length > 0 && !joining && (
          <section className="mt-14">
            <p className="eyebrow mb-2">Or join a ride that&rsquo;s already going</p>
            <h2 className="text-xl mb-5">Published rides with a date set</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              {openRides.map((r) => (
                <article key={r.id} className="card p-5 flex items-center justify-between gap-4">
                  <div>
                    <p className="font-display text-[16px]">
                      {r.route.fromLocation} → {r.route.toLocation}
                    </p>
                    <p className="text-ink-soft text-[14px] mt-0.5">
                      around{" "}
                      {new Date(r.targetDate + "T00:00:00").toLocaleDateString("en-GB", {
                        day: "numeric", month: "long", year: "numeric",
                      })}
                      {" · "}{r.memberCount} in · {r.seatsLeft} seats left
                    </p>
                  </div>
                  <Link href={`/book?ride=${r.id}`} className="btn btn-ghost btn-sm shrink-0">
                    Join this ride
                  </Link>
                </article>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
