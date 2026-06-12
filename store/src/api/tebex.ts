// Thin client for the Tebex Headless API (https://docs.tebex.io/developers/headless-api/overview).
// The webstore token is public — it only allows browsing the store and managing
// anonymous baskets. Checkout itself always happens on Tebex's hosted page.

const BASE = "https://headless.tebex.io/api";
const TOKEN = import.meta.env.VITE_TEBEX_WEBSTORE_TOKEN as string;

export interface TebexPackage {
  id: number;
  name: string;
  description: string;
  image: string | null;
  type: "single" | "subscription";
  base_price: number;
  total_price: number;
  currency: string;
  disable_quantity: boolean;
}

export interface TebexCategory {
  id: number;
  name: string;
  description: string;
  order: number;
  packages: TebexPackage[];
}

export interface BasketRow {
  id: number;
  name: string;
  quantity: number;
  image: string | null;
  price: number;
}

export interface Basket {
  ident: string;
  complete: boolean;
  username: string | null;
  base_price: number;
  total_price: number;
  currency: string;
  packages: BasketRow[];
  links: { checkout: string };
}

class TebexError extends Error {}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.detail ?? body.message ?? detail;
    } catch {
      /* non-JSON error body */
    }
    throw new TebexError(detail);
  }
  const body = await res.json();
  return body.data as T;
}

export function getCategories(): Promise<TebexCategory[]> {
  return request(`/accounts/${TOKEN}/categories?includePackages=1`);
}

// Minecraft stores need a username so Tebex knows which player to deliver to.
export function createBasket(username: string): Promise<Basket> {
  return request(`/accounts/${TOKEN}/baskets`, {
    method: "POST",
    body: JSON.stringify({
      username,
      complete_url: `${window.location.origin}/?checkout=complete`,
      cancel_url: `${window.location.origin}/?checkout=cancelled`,
      complete_auto_redirect: true,
    }),
  });
}

export function getBasket(ident: string): Promise<Basket> {
  return request(`/accounts/${TOKEN}/baskets/${ident}`);
}

export function addPackage(ident: string, packageId: number, quantity = 1): Promise<Basket> {
  return request(`/baskets/${ident}/packages`, {
    method: "POST",
    body: JSON.stringify({ package_id: packageId, quantity }),
  });
}

export function removePackage(ident: string, packageId: number): Promise<Basket> {
  return request(`/baskets/${ident}/packages/remove`, {
    method: "POST",
    body: JSON.stringify({ package_id: packageId }),
  });
}

export function formatPrice(amount: number, currency: string): string {
  return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(amount);
}
