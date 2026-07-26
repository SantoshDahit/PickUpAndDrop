import { cookies } from "next/headers";

export type Session = {
  uid: string;
  name: string;
  isAdmin: boolean;
};

const TOKEN_COOKIE = "pud_token";
const USER_COOKIE = "pud_user";

const COOKIE_OPTS = {
  httpOnly: true,
  sameSite: "lax" as const,
  maxAge: 60 * 60 * 3, // matches the API access-token lifetime
  path: "/",
};

export async function createSession(token: string, user: { id: string; name: string; role: string }) {
  const store = await cookies();
  store.set(TOKEN_COOKIE, token, COOKIE_OPTS);
  store.set(USER_COOKIE, JSON.stringify({ uid: user.id, name: user.name, isAdmin: user.role === "ADMIN" }), COOKIE_OPTS);
}

export async function getSession(): Promise<Session | null> {
  const raw = (await cookies()).get(USER_COOKIE)?.value;
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Session;
  } catch {
    return null;
  }
}

export async function destroySession() {
  const store = await cookies();
  store.delete(TOKEN_COOKIE);
  store.delete(USER_COOKIE);
}
