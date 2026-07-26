const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export interface CurrentUser { id: string; email: string; name: string; phone: string | null; role: string }

export function getToken(): string | null { return localStorage.getItem('pud_token') }
export function getUser(): CurrentUser | null {
  const raw = localStorage.getItem('pud_user')
  return raw ? JSON.parse(raw) : null
}
export function setSession(token: string | null, user: CurrentUser | null) {
  if (token) localStorage.setItem('pud_token', token); else localStorage.removeItem('pud_token')
  if (user) localStorage.setItem('pud_user', JSON.stringify(user)); else localStorage.removeItem('pud_user')
}

export class ApiError extends Error {
  status: number; errorCode: string
  constructor(status: number, message: string, errorCode: string) {
    super(message); this.status = status; this.errorCode = errorCode
  }
}

export async function api<T = unknown>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(BASE + path, { ...options, headers })
  if (res.status === 401) {
    setSession(null, null)
    window.location.href = '/login'
    throw new ApiError(401, 'Session expired', 'CMN_UA_001')
  }
  if (res.status === 204) return undefined as T
  const body = await res.json().catch(() => null)
  if (!res.ok) throw new ApiError(res.status, body?.message ?? `Request failed (${res.status})`, body?.errorCode ?? '')
  return body as T
}

export interface Route { id: string; fromLocation: string; toLocation: string }
export interface DriverPublic { name: string; phone: string | null; vehicle: string | null; plateNo: string | null; seats: number }
export interface Booking {
  id: string; route: Route | null; groupId: string | null; travelDate: string; flightNo: string | null;
  partySize: number; matchPref: 'GROUP' | 'INDIVIDUAL'; status: 'ACTIVE' | 'CANCELLED';
  driver: DriverPublic | null; createdAt: string;
}
export interface OpenRide {
  id: string; route: Route; targetDate: string; memberCount: number; seatsLeft: number;
  earliestDate: string | null; latestDate: string | null;
}
export interface GroupMember { firstName: string; partySize: number; travelDate: string; intro: string | null; me: boolean }
export interface GroupView {
  id: string; route: Route; status: string; agreedDate: string | null; driver: DriverPublic | null; members: GroupMember[];
}
export interface Message { id: string; authorFirstName: string; body: string; mine: boolean; createdAt: string }
