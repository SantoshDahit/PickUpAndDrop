const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export function getToken(): string | null {
  return localStorage.getItem('lg_admin_token');
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem('lg_admin_token', token);
  else localStorage.removeItem('lg_admin_token');
}

export class ApiError extends Error {
  status: number;
  errorCode: string;
  constructor(status: number, message: string, errorCode: string) {
    super(message);
    this.status = status;
    this.errorCode = errorCode;
  }
}

export async function api<T = unknown>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(BASE + path, { ...options, headers });
  if (res.status === 401) {
    setToken(null);
    window.location.href = '/login';
    throw new ApiError(401, 'Session expired', 'CMN_UA_001');
  }
  if (res.status === 204) return undefined as T;
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(res.status, body?.message ?? `Request failed (${res.status})`, body?.errorCode ?? '');
  }
  return body as T;
}

// ---- types (mirror the API DTOs we use) ----

export interface Route { id: string; fromLocation: string; toLocation: string }
export interface DriverPublic { name: string; phone: string | null; vehicle: string | null; plateNo: string | null; seats: number }
export interface BookingSummary {
  id: string; route: Route | null; groupId: string | null; travelDate: string;
  flightNo: string | null; partySize: number; matchPref: 'GROUP' | 'INDIVIDUAL';
  status: 'ACTIVE' | 'CANCELLED'; driver: DriverPublic | null; createdAt: string;
}
export interface Driver {
  id: string; name: string; phone: string | null; licenseNo: string | null; ownsVehicle: boolean;
  vehicle: string | null; plateNo: string | null; seats: number; status: 'ACTIVE' | 'INACTIVE';
}
export interface Page<T> { content: T[]; page: { size: number; number: number; totalElements: number; totalPages: number } }
export interface GroupView {
  id: string; route: Route; status: string; agreedDate: string | null; driver: DriverPublic | null;
  members: { firstName: string; partySize: number; travelDate: string; intro: string | null; me: boolean }[];
}
