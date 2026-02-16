const locationsForm = document.getElementById("locationsForm");
const productsForm = document.getElementById("productsForm");

const zipInput = document.getElementById("zip");
const radiusInput = document.getElementById("radius");
const limitInput = document.getElementById("limit");
const fetchLocationsBtn = document.getElementById("fetchLocationsBtn");

const termInput = document.getElementById("term");
const brandInput = document.getElementById("brand");
const productLimitInput = document.getElementById("productLimit");
const searchProductsBtn = document.getElementById("searchProductsBtn");

const locationStatusEl = document.getElementById("locationStatus");
const productStatusEl = document.getElementById("productStatus");

const locationResultCountEl = document.getElementById("locationResultCount");
const selectionCountEl = document.getElementById("selectionCount");
const productResultCountEl = document.getElementById("productResultCount");

const locationCardsEl = document.getElementById("locationCards");
const productGroupsEl = document.getElementById("productGroups");

const locationsRawJsonEl = document.getElementById("locationsRawJson");
const productsRawJsonEl = document.getElementById("productsRawJson");

const selectAllBtn = document.getElementById("selectAllBtn");
const clearSelectionBtn = document.getElementById("clearSelectionBtn");
const cartItemCountEl = document.getElementById("cartItemCount");
const appCartListEl = document.getElementById("appCartList");
const storeTotalsEl = document.getElementById("storeTotals");
const cheapestStoreStatusEl = document.getElementById("cheapestStoreStatus");
const clearCartBtn = document.getElementById("clearCartBtn");
const cartModalityEl = document.getElementById("cartModality");

const connectKrogerBtn = document.getElementById("connectKrogerBtn");
const syncCheapestCartBtn = document.getElementById("syncCheapestCartBtn");
const authStatusEl = document.getElementById("authStatus");
const cartSyncStatusEl = document.getElementById("cartSyncStatus");
const cartSyncRawJsonEl = document.getElementById("cartSyncRawJson");

let fetchedLocations = [];
let selectedLocationIds = new Set();
let appCartItems = [];

function setStatus(element, message, isError = false) {
  element.textContent = message;
  element.classList.toggle("error", isError);
}

function formatCount(value, singular, plural) {
  return `${value} ${value === 1 ? singular : plural}`;
}

function formatMoney(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return "N/A";
  }
  return `$${Number(value).toFixed(2)}`;
}

function selectedLocationCount() {
  return selectedLocationIds.size;
}

function updateSelectionUi() {
  selectionCountEl.textContent = formatCount(selectedLocationCount(), "selected", "selected");
  searchProductsBtn.disabled = selectedLocationCount() === 0 || fetchedLocations.length === 0;
}

function deriveUnitPrice(product) {
  return (
    product.promoPrice ??
    product.regularPrice ??
    product.nationalPromoPrice ??
    product.nationalRegularPrice ??
    null
  );
}

function renderLocationCards(data) {
  locationCardsEl.innerHTML = "";

  if (!Array.isArray(data) || data.length === 0) {
    locationCardsEl.innerHTML = '<p class="empty">No locations returned for this search.</p>';
    updateSelectionUi();
    return;
  }

  for (const item of data) {
    const card = document.createElement("article");
    card.className = "card selectable-card";

    const header = document.createElement("div");
    header.className = "card-header";

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.className = "location-checkbox";
    checkbox.checked = selectedLocationIds.has(item.locationId);
    checkbox.disabled = !item.locationId;
    checkbox.addEventListener("change", () => {
      if (checkbox.checked) {
        selectedLocationIds.add(item.locationId);
      } else {
        selectedLocationIds.delete(item.locationId);
      }
      updateSelectionUi();
    });

    const titleWrap = document.createElement("div");
    const title = document.createElement("h3");
    title.textContent = item.name || "Unnamed Store";
    const idMeta = document.createElement("p");
    idMeta.className = "meta";
    idMeta.textContent = `Location ID: ${item.locationId || "Unavailable"}`;

    titleWrap.append(title, idMeta);
    header.append(checkbox, titleWrap);

    const chain = document.createElement("p");
    chain.className = "meta";
    chain.textContent = item.chain || "Kroger";

    const locationLine = document.createElement("p");
    locationLine.className = "meta";
    locationLine.textContent = [item.city, item.state, item.zipCode].filter(Boolean).join(", ") || "Address unavailable";

    const phone = document.createElement("p");
    phone.className = "meta";
    phone.textContent = item.phone || "No phone";

    const hours = document.createElement("p");
    hours.className = "meta";
    const timezone = item.timezone ? ` (${item.timezone})` : "";
    hours.textContent = `${item.todayHours || "Hours unavailable"}${timezone}`;

    const badges = document.createElement("div");
    badges.className = "badges";

    if (item.pickup) {
      const pickupBadge = document.createElement("span");
      pickupBadge.className = "badge pickup";
      pickupBadge.textContent = "Pickup";
      badges.appendChild(pickupBadge);
    }

    if (item.delivery) {
      const deliveryBadge = document.createElement("span");
      deliveryBadge.className = "badge delivery";
      deliveryBadge.textContent = "Delivery";
      badges.appendChild(deliveryBadge);
    }

    card.append(header, chain, locationLine, phone, hours, badges);
    locationCardsEl.appendChild(card);
  }

  updateSelectionUi();
}

