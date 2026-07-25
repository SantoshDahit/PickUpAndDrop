import Link from "next/link";
import { signup } from "@/lib/actions";

export default async function SignupPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const { error } = await searchParams;

  return (
    <div className="mx-auto max-w-md px-5 py-16">
      <p className="eyebrow mb-3">Create account</p>
      <h1 className="text-3xl mb-2">Welcome aboard.</h1>
      <p className="text-ink-soft mb-8">
        One account per group is enough — you can book seats for everyone travelling with you.
      </p>

      {error && <div className="notice-error mb-6">{error}</div>}

      <form action={signup} className="card p-6 grid gap-5">
        <div>
          <label className="field-label" htmlFor="name">Full name</label>
          <input className="field-input" id="name" name="name" placeholder="Sita Sharma" required />
        </div>
        <div>
          <label className="field-label" htmlFor="email">Email</label>
          <input className="field-input" id="email" name="email" type="email" placeholder="you@example.com" required />
        </div>
        <div>
          <label className="field-label" htmlFor="phone">Phone / WhatsApp / Viber</label>
          <input className="field-input" id="phone" name="phone" placeholder="+977 98…" />
        </div>
        <div>
          <label className="field-label" htmlFor="password">Password</label>
          <input className="field-input" id="password" name="password" type="password" minLength={6} placeholder="6+ characters" required />
        </div>
        <button className="btn btn-primary w-full mt-1">Create my account</button>
      </form>

      <p className="text-sm text-muted mt-6 text-center">
        Already have an account?{" "}
        <Link href="/login" className="text-ink font-medium underline underline-offset-2">
          Log in
        </Link>
      </p>
    </div>
  );
}
