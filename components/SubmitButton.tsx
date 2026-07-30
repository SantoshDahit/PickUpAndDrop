"use client";

import { useFormStatus } from "react-dom";

/**
 * Submit button that reports the action is running. Without this a click on a
 * slow action looks like nothing happened, which reads as a broken button.
 */
export default function SubmitButton({
  children,
  pendingLabel,
  className = "btn btn-primary w-full mt-1",
}: {
  children: React.ReactNode;
  pendingLabel?: string;
  className?: string;
}) {
  const { pending } = useFormStatus();

  return (
    <button type="submit" className={className} disabled={pending} aria-busy={pending}>
      {pending ? (
        <>
          <span
            aria-hidden
            className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white"
          />
          {pendingLabel ?? "Working…"}
        </>
      ) : (
        children
      )}
    </button>
  );
}