function renderProductGroups(groups) {
  productGroupsEl.innerHTML = "";

  if (!Array.isArray(groups) || groups.length === 0) {
    productGroupsEl.innerHTML = '<p class="empty">No product search has been run yet.</p>';
    return;
  }

  for (const group of groups) {
    const section = document.createElement("section");
    section.className = "group";

    const heading = document.createElement("div");
    heading.className = "group-head";

    const title = document.createElement("h3");
    title.textContent = group.locationName || "Unknown Location";

    const idTag = document.createElement("span");
    idTag.className = "count-pill subtle";
    idTag.textContent = `ID: ${group.locationId}`;

    heading.append(title, idTag);
    section.appendChild(heading);

    if (group.error) {
      const err = document.createElement("p");
      err.className = "error-inline";
      err.textContent = group.error;
      section.appendChild(err);
      productGroupsEl.appendChild(section);
      continue;
    }

    const products = Array.isArray(group.products) ? group.products : [];
    if (products.length === 0) {
      const empty = document.createElement("p");
      empty.className = "empty";
      empty.textContent = "No products returned for this location.";
      section.appendChild(empty);
      productGroupsEl.appendChild(section);
      continue;
    }

    const grid = document.createElement("div");
    grid.className = "cards product-cards";

    for (const product of products) {
      const card = document.createElement("article");
      card.className = "card";

      const productName = document.createElement("h3");
      productName.textContent = product.description || "Unnamed Product";

      const brand = document.createElement("p");
      brand.className = "meta";
      brand.textContent = product.brand ? `Brand: ${product.brand}` : "Brand: N/A";

      const upc = document.createElement("p");
      upc.className = "meta";
      upc.textContent = product.upc ? `UPC: ${product.upc}` : "UPC: N/A";

      const size = document.createElement("p");
      size.className = "meta";
      size.textContent = product.size ? `Size: ${product.size}` : "Size: N/A";

      const price = document.createElement("p");
      price.className = "meta";
      const regular = product.regularPrice ?? "N/A";
      const promo = product.promoPrice ?? "N/A";
      const nationalRegular = product.nationalRegularPrice ?? "N/A";
      const nationalPromo = product.nationalPromoPrice ?? "N/A";
      price.textContent = `Store: ${regular}/${promo} | National: ${nationalRegular}/${nationalPromo}`;

      const unitPrice = deriveUnitPrice(product);
      const unitPriceLine = document.createElement("p");
      unitPriceLine.className = "meta";
      unitPriceLine.textContent = `Effective unit price: ${formatMoney(unitPrice)}`;

      const temp = document.createElement("p");
      temp.className = "meta";
      temp.textContent = product.temperature ? `Temp: ${product.temperature}` : "Temp: N/A";

      const actionRow = document.createElement("div");
      actionRow.className = "action-row";

      const quantityLabel = document.createElement("label");
      quantityLabel.textContent = "Qty";

      const quantityInput = document.createElement("input");
      quantityInput.type = "number";
      quantityInput.min = "1";
      quantityInput.value = "1";
      quantityLabel.appendChild(quantityInput);

      const addBtn = document.createElement("button");
      addBtn.type = "button";
      addBtn.className = "mini-btn";
      addBtn.textContent = "Add to App Cart";
      addBtn.disabled = !product.upc || unitPrice == null;
      addBtn.addEventListener("click", () => {
        addProductToCart(group.locationId, group.locationName, product, quantityInput.value);
      });

      actionRow.append(quantityLabel, addBtn);
      card.append(productName, brand, upc, size, price, unitPriceLine, temp, actionRow);
      grid.appendChild(card);
    }

    section.appendChild(grid);
    productGroupsEl.appendChild(section);
  }
}

