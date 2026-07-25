import Link from "next/link";
import { login } from "@/lib/actions";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const { error } = await searchParams;

  return (
    <div className="mx-auto max-w-md px-5 py-16">
      <p className="eyebrow mb-3">Log in</p>
      <h1 className="text-3xl mb-2">Welcome back.</h1>
      <p className="text-ink-soft mb-8">Check on your trips or book the next pickup.</p>

      {error && <div className="notice-error mb-6">{error}</div>}

      <form action={login} className="card p-6 grid gap-5">
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
        <Link href="/signup" className="text-ink font-medium underline underline-offset-2">
          Create an account
        </Link>
      </p>
    </div>
  );
}
