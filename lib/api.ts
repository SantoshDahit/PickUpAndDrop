import { cookies } from "next/headers";

const BASE = process.env.API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  errorCode: string;
  constructor(status: number, message: string, errorCode: string) {
    super(message);
    this.status = status;
    this.errorCode = errorCode;
  }
}

export async function api<T = unknown>(
  path: string,
  options: { method?: string; body?: unknown; auth?: boolean } = {}
): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (options.auth !== false) {
    const token = (await cookies()).get("pud_token")?.value;
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }
  const res = await fetch(BASE + path, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    cache: "no-store",
  });
  if (res.status === 204) return undefined as T;
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(res.status, body?.message ?? `Request failed (${res.status})`, body?.errorCode ?? "");
  }
  return body as T;
}

// ---- shapes the design's components already use (snake_case preserved) ----

export type Route = { id: string; from_location: string; to_location: string };
export type PriceTier = { route_id: string; group_size: number; price_per_person: number };

interface ApiRoute {
  id: string;
  fromLocation: string;
  toLocation: string;
  tiers: { groupSize: number; pricePerPerson: number }[];
}

/** Routes + flattened tiers in the shape FareCalculator/BookingForm expect. */
export async function getRoutesWithTiers(): Promise<{ routes: Route[]; tiers: PriceTier[] }> {
  const data = await api<ApiRoute[]>("/v1/routes", { auth: false });
  return {
    routes: data.map((r) => ({ id: r.id, from_location: r.fromLocation, to_location: r.toLocation })),
    tiers: data.flatMap((r) =>
      r.tiers.map((t) => ({ route_id: r.id, group_size: t.groupSize, price_per_person: t.pricePerPerson }))
    ),
  };
}

export function priceFor(tiers: PriceTier[], routeId: string, n: number): number | null {
  const routeTiers = tiers.filter((t) => t.route_id === routeId);
  if (routeTiers.length === 0) return null;
  let match: PriceTier | null = null;
  for (const t of routeTiers) {
    if (t.group_size <= n) match = t;
  }
  return (match ?? routeTiers[0]).price_per_person;
}

// ---- API response types used by pages ----

export interface DriverPublic {
  name: string;
  phone: string | null;
  vehicle: string | null;
  plateNo: string | null;
  seats: number;
}

export interface Booking {
  id: string;
  route: { id: string; fromLocation: string; toLocation: string } | null;
  groupId: string | null;
  travelDate: string;
  flightNo: string | null;
  partySize: number;
  matchPref: "GROUP" | "INDIVIDUAL";
  status: "ACTIVE" | "CANCELLED";
  driver: DriverPublic | null;
  createdAt: string;
}

export interface OpenRide {
  id: string;
  route: { id: string; fromLocation: string; toLocation: string };
  targetDate: string;
  memberCount: number;
  seatsLeft: number;
}

export interface GroupSuggestions {
  weekStart: string;
  weekEnd: string;
  groups: {
    id: string; memberCount: number; seatsLeft: number;
    earliestDate: string | null; latestDate: string | null;
    official: boolean; targetDate: string | null;
  }[];
}

export interface GroupView {
  id: string;
  route: { fromLocation: string; toLocation: string };
  status: string;
  agreedDate: string | null;
  driver: DriverPublic | null;
  weekStart: string | null;
  weekEnd: string | null;
  members: { firstName: string; partySize: number; travelDate: string; intro: string | null; me: boolean }[];
}

export type ServiceType = "SIM_CARD";
export type ServiceRequestStatus = "REQUESTED" | "CONFIRMED" | "DELIVERED" | "CANCELLED";

export interface ServiceRequest {
  id: string;
  type: ServiceType;
  status: ServiceRequestStatus;
  arrivalDate: string | null;
  airport: string | null;
  detail: string | null;
  deliverTo: string | null;
  contact: string | null;
  notes: string | null;
  createdAt: string;
}

export interface Message {
  id: string;
  authorFirstName: string;
  body: string;
  mine: boolean;
  /** Posted by the operator — shown as the team name with an "Official" badge. */
  staff: boolean;
  createdAt: string;
}
