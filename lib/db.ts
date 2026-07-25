import { createClient, type InArgs } from "@libsql/client";
import bcrypt from "bcryptjs";

// Locally this uses a plain SQLite file; in production set TURSO_DATABASE_URL
// and TURSO_AUTH_TOKEN and the same code talks to Turso.
const url = process.env.TURSO_DATABASE_URL ?? "file:data/app.db";

if (url.startsWith("file:")) {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const fs = require("fs") as typeof import("fs");
  if (!fs.existsSync("data")) fs.mkdirSync("data", { recursive: true });
}

const client = createClient({
  url,
  authToken: process.env.TURSO_AUTH_TOKEN,
});

const SCHEMA = [
  `CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    phone TEXT,
    is_admin INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  )`,
  `CREATE TABLE IF NOT EXISTS routes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    from_location TEXT NOT NULL,
    to_location TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1
  )`,
  `CREATE TABLE IF NOT EXISTS price_tiers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    route_id INTEGER NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    group_size INTEGER NOT NULL,
    price_per_person INTEGER NOT NULL,
    UNIQUE(route_id, group_size)
  )`,
  `CREATE TABLE IF NOT EXISTS drivers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT,
    license_no TEXT,
    owns_vehicle INTEGER NOT NULL DEFAULT 0,
    vehicle TEXT,
    seats INTEGER NOT NULL DEFAULT 4,
    active INTEGER NOT NULL DEFAULT 1
  )`,
  `CREATE TABLE IF NOT EXISTS trip_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    route_id INTEGER NOT NULL REFERENCES routes(id),
    travel_date TEXT NOT NULL,
    flight_no TEXT,
    num_people INTEGER NOT NULL,
    passenger_names TEXT,
    contact TEXT,
    notes TEXT,
    status TEXT NOT NULL DEFAULT 'pending',
    driver_id INTEGER REFERENCES drivers(id),
    price_per_person INTEGER NOT NULL,
    total_price INTEGER NOT NULL,
    driver_fee INTEGER,
    vehicle_cost INTEGER,
    other_cost INTEGER,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  )`,
];

let ready: Promise<void> | null = null;

function init(): Promise<void> {
  if (!ready) {
    ready = (async () => {
      await client.batch(SCHEMA, "write");

      const users = await client.execute("SELECT COUNT(*) AS c FROM users");
      if (Number(users.rows[0].c) > 0) return;

      // Seed an empty database: admin account + two starter routes with tiers.
      await client.execute({
        sql: "INSERT INTO users (name, email, password_hash, is_admin) VALUES (?, ?, ?, 1)",
        args: ["Admin", "admin@pickupdrop.com", bcrypt.hashSync("admin123", 10)],
      });

      const seoul = await client.execute({
        sql: "INSERT INTO routes (from_location, to_location) VALUES (?, ?)",
        args: ["Incheon Airport (ICN)", "Seoul"],
      });
      const daejeon = await client.execute({
        sql: "INSERT INTO routes (from_location, to_location) VALUES (?, ?)",
        args: ["Incheon Airport (ICN)", "Daejeon"],
      });

      const tiers: [ReturnType<typeof Number>, number, number][] = [];
      const seoulId = Number(seoul.lastInsertRowid);
      const daejeonId = Number(daejeon.lastInsertRowid);
      for (const [size, price] of [[1, 25000], [2, 20000], [3, 20000], [4, 16000], [5, 14000], [6, 12500]]) {
        tiers.push([seoulId, size, price]);
      }
      for (const [size, price] of [[1, 40000], [2, 32000], [3, 28000], [4, 24000], [5, 21000], [6, 19000]]) {
        tiers.push([daejeonId, size, price]);
      }
      await client.batch(
        tiers.map(([routeId, size, price]) => ({
          sql: "INSERT INTO price_tiers (route_id, group_size, price_per_person) VALUES (?, ?, ?)",
          args: [routeId, size, price],
        })),
        "write"
      );
    })();
  }
  return ready;
}

// ---- Generic query helpers ----

export async function all<T>(sql: string, args: InArgs = []): Promise<T[]> {
  await init();
  const rs = await client.execute({ sql, args });
  // libsql Row objects aren't plain objects, which React refuses to pass
  // from Server to Client Components — convert them.
  return rs.rows.map((row) =>
    Object.fromEntries(rs.columns.map((c, i) => [c, row[i]]))
  ) as T[];
}

export async function get<T>(sql: string, args: InArgs = []): Promise<T | undefined> {
  const rows = await all<T>(sql, args);
  return rows[0];
}

export async function run(
  sql: string,
  args: InArgs = []
): Promise<{ lastInsertRowid: number }> {
  await init();
  const rs = await client.execute({ sql, args });
  return { lastInsertRowid: Number(rs.lastInsertRowid ?? 0) };
}

// ---- Typed rows ----

export type Route = {
  id: number;
  from_location: string;
  to_location: string;
  active: number;
};

export type PriceTier = {
  id: number;
  route_id: number;
  group_size: number;
  price_per_person: number;
};

export type Driver = {
  id: number;
  name: string;
  phone: string | null;
  license_no: string | null;
  owns_vehicle: number;
  vehicle: string | null;
  seats: number;
  active: number;
};

// ---- Domain helpers ----

export function getActiveRoutes(): Promise<Route[]> {
  return all<Route>("SELECT * FROM routes WHERE active = 1 ORDER BY id");
}

export function getAllRoutes(): Promise<Route[]> {
  return all<Route>("SELECT * FROM routes ORDER BY id");
}

export function getTiersForRoute(routeId: number): Promise<PriceTier[]> {
  return all<PriceTier>(
    "SELECT * FROM price_tiers WHERE route_id = ? ORDER BY group_size",
    [routeId]
  );
}

export function getAllTiers(): Promise<PriceTier[]> {
  return all<PriceTier>("SELECT * FROM price_tiers ORDER BY route_id, group_size");
}

/** Price per person for a group of `n` on a route: exact tier if present,
 *  otherwise the largest tier at or below `n` (bigger groups keep the best rate). */
export async function priceForGroup(routeId: number, n: number): Promise<number | null> {
  const tiers = await getTiersForRoute(routeId);
  if (tiers.length === 0) return null;
  let match: PriceTier | null = null;
  for (const t of tiers) {
    if (t.group_size <= n) match = t;
  }
  return (match ?? tiers[0]).price_per_person;
}

export function getActiveDrivers(): Promise<Driver[]> {
  return all<Driver>("SELECT * FROM drivers WHERE active = 1 ORDER BY name");
}

export function getAllDrivers(): Promise<Driver[]> {
  return all<Driver>("SELECT * FROM drivers ORDER BY active DESC, name");
}
