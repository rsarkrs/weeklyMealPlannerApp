const form = document.getElementById("locationsForm");
const zipInput = document.getElementById("zip");
const radiusInput = document.getElementById("radius");
const limitInput = document.getElementById("limit");
const submitBtn = document.getElementById("submitBtn");
const statusEl = document.getElementById("status");
const resultCountEl = document.getElementById("resultCount");
const cardsEl = document.getElementById("cards");
const rawJsonEl = document.getElementById("rawJson");

function setStatus(message, isError = false) {
  statusEl.textContent = message;
  statusEl.classList.toggle("error", isError);
}

function renderCards(data) {
  cardsEl.innerHTML = "";

  if (!Array.isArray(data) || data.length === 0) {
    cardsEl.innerHTML = '<p class="empty">No locations returned for this search.</p>';
    return;
  }

  for (const item of data) {
    const card = document.createElement("article");
    card.className = "card";

    const name = item.name || "Unnamed Store";
    const chain = item.chain || "Kroger";
    const locationLine = [item.city, item.state, item.zipCode].filter(Boolean).join(", ");
    const phone = item.phone || "No phone";
    const hours = item.todayHours || "Hours unavailable";
    const timezone = item.timezone || "";

    const badges = [];
    if (item.pickup) badges.push('<span class="badge pickup">Pickup</span>');
    if (item.delivery) badges.push('<span class="badge delivery">Delivery</span>');

    card.innerHTML = `
      <h3>${name}</h3>
      <p class="meta">${chain}</p>
      <p class="meta">${locationLine || "Address unavailable"}</p>
      <p class="meta">${phone}</p>
      <p class="meta">${hours}${timezone ? ` (${timezone})` : ""}</p>
      <div class="badges">${badges.join("")}</div>
    `;

    cardsEl.appendChild(card);
  }
}

async function fetchLocations(event) {
  event.preventDefault();

  const zip = zipInput.value.trim();
  const radius = radiusInput.value;
  const limit = limitInput.value;

  if (!zip) {
    setStatus("Please enter a ZIP code.", true);
    return;
  }

  submitBtn.disabled = true;
  setStatus("Fetching locations...");

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

    rawJsonEl.textContent = JSON.stringify(payload, null, 2);

    if (!response.ok) {
      const message = payload && payload.message ? payload.message : `Request failed with ${response.status}`;
      renderCards([]);
      resultCountEl.textContent = "0 locations";
      setStatus(message, true);
      return;
    }

    const count = Array.isArray(payload) ? payload.length : 0;
    resultCountEl.textContent = `${count} location${count === 1 ? "" : "s"}`;
    renderCards(payload);
    setStatus(`Success. Retrieved ${count} location${count === 1 ? "" : "s"}.`);
  } catch (error) {
    rawJsonEl.textContent = JSON.stringify({ error: String(error) }, null, 2);
    renderCards([]);
    resultCountEl.textContent = "0 locations";
    setStatus("Could not reach the API. Make sure Spring Boot is running on port 8080.", true);
  } finally {
    submitBtn.disabled = false;
  }
}

form.addEventListener("submit", fetchLocations);
