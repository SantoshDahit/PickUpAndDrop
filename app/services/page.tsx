import { redirect } from "next/navigation";
import { api, type ServiceRequest } from "@/lib/api";
import { getSession } from "@/lib/session";
import { cancelServiceRequest, requestSimCard } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";
import SubmitButton from "@/components/SubmitButton";

const STAMP: Record<string, string> = {
  REQUESTED: "stamp-pending",
  CONFIRMED: "stamp-confirmed",
  DELIVERED: "stamp-completed",
  CANCELLED: "stamp-cancelled",
};

const STATUS_LABEL: Record<string, string> = {
  REQUESTED: "Requested",
  CONFIRMED: "Confirmed",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

const SIM_PLANS = [
  "5 days / 3GB",
  "10 days / 5GB",
  "30 days / 10GB",
  "30 days / unlimited",
  "Not sure — advise me",
];

const AIRPORTS = ["ICN — Incheon", "GMP — Gimpo", "PUS — Busan", "CJU — Jeju", "Other"];

export default async function ServicesPage({
  searchParams,
}: {
  searchParams: Promise<{ requested?: string; error?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { requested, error } = await searchParams;
  const requests = await api<ServiceRequest[]>("/v1/service-requests/me");
  const open = requests.filter((r) => r.status === "REQUESTED" || r.status === "CONFIRMED");

  return (
    <div>
      <PageHeader
        script="More than the ride"
        title="Traveller services"
        subtitle="Sort out the essentials before you land — starting with a Korean SIM waiting for you at arrivals."
      />
      <div className="mx-auto max-w-3xl px-5 py-12">
        {requested && (
          <div className="notice-ok mb-8">
            Request received — we&apos;ll confirm the details and the price with you shortly.
          </div>
        )}
        {error && <div className="notice-error mb-8">{error}</div>}

        {/* ---------- SIM card ---------- */}
        <section className="mb-12">
          <h2 className="text-lg mb-1">Korean SIM card</h2>
          <p className="text-muted text-[14.5px] mb-5">
            A local number and data from the moment you land. Tell us when you arrive and what
            you need — we confirm the plan and price before anything is charged. You&apos;ll need
            your passport at handover to register the SIM.
          </p>

          <form action={requestSimCard} className="card p-6 sm:p-7 grid gap-5">
            <div className="grid gap-5 sm:grid-cols-2">
              <div>
                <label className="field-label" htmlFor="arrival_date">Arrival date</label>
                <input className="field-input" id="arrival_date" name="arrival_date" type="date" />
                <p className="text-[13px] text-muted mt-1.5">Leave blank if your flight isn&apos;t booked yet.</p>
              </div>
              <div>
                <label className="field-label" htmlFor="airport">Arriving at</label>
                <select className="field-input" id="airport" name="airport" defaultValue={AIRPORTS[0]}>
                  {AIRPORTS.map((a) => <option key={a} value={a}>{a}</option>)}
                </select>
              </div>
            </div>
            <div>
              <label className="field-label" htmlFor="plan">Plan</label>
              <select className="field-input" id="plan" name="plan" defaultValue={SIM_PLANS[2]}>
                {SIM_PLANS.map((p) => <option key={p} value={p}>{p}</option>)}
              </select>
            </div>
            <div className="grid gap-5 sm:grid-cols-2">
              <div>
                <label className="field-label" htmlFor="deliver_to">Where should we hand it over?</label>
                <input
                  className="field-input"
                  id="deliver_to"
                  name="deliver_to"
                  maxLength={255}
                  placeholder="At arrivals with my pickup"
                />
              </div>
              <div>
                <label className="field-label" htmlFor="contact">Contact on arrival</label>
                <input
                  className="field-input"
                  id="contact"
                  name="contact"
                  maxLength={60}
                  placeholder="WhatsApp / Viber number"
                />
              </div>
            </div>
            <div>
              <label className="field-label" htmlFor="notes">Anything else?</label>
              <textarea
                className="field-input"
                id="notes"
                name="notes"
                rows={2}
                maxLength={1000}
                placeholder="Flight number, landing time, second SIM for a friend…"
              />
            </div>
            <SubmitButton pendingLabel="Sending your request…">Request a SIM card</SubmitButton>
          </form>
        </section>

        {/* ---------- My requests ---------- */}
        <section className="mb-12">
          <h2 className="text-lg mb-1">Your requests</h2>
          <p className="text-muted text-[14.5px] mb-5">
            {open.length > 0
              ? "We'll be in touch about the ones still open."
              : "Anything you ask for shows up here with its status."}
          </p>

          {requests.length === 0 ? (
            <div className="card p-8 text-center text-muted text-[14.5px]">
              No requests yet.
            </div>
          ) : (
            <div className="grid gap-3">
              {requests.map((r) => (
                <div key={r.id} className="card p-5 flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <p className="font-medium mb-1">
                      SIM card
                      <span className={`stamp ${STAMP[r.status]} ml-2.5`}>
                        {STATUS_LABEL[r.status]}
                      </span>
                    </p>
                    <p className="text-[14px] text-muted">
                      {r.detail ?? "Plan to be confirmed"}
                      {r.arrivalDate ? ` · arriving ${r.arrivalDate}` : " · arrival date to confirm"}
                      {r.airport ? ` · ${r.airport}` : ""}
                    </p>
                    {r.notes && <p className="text-[13.5px] text-muted mt-1">“{r.notes}”</p>}
                  </div>
                  {(r.status === "REQUESTED" || r.status === "CONFIRMED") && (
                    <form action={cancelServiceRequest}>
                      <input type="hidden" name="id" value={r.id} />
                      <button className="btn btn-ghost btn-sm cursor-pointer">Cancel</button>
                    </form>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>

        {/* ---------- Other facilities (information only) ---------- */}
        <section>
          <h2 className="text-lg mb-1">Also available</h2>
          <p className="text-muted text-[14.5px] mb-5">
            Ask our team about these — they&apos;re arranged directly, not through the site.
          </p>

          <div className="card p-6">
            <p className="font-medium mb-1.5">Bank balance documentation</p>
            <p className="text-[14.5px] text-ink-soft leading-relaxed">
              Our team can talk you through the bank balance paperwork travellers are commonly
              asked for, and what your bank needs from you to issue it. Get in touch and we&apos;ll
              explain what applies to your situation.
            </p>
            <p className="text-[13.5px] text-muted mt-3">
              Enquiries are handled by the team directly — nothing is arranged or requested here.
            </p>
          </div>
        </section>
      </div>
    </div>
  );
}
