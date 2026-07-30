import Link from "next/link";
import { requestPasswordReset } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";
import SubmitButton from "@/components/SubmitButton";

export default async function ForgotPasswordPage({
  searchParams,
}: {
  searchParams: Promise<{ sent?: string }>;
}) {
  const { sent } = await searchParams;

  return (
    <div>
      <PageHeader
        script="No problem"
        title="Reset your password"
        subtitle="We'll email you a link to choose a new one."
      />
      <div className="mx-auto max-w-md px-5 py-12">
        {sent ? (
          <div className="card p-6 sm:p-7">
            <div className="notice-ok mb-5">
              If that email has an account, a reset link is on its way. The link is
              good for one hour.
            </div>
            <p className="text-sm text-muted">
              Nothing arrived? Check your spam folder, or{" "}
              <Link href="/forgot-password" className="text-accent font-medium hover:underline underline-offset-2">
                try a different email
              </Link>
              .
            </p>
          </div>
        ) : (
          <form action={requestPasswordReset} className="card p-6 sm:p-7 grid gap-5">
            <div>
              <label className="field-label" htmlFor="email">Email</label>
              <input
                className="field-input"
                id="email"
                name="email"
                type="email"
                placeholder="you@example.com"
                required
              />
            </div>
            <SubmitButton pendingLabel="Sending the link…">Email me a reset link</SubmitButton>
          </form>
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
