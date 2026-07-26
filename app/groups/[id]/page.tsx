import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { api, ApiError, type Booking, type GroupView, type Message } from "@/lib/api";
import { getSession } from "@/lib/session";
import { leaveGroup, postGroupMessage, updateTravelDate } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";

export default async function GroupPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ error?: string; joined?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { id } = await params;
  const { error, joined } = await searchParams;

  let view: GroupView;
  let messages: Message[];
  let mine: Booking[];
  try {
    [view, messages, mine] = await Promise.all([
      api<GroupView>(`/v1/groups/${id}`),
      api<Message[]>(`/v1/groups/${id}/messages`),
      api<Booking[]>("/v1/bookings/me"),
    ]);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  const myBooking = mine.find((b) => b.groupId === id && b.status === "ACTIVE");
  const me = view.members.find((m) => m.me);

  return (
    <div>
      <PageHeader
        script="Your travel group"
        title={`${view.route.fromLocation} → ${view.route.toLocation}`}
        subtitle={
          view.weekStart && view.weekEnd
            ? `Landing week ${new Date(view.weekStart + "T00:00:00").toLocaleDateString("en-GB", { day: "numeric", month: "long" })} – ${new Date(view.weekEnd + "T00:00:00").toLocaleDateString("en-GB", { day: "numeric", month: "long" })} — chat below and settle on one landing day. One van, split fare.`
            : "Chat below and settle on one landing day — one van, split fare."
        }
      />
      <div className="mx-auto max-w-5xl px-5 py-12">
        {joined && <div className="notice-ok mb-6">You&rsquo;re in — say hi to the group below!</div>}
        {error && <div className="notice-error mb-6">{error}</div>}
        {view.agreedDate && (
          <div className="notice-ok mb-6">
            🎉 Everyone agrees on{" "}
            <strong>
              {new Date(view.agreedDate + "T00:00:00").toLocaleDateString("en-GB", {
                weekday: "long", day: "numeric", month: "long", year: "numeric",
              })}
            </strong>{" "}
            — you&rsquo;re set.
          </div>
        )}
        {view.driver && (
          <div className="card p-5 mb-8 flex flex-wrap items-center gap-x-6 gap-y-1 text-[15px]">
            <span className="eyebrow !mb-0">Your driver</span>
            <span className="font-display">{view.driver.name}</span>
            {view.driver.vehicle && <span className="text-ink-soft">{view.driver.vehicle}</span>}
            {view.driver.plateNo && <span className="tabular font-semibold">{view.driver.plateNo}</span>}
            {view.driver.phone && <span className="text-ink-soft">{view.driver.phone}</span>}
          </div>
        )}

        <div className="grid gap-8 lg:grid-cols-[1fr_1.2fr] items-start">
          <div className="grid gap-4">
            <h2 className="text-lg">Who&rsquo;s in the van</h2>
            {view.members.map((m, i) => (
              <article key={i} className="card p-5">
                <div className="flex items-center justify-between gap-3">
                  <p className="font-display text-[16px]">
                    {m.firstName} {m.me && <span className="stamp stamp-confirmed ml-1">you</span>}
                  </p>
                  <span className="text-ink-soft text-[14px]">
                    {m.partySize} {m.partySize === 1 ? "seat" : "seats"}
                  </span>
                </div>
                <p className="text-ink-soft text-[14.5px] mt-1">
                  lands{" "}
                  {new Date(m.travelDate + "T00:00:00").toLocaleDateString("en-GB", {
                    day: "numeric", month: "long",
                  })}
                </p>
                {m.intro && <p className="text-[14px] text-muted mt-1.5">{m.intro}</p>}
              </article>
            ))}

            {myBooking && me && (
              <div className="card p-5">
                <p className="field-label">Your landing day</p>
                <form action={updateTravelDate} className="flex gap-2.5">
                  <input type="hidden" name="group_id" value={id} />
                  <input type="hidden" name="booking_id" value={myBooking.id} />
                  <input
                    className="field-input !h-[42px] max-w-[190px]"
                    type="date"
                    name="travel_date"
                    defaultValue={me.travelDate}
                    required
                  />
                  <button className="btn btn-ghost btn-sm !h-[42px]">Update</button>
                </form>
                <form action={leaveGroup} className="mt-3">
                  <input type="hidden" name="group_id" value={id} />
                  <button className="btn btn-ghost btn-sm">Leave group — ride alone</button>
                </form>
              </div>
            )}
          </div>

          <div>
            <h2 className="text-lg mb-4">Group chat</h2>
            <div className="ticket">
              <div className="p-5 grid gap-4 max-h-[440px] overflow-y-auto">
                {messages.length === 0 && (
                  <p className="text-muted text-[14px] text-center py-6">
                    No messages yet — say hi and share your plans!
                  </p>
                )}
                {messages.map((m) => (
                  <div key={m.id} className={m.mine ? "text-right" : "text-left"}>
                    <p className="text-[12.5px] text-muted mb-0.5">
                      <span className="font-medium text-ink-soft">{m.authorFirstName}</span>{" "}
                      {m.createdAt.slice(0, 16).replace("T", " ")}
                    </p>
                    <p
                      className={`inline-block rounded-[10px] px-3.5 py-2 text-[14.5px] max-w-[85%] ${
                        m.mine ? "bg-navy text-white" : ""
                      }`}
                      style={m.mine ? undefined : { background: "var(--paper-deep)" }}
                    >
                      {m.body}
                    </p>
                  </div>
                ))}
              </div>
              <form action={postGroupMessage} className="flex gap-2.5 p-4 border-t border-line">
                <input type="hidden" name="group_id" value={id} />
                <input
                  className="field-input !h-[44px]"
                  name="body"
                  maxLength={1000}
                  placeholder="Write a message…"
                  autoComplete="off"
                  required
                />
                <button className="btn btn-primary btn-sm !h-[44px]">Send</button>
              </form>
            </div>
            <p className="text-[13px] text-muted mt-3">
              Refresh to see new replies. Back to{" "}
              <Link href="/trips" className="text-accent hover:underline underline-offset-2">
                my trips
              </Link>
              .
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
