import Link from "next/link";
import { login } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const { error } = await searchParams;

  return (
    <div>
      <PageHeader
        script="Welcome back!"
        title="Log in to your account"
        subtitle="Check on your trips or book the next pickup."
      />
      <div className="mx-auto max-w-md px-5 py-12">
        {error && <div className="notice-error mb-6">{error}</div>}

        <form action={login} className="card p-6 sm:p-7 grid gap-5">
          <div>
            <label className="field-label" htmlFor="email">Email</label>
            <input className="field-input" id="email" name="email" type="email" placeholder="you@example.com" required />
          </div>
          <div>
            <label className="field-label" htmlFor="password">Password</label>
            <input className="field-input" id="password" name="password" type="password" required />
          </div>
          <button className="btn btn-primary w-full mt-1">Log in</button>
        </form>

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
