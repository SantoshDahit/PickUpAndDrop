import { redirect } from "next/navigation";
import { getActiveRoutes, getAllTiers } from "@/lib/db";
import { getSession } from "@/lib/session";
import BookingForm from "@/components/BookingForm";

export default async function BookPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { error } = await searchParams;
  const routes = await getActiveRoutes();
  const tiers = await getAllTiers();

  return (
    <div className="mx-auto max-w-5xl px-5 py-12">
      <p className="eyebrow mb-3">Book a pickup</p>
      <h1 className="text-3xl mb-2">Where are you landing, {session.name.split(" ")[0]}?</h1>
      <p className="text-ink-soft mb-10 max-w-xl">
        Fill this in once for your whole group. We&rsquo;ll confirm your van and
        driver over your contact below.
      </p>
      <BookingForm routes={routes} tiers={tiers} error={error} />
    </div>
  );
}
