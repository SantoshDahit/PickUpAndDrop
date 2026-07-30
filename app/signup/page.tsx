import Link from "next/link";
import { signup } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";
import SubmitButton from "@/components/SubmitButton";

export default async function SignupPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; taken?: string; name?: string; email?: string; phone?: string }>;
}) {
  // On an error the action sends back what was typed (never the password) so a
  // failed attempt doesn't wipe the form and look like the button did nothing.
  const { error, taken, name, email, phone } = await searchParams;

  return (
    <div>
      <PageHeader
        script="Welcome aboard!"
        title="Create your account"
        subtitle="One account per group is enough — you can book seats for everyone travelling with you."
      />
      <div className="mx-auto max-w-md px-5 py-12">
        {error && (
          <div className="notice-error mb-6">
            {error}
            {taken && (
              <>
                {" "}
                <Link href="/login" className="font-medium underline underline-offset-2">
                  Log in instead
                </Link>
                {" · "}
                <Link href="/forgot-password" className="font-medium underline underline-offset-2">
                  Forgot your password?
                </Link>
              </>
            )}
          </div>
        )}

        <form action={signup} className="card p-6 sm:p-7 grid gap-5">
          <div>
            <label className="field-label" htmlFor="name">Full name</label>
            <input className="field-input" id="name" name="name" placeholder="Sita Sharma" defaultValue={name ?? ""} required />
          </div>
          <div>
            <label className="field-label" htmlFor="email">Email</label>
            <input className="field-input" id="email" name="email" type="email" placeholder="you@example.com" defaultValue={email ?? ""} required />
          </div>
          <div>
            <label className="field-label" htmlFor="phone">Phone / WhatsApp / Viber</label>
            <input className="field-input" id="phone" name="phone" placeholder="+977 98…" defaultValue={phone ?? ""} maxLength={30} />
          </div>
          <div>
            <label className="field-label" htmlFor="password">Password</label>
            <input className="field-input" id="password" name="password" type="password" minLength={6} placeholder="6+ characters" required />
            <p className="text-[13px] text-muted mt-1.5">At least 6 characters.</p>
          </div>
          <SubmitButton pendingLabel="Creating your account…">Create my account</SubmitButton>
        </form>

        <p className="text-sm text-muted mt-6 text-center">
          Already have an account?{" "}
          <Link href="/login" className="text-accent font-medium hover:underline underline-offset-2">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
