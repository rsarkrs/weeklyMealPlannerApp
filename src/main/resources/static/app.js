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

let fetchedLocations = [];
let selectedLocationIds = new Set();

function setStatus(element, message, isError = false) {
  element.textContent = message;
  element.classList.toggle("error", isError);
}

function formatCount(value, singular, plural) {
  return `${value} ${value === 1 ? singular : plural}`;
}

function selectedLocationCount() {
  return selectedLocationIds.size;
}

function updateSelectionUi() {
  selectionCountEl.textContent = formatCount(selectedLocationCount(), "selected", "selected");
  searchProductsBtn.disabled = selectedLocationCount() === 0 || fetchedLocations.length === 0;
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
      price.textContent = `Regular: ${regular} | Promo: ${promo}`;

      const temp = document.createElement("p");
      temp.className = "meta";
      temp.textContent = product.temperature ? `Temp: ${product.temperature}` : "Temp: N/A";

      card.append(productName, brand, upc, size, price, temp);
      grid.appendChild(card);
    }

    section.appendChild(grid);
    productGroupsEl.appendChild(section);
  }
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

locationsForm.addEventListener("submit", fetchLocations);
productsForm.addEventListener("submit", searchProducts);
selectAllBtn.addEventListener("click", selectAllLocations);
clearSelectionBtn.addEventListener("click", clearSelectedLocations);

updateSelectionUi();