function addProductToCart(locationId, locationName, product, quantityRaw) {
  const quantity = Number.parseInt(quantityRaw, 10);
  if (!Number.isInteger(quantity) || quantity <= 0) {
    setStatus(productStatusEl, "Quantity must be a positive integer.", true);
    return;
  }
  if (!product.upc) {
    setStatus(productStatusEl, "Product UPC is required to add to cart.", true);
    return;
  }

  const unitPrice = deriveUnitPrice(product);
  if (unitPrice == null) {
    setStatus(productStatusEl, "Product price unavailable; cannot add to cart.", true);
    return;
  }

  const key = `${locationId}:${product.upc}`;
  const existing = appCartItems.find((item) => item.key === key);
  if (existing) {
    existing.quantity += quantity;
  } else {
    appCartItems.push({
      key,
      locationId,
      locationName: locationName || locationId,
      productId: product.productId || "",
      upc: product.upc,
      description: product.description || "Unnamed Product",
      quantity,
      unitPrice
    });
  }

  setStatus(productStatusEl, "Added item to app cart.");
  renderAppCart();
}

function removeCartItem(key) {
  appCartItems = appCartItems.filter((item) => item.key !== key);
  renderAppCart();
}

function clearCart() {
  appCartItems = [];
  renderAppCart();
  setStatus(cartSyncStatusEl, "");
}

function buildStoreTotals() {
  const grouped = new Map();
  for (const item of appCartItems) {
    const current = grouped.get(item.locationId) || {
      locationId: item.locationId,
      locationName: item.locationName,
      total: 0,
      items: []
    };
    current.total += item.unitPrice * item.quantity;
    current.items.push(item);
    grouped.set(item.locationId, current);
  }
  return Array.from(grouped.values()).sort((a, b) => a.total - b.total);
}

function renderAppCart() {
  appCartListEl.innerHTML = "";
  storeTotalsEl.innerHTML = "";
  cartItemCountEl.textContent = formatCount(appCartItems.length, "item", "items");

  if (appCartItems.length === 0) {
    appCartListEl.innerHTML = '<p class="empty">No items in app cart yet.</p>';
    cheapestStoreStatusEl.textContent = "";
    syncCheapestCartBtn.disabled = true;
    return;
  }

  for (const item of appCartItems) {
    const row = document.createElement("div");
    row.className = "cart-item";
    row.innerHTML = `
      <div>
        <p class="item-title">${item.description}</p>
        <p class="meta">Store: ${item.locationName} (${item.locationId})</p>
        <p class="meta">UPC: ${item.upc}</p>
      </div>
      <p class="meta">Qty: ${item.quantity}</p>
      <p class="meta">Unit: ${formatMoney(item.unitPrice)}</p>
      <button type="button" class="secondary-btn mini-btn">Remove</button>
    `;
    row.querySelector("button").addEventListener("click", () => removeCartItem(item.key));
    appCartListEl.appendChild(row);
  }

  const totals = buildStoreTotals();
  const cheapest = totals[0];
  for (const summary of totals) {
    const div = document.createElement("div");
    div.className = "store-total" + (cheapest && summary.locationId === cheapest.locationId ? " cheapest" : "");
    div.innerHTML = `
      <span>${summary.locationName} (${summary.locationId})</span>
      <strong>${formatMoney(summary.total)}</strong>
    `;
    storeTotalsEl.appendChild(div);
  }

  if (cheapest) {
    cheapestStoreStatusEl.textContent = `Cheapest store currently: ${cheapest.locationName} (${cheapest.locationId}) at ${formatMoney(cheapest.total)}.`;
  } else {
    cheapestStoreStatusEl.textContent = "";
  }
  syncCheapestCartBtn.disabled = !cheapest;
}

