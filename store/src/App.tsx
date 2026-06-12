import { useCallback, useEffect, useState } from "react";
import {
  addPackage,
  createBasket,
  formatPrice,
  getBasket,
  getCategories,
  removePackage,
  type Basket,
  type TebexCategory,
  type TebexPackage,
} from "./api/tebex";

const BASKET_KEY = "tebex_basket_ident";
const USERNAME_KEY = "tebex_username";

export default function App() {
  const [categories, setCategories] = useState<TebexCategory[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [basket, setBasket] = useState<Basket | null>(null);
  const [username, setUsername] = useState(() => localStorage.getItem(USERNAME_KEY) ?? "");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(() => {
    const status = new URLSearchParams(window.location.search).get("checkout");
    if (status === "complete") return "Thanks for your purchase! Your items will arrive in game shortly.";
    if (status === "cancelled") return "Checkout cancelled — your basket is still here.";
    return null;
  });

  useEffect(() => {
    getCategories()
      .then((cats) => setCategories([...cats].sort((a, b) => a.order - b.order)))
      .catch((e: Error) => setError(`Couldn't load the store: ${e.message}`));

    const ident = localStorage.getItem(BASKET_KEY);
    if (ident) {
      getBasket(ident)
        .then((b) => (b.complete ? localStorage.removeItem(BASKET_KEY) : setBasket(b)))
        .catch(() => localStorage.removeItem(BASKET_KEY));
    }
  }, []);

  const ensureBasket = useCallback(async (): Promise<Basket> => {
    if (basket) return basket;
    const name = username.trim();
    if (!name) throw new Error("Enter your Minecraft username first.");
    const created = await createBasket(name);
    localStorage.setItem(BASKET_KEY, created.ident);
    localStorage.setItem(USERNAME_KEY, name);
    setBasket(created);
    return created;
  }, [basket, username]);

  const withBusy = useCallback(async (work: () => Promise<void>) => {
    setBusy(true);
    setNotice(null);
    try {
      await work();
    } catch (e) {
      setNotice(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }, []);

  const handleAdd = (pkg: TebexPackage) =>
    withBusy(async () => {
      const b = await ensureBasket();
      setBasket(await addPackage(b.ident, pkg.id));
    });

  const handleRemove = (packageId: number) =>
    withBusy(async () => {
      if (!basket) return;
      setBasket(await removePackage(basket.ident, packageId));
    });

  return (
    <div className="layout">
      <header>
        <h1>Server Store</h1>
        <p className="tagline">Support the server and get awesome perks</p>
      </header>

      {notice && <div className="notice">{notice}</div>}
      {error && <div className="notice error">{error}</div>}

      <main>
        <section className="catalog">
          {categories === null && !error && <p className="muted">Loading packages…</p>}
          {categories?.map((cat) => (
            <div key={cat.id} className="category">
              <h2>{cat.name}</h2>
              <div className="packages">
                {cat.packages.map((pkg) => (
                  <article key={pkg.id} className="package">
                    {pkg.image && <img src={pkg.image} alt="" />}
                    <h3>{pkg.name}</h3>
                    <div
                      className="description"
                      dangerouslySetInnerHTML={{ __html: pkg.description }}
                    />
                    <div className="package-footer">
                      <span className="price">{formatPrice(pkg.total_price, pkg.currency)}</span>
                      <button disabled={busy} onClick={() => handleAdd(pkg)}>
                        Add to basket
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          ))}
        </section>

        <aside className="basket">
          <h2>Basket</h2>
          {!basket && (
            <label className="username">
              Minecraft username
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Steve"
                maxLength={16}
              />
              <span className="muted">Needed so we know who to deliver to.</span>
            </label>
          )}
          {basket && basket.username && (
            <p className="muted">
              Delivering to <strong>{basket.username}</strong>
            </p>
          )}
          {basket && basket.packages.length > 0 ? (
            <>
              <ul>
                {basket.packages.map((row) => (
                  <li key={row.id}>
                    <span>
                      {row.name}
                      {row.quantity > 1 ? ` ×${row.quantity}` : ""}
                    </span>
                    <span>
                      {formatPrice(row.price, basket.currency)}{" "}
                      <button
                        className="remove"
                        disabled={busy}
                        onClick={() => handleRemove(row.id)}
                        aria-label={`Remove ${row.name}`}
                      >
                        ✕
                      </button>
                    </span>
                  </li>
                ))}
              </ul>
              <div className="total">
                <span>Total</span>
                <span>{formatPrice(basket.total_price, basket.currency)}</span>
              </div>
              <a className="checkout" href={basket.links.checkout}>
                Checkout securely with Tebex
              </a>
            </>
          ) : (
            <p className="muted">Your basket is empty.</p>
          )}
        </aside>
      </main>

      <footer>
        <p className="muted">
          Payments are processed by Tebex. This store is not affiliated with Mojang or Microsoft.
        </p>
      </footer>
    </div>
  );
}
