import { redirect } from "next/navigation";
import { getActiveRoutes, getAllTiers } from "@/lib/db";
import { getSession } from "@/lib/session";
import BookingForm from "@/components/BookingForm";
import PageHeader from "@/components/PageHeader";

export default async function BookPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; route?: string; people?: string; date?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { error, route, people, date } = await searchParams;
  const routes = await getActiveRoutes();
  const tiers = await getAllTiers();

  const initialRouteId = routes.some((r) => r.id === Number(route)) ? Number(route) : undefined;
  const initialPeople = Math.min(12, Math.max(1, Number(people) || 0)) || undefined;
  const initialDate = date && /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : undefined;

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
        />
      </div>
    </div>
  );
}