async function fetchLocations(event) {
  event.preventDefault();

  const zip = zipInput.value.trim();
  const radius = radiusInput.value;
  const limit = limitInput.value;

  if (!zip) {
    setStatus(locationStatusEl, "Please enter a ZIP code.", true);
    return;
  }

  fetchLocationsBtn.disabled = true;
  setStatus(locationStatusEl, "Fetching locations...");

  try {
    const params = new URLSearchParams({ zip, radius, limit });
    const response = await fetch(`/api/v1/kroger/locations?${params.toString()}`);
    const text = await response.text();

    let payload;
    try {
      payload = text ? JSON.parse(text) : [];
    } catch {
      payload = { raw: text };
    }

    locationsRawJsonEl.textContent = JSON.stringify(payload, null, 2);

    if (!response.ok) {
      const message = payload && payload.message ? payload.message : `Request failed with ${response.status}`;
      fetchedLocations = [];
      selectedLocationIds = new Set();
      renderLocationCards([]);
      locationResultCountEl.textContent = "0 locations";
      setStatus(locationStatusEl, message, true);
      return;
    }

    fetchedLocations = Array.isArray(payload) ? payload : [];
    selectedLocationIds = new Set(fetchedLocations.filter((loc) => loc.locationId).map((loc) => loc.locationId));

    const count = fetchedLocations.length;
    locationResultCountEl.textContent = formatCount(count, "location", "locations");
    renderLocationCards(fetchedLocations);
    setStatus(locationStatusEl, `Success. Retrieved ${formatCount(count, "location", "locations")}.`);
  } catch (error) {
    locationsRawJsonEl.textContent = JSON.stringify({ error: String(error) }, null, 2);
    fetchedLocations = [];
    selectedLocationIds = new Set();
    renderLocationCards([]);
    locationResultCountEl.textContent = "0 locations";
    setStatus(locationStatusEl, "Could not reach the locations API. Make sure Spring Boot is running on port 8080.", true);
  } finally {
    fetchLocationsBtn.disabled = false;
  }
}

async function searchProducts(event) {
  event.preventDefault();

  const term = termInput.value.trim();
  const brand = brandInput.value.trim();
  const limit = productLimitInput.value;

  if (!term) {
    setStatus(productStatusEl, "Please enter a product search term.", true);
    return;
  }

  const locationIds = Array.from(selectedLocationIds);
  if (locationIds.length === 0) {
    setStatus(productStatusEl, "Select at least one location before searching products.", true);
    return;
  }

  searchProductsBtn.disabled = true;
  setStatus(productStatusEl, `Searching "${term}" across ${locationIds.length} selected locations...`);

  try {
    const requests = locationIds.map(async (locationId) => {
      const location = fetchedLocations.find((loc) => loc.locationId === locationId);
      const locationName = location && location.name ? location.name : "Unknown Location";

      const params = new URLSearchParams({
        term,
        locationId,
        limit: String(limit)
      });
      if (brand) {
        params.set("brand", brand);
      }

      const response = await fetch(`/api/v1/kroger/products?${params.toString()}`);
      const text = await response.text();

      let payload;
      try {
        payload = text ? JSON.parse(text) : [];
      } catch {
        payload = { raw: text };
      }

      if (!response.ok) {
        const message = payload && payload.message ? payload.message : `Request failed with ${response.status}`;
        return { locationId, locationName, error: message, raw: payload };
      }

      return { locationId, locationName, products: Array.isArray(payload) ? payload : [], raw: payload };
    });

    const results = await Promise.all(requests);
    const totalProducts = results.reduce((sum, r) => sum + (Array.isArray(r.products) ? r.products.length : 0), 0);
    const failures = results.filter((r) => r.error).length;

    productResultCountEl.textContent = formatCount(totalProducts, "product", "products");
    renderProductGroups(results);
    productsRawJsonEl.textContent = JSON.stringify(results, null, 2);

    if (failures > 0) {
      setStatus(productStatusEl, `Completed with ${failures} location failure(s).`, true);
    } else {
      setStatus(productStatusEl, `Success. Retrieved ${formatCount(totalProducts, "product", "products")} across ${locationIds.length} locations.`);
    }
  } catch (error) {
    productsRawJsonEl.textContent = JSON.stringify({ error: String(error) }, null, 2);
    renderProductGroups([]);
    productResultCountEl.textContent = "0 products";
    setStatus(productStatusEl, "Could not reach the products API. Make sure Spring Boot is running on port 8080.", true);
  } finally {
    searchProductsBtn.disabled = false;
  }
}

function selectAllLocations() {
  selectedLocationIds = new Set(fetchedLocations.filter((loc) => loc.locationId).map((loc) => loc.locationId));
  renderLocationCards(fetchedLocations);
}

function clearSelectedLocations() {
  selectedLocationIds = new Set();
  renderLocationCards(fetchedLocations);
}

async function connectKrogerAccount() {
  if (connectKrogerBtn) {
    connectKrogerBtn.disabled = true;
  }
  setStatus(authStatusEl, "Requesting authorize URL...");

  try {
    const response = await fetch("/api/v1/kroger/oauth/authorize-url");
    const payload = await response.json();

    if (!response.ok) {
      setStatus(authStatusEl, payload.error || "Failed to generate authorize URL.", true);
      return;
    }

    setStatus(authStatusEl, "Redirecting to Kroger authorization...");
    sessionStorage.setItem("krogerAutoAuthAttempted", "true");
    window.location.assign(payload.authorizeUrl);
  } catch (error) {
    setStatus(authStatusEl, `Failed to connect account: ${String(error)}`, true);
  } finally {
    if (connectKrogerBtn) {
      connectKrogerBtn.disabled = false;
    }
  }
}

