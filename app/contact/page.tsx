import Link from "next/link";
import { redirect } from "next/navigation";
import { api, type SupportThread } from "@/lib/api";
import { getSession } from "@/lib/session";
import { sendSupportMessage } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";
import SubmitButton from "@/components/SubmitButton";

export default async function ContactPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { error } = await searchParams;
  const thread = await api<SupportThread>("/v1/support/messages");

  return (
    <div>
      <PageHeader
        script="We're here"
        title="Message the team"
        subtitle="Ask us anything — your pickup, a SIM card, or paperwork you've been asked for. We reply here."
      />
      <div className="mx-auto max-w-2xl px-5 py-12">
        {error && <div className="notice-error mb-6">{error}</div>}

        <div className="ticket">
          <div className="p-5 sm:p-6 grid gap-4 max-h-[460px] overflow-y-auto">
            {thread.messages.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-[15px] text-ink-soft mb-1.5">No messages yet.</p>
                <p className="text-muted text-[14px]">
                  Write below and the team will get back to you. Replies show up on this page.
                </p>
              </div>
            ) : (
              thread.messages.map((m) => (
                <div key={m.id} className={m.mine ? "text-right" : "text-left"}>
                  <p className="text-[12.5px] text-muted mb-0.5">
                    <span className={`font-medium ${m.staff ? "text-accent-deep" : "text-ink-soft"}`}>
                      {m.authorName}
                    </span>
                    {m.staff && (
                      <span
                        className="ml-1.5 align-middle rounded-full px-1.5 py-0.5 text-[10.5px] font-medium uppercase tracking-wide"
                        style={{ background: "var(--accent-tint)", color: "var(--accent-deep)" }}
                      >
                        Official
                      </span>
                    )}{" "}
                    {m.createdAt.slice(0, 16).replace("T", " ")}
                  </p>
                  <p
                    className={`inline-block rounded-[10px] px-3.5 py-2 text-[14.5px] max-w-[85%] whitespace-pre-wrap ${
                      m.mine ? "bg-navy text-white" : ""
                    }`}
                    style={
                      m.mine
                        ? undefined
                        : m.staff
                          ? { background: "var(--accent-tint)", boxShadow: "inset 0 0 0 1px var(--accent)" }
                          : { background: "var(--paper-deep)" }
                    }
                  >
                    {m.body}
                  </p>
                </div>
              ))
            )}
          </div>

          <form action={sendSupportMessage} className="flex gap-2.5 p-4 border-t border-line">
            <input
              className="field-input !h-[44px]"
              name="body"
              maxLength={1000}
              placeholder="Write a message…"
              required
              autoComplete="off"
            />
            <SubmitButton pendingLabel="Sending…" className="btn btn-primary btn-sm !h-[44px] !px-6">
              Send
            </SubmitButton>
          </form>
        </div>

        <p className="text-sm text-muted mt-6 text-center">
          Travelling with a group? Day-to-day arrangements are easier in your{" "}
          <Link href="/trips" className="text-accent font-medium hover:underline underline-offset-2">
            group chat
          </Link>
          .
        </p>
      </div>
    </div>
  );
}
