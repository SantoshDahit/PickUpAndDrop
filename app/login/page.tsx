import Link from "next/link";
import { login } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";
import SubmitButton from "@/components/SubmitButton";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; email?: string; reset?: string }>;
}) {
  const { error, email, reset } = await searchParams;

  return (
    <div>
      <PageHeader
        script="Welcome back!"
        title="Log in to your account"
        subtitle="Check on your trips or book the next pickup."
      />
      <div className="mx-auto max-w-md px-5 py-12">
        {reset && <div className="notice-ok mb-6">Your password has been changed — log in with it below.</div>}
        {error && <div className="notice-error mb-6">{error}</div>}

        <form action={login} className="card p-6 sm:p-7 grid gap-5">
          <div>
            <label className="field-label" htmlFor="email">Email</label>
            <input className="field-input" id="email" name="email" type="email" placeholder="you@example.com" defaultValue={email ?? ""} required />
          </div>
          <div>
            <label className="field-label" htmlFor="password">Password</label>
            <input className="field-input" id="password" name="password" type="password" required />
          </div>
          <SubmitButton pendingLabel="Logging you in…">Log in</SubmitButton>
        </form>

        <p className="text-sm text-muted mt-5 text-center">
          <Link href="/forgot-password" className="text-accent font-medium hover:underline underline-offset-2">
            Forgot your password?
          </Link>
        </p>

        <p className="text-sm text-muted mt-6 text-center">
          First time here?{" "}
          <Link href="/signup" className="text-accent font-medium hover:underline underline-offset-2">
            Create an account
          </Link>
        </p>
      </div>
    </div>
  );
}
