import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { api, ApiError, type Booking, type GroupSuggestions } from "@/lib/api";
import { getSession } from "@/lib/session";
import { selectGroup } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";

function fmt(d: string) {
  return new Date(d + "T00:00:00").toLocaleDateString("en-GB", { day: "numeric", month: "long" });
}

export default async function ChooseGroupPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ error?: string; moved?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { id } = await params;
  const { error, moved } = await searchParams;

  let suggestions: GroupSuggestions;
  let mine: Booking[];
  try {
    [suggestions, mine] = await Promise.all([
      api<GroupSuggestions>(`/v1/bookings/${id}/group-suggestions`),
      api<Booking[]>("/v1/bookings/me"),
    ]);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
  const booking = mine.find((b) => b.id === id);

  return (
    <div>
      <PageHeader
        script="Find your group"
        title={`Landing week ${fmt(suggestions.weekStart)} – ${fmt(suggestions.weekEnd)}`}
        subtitle="Travellers landing the same week share one van and split the fare. Join a group below, or start the week's group yourself."
      />
      <div className="mx-auto max-w-3xl px-5 py-12">
        {moved && (
          <div className="notice-ok mb-6">
            Your new landing day is in a different week, so you left your old group —
            pick the right one for your new dates below.
          </div>
        )}
        {error && <div className="notice-error mb-6">{error}</div>}
        {booking && (
          <p className="text-ink-soft text-[15px] mb-8">
            Your trip: <strong>{booking.route?.fromLocation} → {booking.route?.toLocation}</strong>
            {" · "}landing {fmt(booking.travelDate)} · {booking.partySize}{" "}
            {booking.partySize === 1 ? "traveller" : "travellers"}
            {booking.groupId && <> · currently in a group (joining another switches you)</>}
          </p>
        )}

        <div className="grid gap-5">
          {suggestions.groups.map((g) => (
            <article key={g.id} className="ticket grid sm:grid-cols-[1fr_170px]">
              <div className="p-6">
                <div className="flex items-center gap-3 mb-2">
                  <p className="font-display text-[17px]">
                    {g.memberCount === 0
                      ? "Fresh group — be the first in"
                      : `Group of ${g.memberCount} ${g.memberCount === 1 ? "traveller" : "travellers"}`}
                  </p>
                  {g.official && <span className="stamp stamp-confirmed">official ride</span>}
                </div>
                <p className="text-ink-soft text-[14.5px]">
                  {g.official && g.targetDate && <>Planned for {fmt(g.targetDate)} · </>}
                  {g.earliestDate && (
                    <>currently landing {fmt(g.earliestDate)}
                      {g.latestDate && g.latestDate !== g.earliestDate && <> – {fmt(g.latestDate)}</>} · </>
                  )}
                  {g.seatsLeft} {g.seatsLeft === 1 ? "seat" : "seats"} free
                </p>
              </div>
              <div className="ticket-perf p-6 flex items-center justify-center">
                <form action={selectGroup}>
                  <input type="hidden" name="booking_id" value={id} />
                  <input type="hidden" name="group_id" value={g.id} />
                  <button className="btn btn-primary btn-sm">Join &amp; open chat</button>
                </form>
              </div>
            </article>
          ))}

          <article className="card p-6 flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="font-display text-[17px] mb-1">
                {suggestions.groups.length === 0
                  ? "No groups for your week yet"
                  : "None of these fit?"}
              </p>
              <p className="text-ink-soft text-[14.5px]">
                Start the group for {fmt(suggestions.weekStart)} – {fmt(suggestions.weekEnd)} —
                travellers landing that week will find you here.
              </p>
            </div>
            <form action={selectGroup}>
              <input type="hidden" name="booking_id" value={id} />
              <button className="btn btn-ghost">Start a new group</button>
            </form>
          </article>
        </div>

        <p className="text-[13.5px] text-muted mt-8">
          Prefer to ride alone? Just leave it — your booking stays individual.{" "}
          <Link href="/trips" className="text-accent hover:underline underline-offset-2">Back to my trips</Link>
        </p>
      </div>
    </div>
  );
}
