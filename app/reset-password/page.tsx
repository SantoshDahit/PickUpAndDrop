import Link from "next/link";
import { resetPassword } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";
import SubmitButton from "@/components/SubmitButton";

export default async function ResetPasswordPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string; error?: string }>;
}) {
  const { token, error } = await searchParams;

  return (
    <div>
      <PageHeader
        script="Almost there"
        title="Choose a new password"
        subtitle="Pick something you'll remember — you'll be logged in with it next."
      />
      <div className="mx-auto max-w-md px-5 py-12">
        {!token ? (
          <div className="card p-6 sm:p-7">
            <div className="notice-error mb-5">
              This link is missing its reset code. Open the link from the email
              exactly as it was sent.
            </div>
            <Link href="/forgot-password" className="btn btn-primary w-full">
              Request a new link
            </Link>
          </div>
        ) : (
          <>
            {error && <div className="notice-error mb-6">{error}</div>}
            <form action={resetPassword} className="card p-6 sm:p-7 grid gap-5">
              <input type="hidden" name="token" value={token} />
              <div>
                <label className="field-label" htmlFor="password">New password</label>
                <input
                  className="field-input"
                  id="password"
                  name="password"
                  type="password"
                  minLength={6}
                  placeholder="6+ characters"
                  required
                />
                <p className="text-[13px] text-muted mt-1.5">At least 6 characters.</p>
              </div>
              <div>
                <label className="field-label" htmlFor="confirm_password">Confirm new password</label>
                <input
                  className="field-input"
                  id="confirm_password"
                  name="confirm_password"
                  type="password"
                  minLength={6}
                  required
                />
              </div>
              <SubmitButton pendingLabel="Saving your password…">Set my new password</SubmitButton>
            </form>
          </>
        )}

        <p className="text-sm text-muted mt-6 text-center">
          <Link href="/login" className="text-accent font-medium hover:underline underline-offset-2">
            Back to log in
          </Link>
        </p>
      </div>
    </div>
  );
}