async function autoConnectKrogerOnAppLoad() {
  const url = new URL(window.location.href);
  const oauthStatus = url.searchParams.get("oauth");
  const oauthMessage = url.searchParams.get("message");
  if (oauthStatus === "connected") {
    setStatus(authStatusEl, "Kroger account connected.");
    sessionStorage.removeItem("krogerAutoAuthAttempted");
  } else if (oauthStatus === "error") {
    const decoded = oauthMessage ? decodeURIComponent(oauthMessage) : "OAuth failed.";
    setStatus(authStatusEl, decoded, true);
  }

  if (oauthStatus) {
    url.searchParams.delete("oauth");
    url.searchParams.delete("message");
    window.history.replaceState({}, "", url.pathname + (url.search ? url.search : ""));
  }

  let statusPayload;
  try {
    const statusResponse = await fetch("/api/v1/kroger/oauth/status");
    if (!statusResponse.ok) {
      setStatus(authStatusEl, "Could not determine OAuth status.", true);
      return;
    }
    statusPayload = await statusResponse.json();
  } catch (error) {
    setStatus(authStatusEl, `OAuth status check failed: ${String(error)}`, true);
    return;
  }

  if (statusPayload.connected) {
    setStatus(authStatusEl, statusPayload.expiresAt ? `Connected (expires ${statusPayload.expiresAt})` : "Connected.");
    sessionStorage.removeItem("krogerAutoAuthAttempted");
    return;
  }

  const attempted = sessionStorage.getItem("krogerAutoAuthAttempted") === "true";
  if (!attempted) {
    setStatus(authStatusEl, "Not connected. Starting Kroger OAuth...");
    await connectKrogerAccount();
  } else {
    setStatus(authStatusEl, "Not connected. OAuth attempt already ran in this tab; refresh to retry.", true);
  }
}

function buildCheapestCartPayload() {
  const totals = buildStoreTotals();
  const cheapest = totals[0];
  if (!cheapest) {
    return null;
  }

  const byUpc = new Map();
  for (const item of cheapest.items) {
    const currentQty = byUpc.get(item.upc) || 0;
    byUpc.set(item.upc, currentQty + item.quantity);
  }

  const modality = cartModalityEl.value || "PICKUP";
  const items = Array.from(byUpc.entries()).map(([upc, quantity]) => ({ upc, quantity, modality }));

  return {
    cheapestStore: cheapest,
    requestBody: { items }
  };
}

async function syncCheapestCart() {
  const payload = buildCheapestCartPayload();
  if (!payload) {
    setStatus(cartSyncStatusEl, "No cart items available to sync.", true);
    return;
  }

  syncCheapestCartBtn.disabled = true;
  setStatus(cartSyncStatusEl, `Syncing ${payload.requestBody.items.length} item(s) to Kroger cart...`);

  try {
    const response = await fetch("/api/v1/kroger/cart/add", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload.requestBody)
    });

    const text = await response.text();
    let result;
    try {
      result = text ? JSON.parse(text) : {};
    } catch {
      result = { raw: text };
    }

    cartSyncRawJsonEl.textContent = JSON.stringify(result, null, 2);

    if (!response.ok) {
      const message = result && result.error ? result.error : `Sync failed with status ${response.status}`;
      setStatus(cartSyncStatusEl, message, true);
      return;
    }

    const store = payload.cheapestStore;
    setStatus(cartSyncStatusEl, `Kroger cart sync complete for cheapest store ${store.locationName} (${store.locationId}).`);
  } catch (error) {
    setStatus(cartSyncStatusEl, `Cart sync failed: ${String(error)}`, true);
  } finally {
    syncCheapestCartBtn.disabled = false;
  }
}

locationsForm.addEventListener("submit", fetchLocations);
productsForm.addEventListener("submit", searchProducts);
selectAllBtn.addEventListener("click", selectAllLocations);
clearSelectionBtn.addEventListener("click", clearSelectedLocations);
clearCartBtn.addEventListener("click", clearCart);
if (connectKrogerBtn) {
  connectKrogerBtn.addEventListener("click", connectKrogerAccount);
}
syncCheapestCartBtn.addEventListener("click", syncCheapestCart);

updateSelectionUi();
renderAppCart();
autoConnectKrogerOnAppLoad();
