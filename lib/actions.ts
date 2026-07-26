"use server";

import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import { api, ApiError } from "./api";
import { createSession, destroySession, getSession } from "./session";

interface TokenResponse {
  accessToken: string;
  user: { id: string; name: string; role: string };
}

function messageOf(e: unknown, fallback: string): string {
  return e instanceof ApiError ? e.message : fallback;
}

// ---------- Auth ----------

export async function signup(formData: FormData) {
  const payload = {
    name: String(formData.get("name") ?? "").trim(),
    email: String(formData.get("email") ?? "").trim(),
    phone: String(formData.get("phone") ?? "").trim() || null,
    password: String(formData.get("password") ?? ""),
  };
  let res: TokenResponse;
  try {
    res = await api<TokenResponse>("/v1/auth/signup", { method: "POST", body: payload, auth: false });
  } catch (e) {
    redirect("/signup?error=" + encodeURIComponent(messageOf(e, "Signup failed — please try again.")));
  }
  await createSession(res!.accessToken, res!.user);
  redirect("/book");
}

export async function login(formData: FormData) {
  const payload = {
    email: String(formData.get("email") ?? "").trim(),
    password: String(formData.get("password") ?? ""),
  };
  let res: TokenResponse;
  try {
    res = await api<TokenResponse>("/v1/auth/login", { method: "POST", body: payload, auth: false });
  } catch (e) {
    redirect("/login?error=" + encodeURIComponent(messageOf(e, "Email or password didn't match.")));
  }
  await createSession(res!.accessToken, res!.user);
  redirect("/trips");
}

export async function logout() {
  await destroySession();
  redirect("/");
}

export async function changePassword(formData: FormData) {
  const session = await getSession();
  if (!session) redirect("/login");

  const next = String(formData.get("new_password") ?? "");
  const confirm = String(formData.get("confirm_password") ?? "");
  if (next !== confirm) {
    redirect("/account?error=" + encodeURIComponent("New passwords don't match."));
  }
  try {
    await api("/v1/users/me/password", {
      method: "PATCH",
      body: { currentPassword: String(formData.get("current_password") ?? ""), newPassword: next },
    });
  } catch (e) {
    redirect("/account?error=" + encodeURIComponent(messageOf(e, "Password change failed.")));
  }
  redirect("/account?ok=1");
}

// ---------- Bookings ----------

export async function createTripRequest(formData: FormData) {
  const session = await getSession();
  if (!session) redirect("/login");

  const passengerNames = String(formData.get("passenger_names") ?? "").trim();
  const notes = String(formData.get("notes") ?? "").trim();
  const groupId = String(formData.get("group_id") ?? "").trim();

  const payload = {
    routeId: String(formData.get("route_id") ?? ""),
    groupId: groupId || null,
    travelDate: String(formData.get("travel_date") ?? ""),
    flightNo: String(formData.get("flight_no") ?? "").trim() || null,
    partySize: Math.max(1, Math.min(6, Number(formData.get("num_people") ?? 1))),
    matchPref: "GROUP", // the product default: cheaper together; riders coordinate in the group
    intro: passengerNames ? `Travelling: ${passengerNames}` : null,
    contact: String(formData.get("contact") ?? "").trim() || null,
    notes: notes || null,
  };

  try {
    await api("/v1/bookings", { method: "POST", body: payload });
  } catch (e) {
    redirect("/book?error=" + encodeURIComponent(messageOf(e, "Booking failed — please try again.")));
  }
  redirect("/trips?requested=1");
}

export async function cancelOwnRequest(formData: FormData) {
  const session = await getSession();
  if (!session) redirect("/login");
  try {
    await api(`/v1/bookings/${String(formData.get("id"))}`, { method: "DELETE" });
  } catch {
    // already cancelled or gone — the refreshed page reflects reality
  }
  revalidatePath("/trips");
}

// ---------- Group page ----------

export async function postGroupMessage(formData: FormData) {
  const session = await getSession();
  if (!session) redirect("/login");
  const groupId = String(formData.get("group_id"));
  const body = String(formData.get("body") ?? "").trim();
  if (body) {
    try {
      await api(`/v1/groups/${groupId}/messages`, { method: "POST", body: { body } });
    } catch (e) {
      redirect(`/groups/${groupId}?error=` + encodeURIComponent(messageOf(e, "Message failed.")));
    }
  }
  revalidatePath(`/groups/${groupId}`);
}

export async function updateTravelDate(formData: FormData) {
  const session = await getSession();
  if (!session) redirect("/login");
  const groupId = String(formData.get("group_id"));
  const bookingId = String(formData.get("booking_id"));
  let updated: { groupId: string | null };
  try {
    updated = await api<{ groupId: string | null }>(`/v1/bookings/${bookingId}`, {
      method: "PATCH",
      body: { travelDate: String(formData.get("travel_date")) },
    });
  } catch (e) {
    redirect(`/groups/${groupId}?error=` + encodeURIComponent(messageOf(e, "Date change failed.")));
  }
  if (!updated!.groupId) {
    // The new date lands in a different week — membership ended at the
    // boundary; send them straight to that week's groups.
    redirect(`/trips/${bookingId}/group?moved=1`);
  }
  revalidatePath(`/groups/${groupId}`);
}

export async function selectGroup(formData: FormData) {
  const session = await getSession();
  if (!session) redirect("/login");
  const bookingId = String(formData.get("booking_id"));
  const groupId = String(formData.get("group_id") ?? "").trim() || null;
  let result: { groupId: string | null };
  try {
    result = await api<{ groupId: string | null }>(`/v1/bookings/${bookingId}/group`, {
      method: "PUT",
      body: { groupId },
    });
  } catch (e) {
    redirect(`/trips/${bookingId}/group?error=` + encodeURIComponent(messageOf(e, "Joining failed.")));
  }
  redirect(`/groups/${result!.groupId}?joined=1`);
}

export async function leaveGroup(formData: FormData) {
  const session = await getSession();
  if (!session) redirect("/login");
  try {
    await api(`/v1/groups/${String(formData.get("group_id"))}/members/me`, { method: "DELETE" });
  } catch (e) {
    redirect("/trips?error=" + encodeURIComponent(messageOf(e, "Leaving failed.")));
  }
  redirect("/trips?left=1");
}
