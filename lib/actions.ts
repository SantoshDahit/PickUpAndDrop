"use server";

import bcrypt from "bcryptjs";
import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import { get, run, priceForGroup } from "./db";
import { createSession, destroySession, getSession } from "./session";

// ---------- Auth ----------

export async function signup(formData: FormData) {
  const name = String(formData.get("name") ?? "").trim();
  const email = String(formData.get("email") ?? "").trim().toLowerCase();
  const phone = String(formData.get("phone") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  if (!name || !email || password.length < 6) {
    redirect("/signup?error=" + encodeURIComponent("Please fill every field. Password needs 6+ characters."));
  }

  const existing = await get("SELECT id FROM users WHERE email = ?", [email]);
  if (existing) {
    redirect("/signup?error=" + encodeURIComponent("That email is already registered. Try logging in."));
  }

  const result = await run(
    "INSERT INTO users (name, email, phone, password_hash) VALUES (?, ?, ?, ?)",
    [name, email, phone, bcrypt.hashSync(password, 10)]
  );

  await createSession({ uid: result.lastInsertRowid, name, isAdmin: false });
  redirect("/book");
}

export async function login(formData: FormData) {
  const email = String(formData.get("email") ?? "").trim().toLowerCase();
  const password = String(formData.get("password") ?? "");

  const user = await get<{ id: number; name: string; password_hash: string; is_admin: number }>(
    "SELECT id, name, password_hash, is_admin FROM users WHERE email = ?",
    [email]
  );

  if (!user || !bcrypt.compareSync(password, user.password_hash)) {
    redirect("/login?error=" + encodeURIComponent("Email or password didn't match."));
  }

  await createSession({ uid: user!.id, name: user!.name, isAdmin: user!.is_admin === 1 });
  redirect(user!.is_admin === 1 ? "/admin" : "/trips");
}

export async function logout() {
  await destroySession();
  redirect("/");
}

// ---------- Trip requests ----------

export async function createTripRequest(formData: FormData) {
  const session = await getSession();
  if (!session) redirect("/login");

  const routeId = Number(formData.get("route_id"));
  const travelDate = String(formData.get("travel_date") ?? "");
  const flightNo = String(formData.get("flight_no") ?? "").trim();
  const numPeople = Math.max(1, Math.min(12, Number(formData.get("num_people") ?? 1)));
  const passengerNames = String(formData.get("passenger_names") ?? "").trim();
  const contact = String(formData.get("contact") ?? "").trim();
  const notes = String(formData.get("notes") ?? "").trim();

  if (!routeId || !travelDate) {
    redirect("/book?error=" + encodeURIComponent("Please choose a route and travel date."));
  }

  const perPerson = await priceForGroup(routeId, numPeople);
  if (perPerson === null) {
    redirect("/book?error=" + encodeURIComponent("This route has no pricing yet. Please contact us."));
  }

  await run(
    `INSERT INTO trip_requests
       (user_id, route_id, travel_date, flight_no, num_people, passenger_names, contact, notes, price_per_person, total_price)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      session!.uid,
      routeId,
      travelDate,
      flightNo,
      numPeople,
      passengerNames,
      contact,
      notes,
      perPerson!,
      perPerson! * numPeople,
    ]
  );

  redirect("/trips?requested=1");
}

export async function cancelOwnRequest(formData: FormData) {
  const session = await getSession();
  if (!session) redirect("/login");
  const id = Number(formData.get("id"));
  await run(
    "UPDATE trip_requests SET status = 'cancelled' WHERE id = ? AND user_id = ? AND status = 'pending'",
    [id, session!.uid]
  );
  revalidatePath("/trips");
}

// ---------- Admin ----------

async function requireAdmin() {
  const session = await getSession();
  if (!session?.isAdmin) redirect("/login");
  return session!;
}

export async function addRoute(formData: FormData) {
  await requireAdmin();
  const from = String(formData.get("from_location") ?? "").trim();
  const to = String(formData.get("to_location") ?? "").trim();
  if (from && to) {
    await run("INSERT INTO routes (from_location, to_location) VALUES (?, ?)", [from, to]);
  }
  revalidatePath("/admin/routes");
}

export async function toggleRoute(formData: FormData) {
  await requireAdmin();
  const id = Number(formData.get("id"));
  await run("UPDATE routes SET active = 1 - active WHERE id = ?", [id]);
  revalidatePath("/admin/routes");
}

export async function setTier(formData: FormData) {
  await requireAdmin();
  const routeId = Number(formData.get("route_id"));
  const groupSize = Number(formData.get("group_size"));
  const price = Number(formData.get("price_per_person"));
  if (routeId && groupSize >= 1 && price > 0) {
    await run(
      `INSERT INTO price_tiers (route_id, group_size, price_per_person) VALUES (?, ?, ?)
       ON CONFLICT(route_id, group_size) DO UPDATE SET price_per_person = excluded.price_per_person`,
      [routeId, groupSize, price]
    );
  }
  revalidatePath("/admin/routes");
}

export async function deleteTier(formData: FormData) {
  await requireAdmin();
  const id = Number(formData.get("id"));
  await run("DELETE FROM price_tiers WHERE id = ?", [id]);
  revalidatePath("/admin/routes");
}

export async function addDriver(formData: FormData) {
  await requireAdmin();
  const name = String(formData.get("name") ?? "").trim();
  const phone = String(formData.get("phone") ?? "").trim();
  const licenseNo = String(formData.get("license_no") ?? "").trim();
  const ownsVehicle = formData.get("owns_vehicle") ? 1 : 0;
  const vehicle = String(formData.get("vehicle") ?? "").trim();
  const seats = Math.max(1, Number(formData.get("seats") ?? 4));
  if (name) {
    await run(
      "INSERT INTO drivers (name, phone, license_no, owns_vehicle, vehicle, seats) VALUES (?, ?, ?, ?, ?, ?)",
      [name, phone, licenseNo, ownsVehicle, vehicle, seats]
    );
  }
  revalidatePath("/admin/drivers");
}

export async function toggleDriver(formData: FormData) {
  await requireAdmin();
  const id = Number(formData.get("id"));
  await run("UPDATE drivers SET active = 1 - active WHERE id = ?", [id]);
  revalidatePath("/admin/drivers");
}

export async function updateRequest(formData: FormData) {
  await requireAdmin();
  const id = Number(formData.get("id"));
  const status = String(formData.get("status") ?? "pending");
  const driverIdRaw = formData.get("driver_id");
  const driverId = driverIdRaw && Number(driverIdRaw) > 0 ? Number(driverIdRaw) : null;
  const driverFee = formData.get("driver_fee") ? Number(formData.get("driver_fee")) : null;
  const vehicleCost = formData.get("vehicle_cost") ? Number(formData.get("vehicle_cost")) : null;
  const otherCost = formData.get("other_cost") ? Number(formData.get("other_cost")) : null;

  if (!["pending", "confirmed", "completed", "cancelled"].includes(status)) return;

  await run(
    `UPDATE trip_requests
       SET status = ?, driver_id = ?, driver_fee = ?, vehicle_cost = ?, other_cost = ?
     WHERE id = ?`,
    [status, driverId, driverFee, vehicleCost, otherCost, id]
  );

  revalidatePath("/admin");
}
