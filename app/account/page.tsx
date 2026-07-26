import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { changePassword } from "@/lib/actions";
import { api } from "@/lib/api";
import PageHeader from "@/components/PageHeader";

export default async function AccountPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; ok?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { error, ok } = await searchParams;
  const user = await api<{ name: string; email: string }>("/v1/users/me");

  return (
    <div>
      <PageHeader script="Account" title={user?.name ?? "Account"} subtitle={user?.email} />
      <div className="mx-auto max-w-md px-5 py-12">
        {error && <div className="notice-error mb-6">{error}</div>}
        {ok && <div className="notice-ok mb-6">Password changed. Use the new one next time you log in.</div>}

        <h2 className="text-lg mb-4">Change password</h2>
        <form action={changePassword} className="card p-6 sm:p-7 grid gap-5">
          <div>
            <label className="field-label" htmlFor="current_password">Current password</label>
            <input className="field-input" id="current_password" name="current_password" type="password" required />
          </div>
          <div>
            <label className="field-label" htmlFor="new_password">New password</label>
            <input className="field-input" id="new_password" name="new_password" type="password" minLength={6} placeholder="6+ characters" required />
          </div>
          <div>
            <label className="field-label" htmlFor="confirm_password">Repeat new password</label>
            <input className="field-input" id="confirm_password" name="confirm_password" type="password" minLength={6} required />
          </div>
          <button className="btn btn-primary w-full mt-1">Change password</button>
        </form>
      </div>
    </div>
  );
}
