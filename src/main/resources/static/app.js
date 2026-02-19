const locationsForm = document.getElementById("locationsForm");
const productsForm = document.getElementById("productsForm");
const mealIntakeForm = document.getElementById("mealIntakeForm");
const mealPlanZipInput = document.getElementById("mealPlanZip");
const peopleCountInput = document.getElementById("peopleCount");
const mealsPerDayInput = document.getElementById("mealsPerDay");
const existingProfileSelect = document.getElementById("existingProfileSelect");
const createProfileBtn = document.getElementById("createProfileBtn");
const loadIntakeProfileBtn = document.getElementById("loadIntakeProfileBtn");
const activeProfileIdEl = document.getElementById("activeProfileId");
const personRowsEl = document.getElementById("personRows");
const proteinsInput = document.getElementById("proteinsInput");
const veggiesInput = document.getElementById("veggiesInput");
const carbsInput = document.getElementById("carbsInput");
const allergiesInput = document.getElementById("allergiesInput");
const optimizationGoalInput = document.getElementById("optimizationGoal");
const maxPrepMinutesInput = document.getElementById("maxPrepMinutes");
const maxCookMinutesInput = document.getElementById("maxCookMinutes");
const generateMealIntakeJsonBtn = document.getElementById("generateMealIntakeJsonBtn");
const mealIntakeStatusEl = document.getElementById("mealIntakeStatus");
const mealIntakeJsonEl = document.getElementById("mealIntakeJson");
const mealPlanResultCountEl = document.getElementById("mealPlanResultCount");
const mealPlanCheapestStoreStatusEl = document.getElementById("mealPlanCheapestStoreStatus");
const calorieTargetCardsEl = document.getElementById("calorieTargetCards");
const mealPlanCardsEl = document.getElementById("mealPlanCards");
const shoppingListCountEl = document.getElementById("shoppingListCount");
const shoppingListCardsEl = document.getElementById("shoppingListCards");
const profileRawJsonEl = document.getElementById("profileRawJson");

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
const PROFILE_STORAGE_KEY = "weeklyMealPlanner.mealIntakeProfiles";
let activeProfileId = null;

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

function formatNumber(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return "N/A";
  }
  return Number(value).toFixed(2);
}

function clearGeneratedPlanUi() {
  mealPlanResultCountEl.textContent = "0 people";
  mealPlanCheapestStoreStatusEl.textContent = "";
  calorieTargetCardsEl.innerHTML = "";
  mealPlanCardsEl.innerHTML = "";
  shoppingListCountEl.textContent = "0 ingredients";
  shoppingListCardsEl.innerHTML = "";
}

function renderCalorieTargets(calorieTargets) {
  calorieTargetCardsEl.innerHTML = "";

  if (!Array.isArray(calorieTargets) || calorieTargets.length === 0) {
    calorieTargetCardsEl.innerHTML = '<p class="empty">No calorie targets were generated.</p>';
    return;
  }

  for (const target of calorieTargets) {
    const card = document.createElement("article");
    card.className = "card";

    const title = document.createElement("h3");
    title.textContent = `Person: ${target.personId || "Unknown"}`;
    const sex = document.createElement("p");
    sex.className = "meta";
    sex.textContent = `Sex: ${target.sex || "N/A"}`;
    const bmr = document.createElement("p");
    bmr.className = "meta";
    bmr.textContent = `BMR: ${formatNumber(target.bmr)} kcal`;
    const tdee = document.createElement("p");
    tdee.className = "meta";
    tdee.textContent = `TDEE (sedentary): ${formatNumber(target.tdee)} kcal`;
    const daily = document.createElement("p");
    daily.className = "meta";
    daily.textContent = `Daily target: ${formatNumber(target.dailyCalorieTarget)} kcal`;
    const floor = document.createElement("p");
    floor.className = "meta";
    floor.textContent = `Calorie floor: ${formatNumber(target.appliedCalorieFloor)} kcal`;

    card.append(title, sex, bmr, tdee, daily, floor);
    calorieTargetCardsEl.appendChild(card);
  }
}

function renderMealPlans(plans) {
  mealPlanCardsEl.innerHTML = "";

  if (!Array.isArray(plans) || plans.length === 0) {
    mealPlanCardsEl.innerHTML = '<p class="empty">No meal plans were generated.</p>';
    return;
  }

  for (const personPlan of plans) {
    const section = document.createElement("section");
    section.className = "group";

    const head = document.createElement("div");
    head.className = "group-head";
    const title = document.createElement("h3");
    title.textContent = `Plan for ${personPlan.personId || "Unknown Person"}`;
    head.appendChild(title);
    section.appendChild(head);

    const dayGrid = document.createElement("div");
    dayGrid.className = "meal-day-grid";

    const days = Array.isArray(personPlan.days) ? personPlan.days : [];
    for (const day of days) {
      const dayCard = document.createElement("article");
      dayCard.className = "card";

      const dayTitle = document.createElement("h3");
      dayTitle.textContent = `Day ${day.dayNumber ?? "?"}`;
      dayCard.appendChild(dayTitle);

      const meals = Array.isArray(day.meals) ? day.meals : [];
      for (const meal of meals) {
        const mealBlock = document.createElement("div");
        mealBlock.className = "meal-block";

        const mealName = document.createElement("h4");
        mealName.textContent = `${meal.mealType || "Meal"}: ${meal.mealName || "Unnamed"}`;
        const meta = document.createElement("p");
        meta.className = "meta";
        meta.textContent = `Protein: ${meal.primaryProtein || "N/A"} | Calories: ${meal.estimatedCalories ?? "N/A"} | Prep/Cook: ${meal.prepMinutes ?? "N/A"}/${meal.cookMinutes ?? "N/A"} min`;

        const ingredientList = document.createElement("ul");
        ingredientList.className = "ingredient-list";
        const ingredients = Array.isArray(meal.ingredients) ? meal.ingredients : [];
        for (const ingredient of ingredients) {
          const li = document.createElement("li");
          li.textContent = `${ingredient.ingredient}: ${formatNumber(ingredient.householdQuantity)} ${ingredient.householdUnit || ""} (~${formatNumber(ingredient.metricQuantity)} ${ingredient.metricUnit || ""})`;
          ingredientList.appendChild(li);
        }

        const stepList = document.createElement("ol");
        stepList.className = "step-list";
        const steps = Array.isArray(meal.steps) ? meal.steps : [];
        for (const step of steps) {
          const li = document.createElement("li");
          li.textContent = step;
          stepList.appendChild(li);
        }

        mealBlock.append(mealName, meta, ingredientList, stepList);
        dayCard.appendChild(mealBlock);
      }

      dayGrid.appendChild(dayCard);
    }

    section.appendChild(dayGrid);
    mealPlanCardsEl.appendChild(section);
  }
}

function renderShoppingListRaw(shoppingList) {
  shoppingListCardsEl.innerHTML = "";

  if (!Array.isArray(shoppingList) || shoppingList.length === 0) {
    shoppingListCountEl.textContent = "0 ingredients";
    shoppingListCardsEl.innerHTML = '<p class="empty">No consolidated shopping list generated.</p>';
    return;
  }

  shoppingListCountEl.textContent = formatCount(shoppingList.length, "ingredient", "ingredients");

  for (const item of shoppingList) {
    const card = document.createElement("article");
    card.className = "card";

    const name = document.createElement("h3");
    name.textContent = item.ingredient || "Unnamed ingredient";
    const household = document.createElement("p");
    household.className = "meta";
    household.textContent = `Household: ${formatNumber(item.totalHouseholdQuantity)} ${item.householdUnit || ""}`;
    const metric = document.createElement("p");
    metric.className = "meta";
    metric.textContent = `Metric: ${formatNumber(item.totalMetricQuantity)} ${item.metricUnit || ""}`;

    card.append(name, household, metric);
    shoppingListCardsEl.appendChild(card);
  }
}

function renderGeneratedPlan(payload) {
  const planPayload = payload && payload.generatedPlan ? payload.generatedPlan : payload;
  const plans = Array.isArray(planPayload.plans) ? planPayload.plans : [];
  const shoppingList = Array.isArray(planPayload.shoppingListRaw) ? planPayload.shoppingListRaw : [];
  const priced = planPayload.shoppingListPriced || null;

  mealPlanResultCountEl.textContent = formatCount(plans.length, "person", "people");
  renderCalorieTargets(planPayload.calorieTargets);
  renderMealPlans(plans);
  renderShoppingListRaw(shoppingList);

  if (priced && priced.cheapestStoreName) {
    mealPlanCheapestStoreStatusEl.textContent = `Cheapest store estimate: ${priced.cheapestStoreName} (${priced.cheapestStoreLocationId}) at ${formatMoney(priced.cheapestStoreTotal)}.`;
  } else if (priced) {
    mealPlanCheapestStoreStatusEl.textContent = "Pricing completed but no cheapest store could be determined.";
  } else {
    mealPlanCheapestStoreStatusEl.textContent = "No pricing response available.";
  }
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

function parseListInput(value) {
  if (!value) {
    return [];
  }
  return value
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function createUniqueProfileId() {
  const stamp = Date.now().toString(36);
  const suffix = Math.random().toString(36).slice(2, 6);
  return `profile-${stamp}-${suffix}`;
}

function getStoredProfiles() {
  try {
    const raw = window.localStorage.getItem(PROFILE_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function saveStoredProfiles(profiles) {
  window.localStorage.setItem(PROFILE_STORAGE_KEY, JSON.stringify(profiles));
}

function renderProfileOptions() {
  if (!existingProfileSelect) {
    return;
  }

  const profiles = getStoredProfiles();
  existingProfileSelect.innerHTML = "";

  if (profiles.length === 0) {
    const option = document.createElement("option");
    option.value = "";
    option.textContent = "No saved profiles";
    existingProfileSelect.appendChild(option);
    return;
  }

  const emptyOption = document.createElement("option");
  emptyOption.value = "";
  emptyOption.textContent = "Select profile";
  existingProfileSelect.appendChild(emptyOption);

  for (const profile of profiles) {
    const option = document.createElement("option");
    option.value = profile.profileId;
    option.textContent = `${profile.profileId} (${profile.peopleCount} people)`;
    existingProfileSelect.appendChild(option);
  }
}

function buildPersonRow(index, person = {}) {
  const row = document.createElement("div");
  row.className = "person-row";
  row.dataset.personId = person.personId || `person-${index + 1}`;

  const title = document.createElement("p");
  title.className = "person-row-title";
  title.textContent = `Person ${index + 1}`;

  const sexLabel = document.createElement("label");
  sexLabel.textContent = "Sex";
  const sexSelect = document.createElement("select");
  sexSelect.className = "person-sex";
  const maleOption = document.createElement("option");
  maleOption.value = "male";
  maleOption.textContent = "male";
  const femaleOption = document.createElement("option");
  femaleOption.value = "female";
  femaleOption.textContent = "female";
  sexSelect.append(maleOption, femaleOption);
  sexSelect.value = String(person.sex || "").toLowerCase() === "female" ? "female" : "male";
  sexLabel.appendChild(sexSelect);

  const ageLabel = document.createElement("label");
  ageLabel.textContent = "Age (years)";
  const ageInput = document.createElement("input");
  ageInput.type = "number";
  ageInput.className = "person-age";
  ageInput.min = "1";
  ageInput.max = "120";
  ageInput.step = "1";
  ageInput.value = person.ageYears != null ? String(person.ageYears) : "30";
  ageLabel.appendChild(ageInput);

  const heightLabel = document.createElement("label");
  heightLabel.textContent = "Height (in)";
  const heightInput = document.createElement("input");
  heightInput.type = "number";
  heightInput.className = "person-height";
  heightInput.min = "36";
  heightInput.max = "96";
  heightInput.step = "1";
  heightInput.value = person.heightInches != null ? String(person.heightInches) : "";
  heightLabel.appendChild(heightInput);

  const weightLabel = document.createElement("label");
  weightLabel.textContent = "Weight (lb)";
  const weightInput = document.createElement("input");
  weightInput.type = "number";
  weightInput.className = "person-weight";
  weightInput.min = "60";
  weightInput.max = "800";
  weightInput.step = "0.1";
  weightInput.value = person.weightLbs != null ? String(person.weightLbs) : "";
  weightLabel.appendChild(weightInput);

  const lossLabel = document.createElement("label");
  lossLabel.textContent = "Target loss (lb/week)";
  const lossInput = document.createElement("input");
  lossInput.type = "number";
  lossInput.className = "person-loss";
  lossInput.min = "0";
  lossInput.max = "5";
  lossInput.step = "0.1";
  lossInput.value = person.targetLossLbsPerWeek != null ? String(person.targetLossLbsPerWeek) : "1";
  lossLabel.appendChild(lossInput);

  row.append(title, sexLabel, ageLabel, heightLabel, weightLabel, lossLabel);
  return row;
}

function renderPersonRows(count, people = []) {
  if (!personRowsEl) {
    return;
  }

  personRowsEl.innerHTML = "";
  for (let index = 0; index < count; index += 1) {
    personRowsEl.appendChild(buildPersonRow(index, people[index] || {}));
  }
}

function collectPeopleFromRows() {
  const rows = personRowsEl ? Array.from(personRowsEl.querySelectorAll(".person-row")) : [];
  const people = [];

  for (let index = 0; index < rows.length; index += 1) {
    const row = rows[index];
    const personId = row.dataset.personId || `person-${index + 1}`;
    const sex = row.querySelector(".person-sex").value;
    const ageYears = Number.parseFloat(row.querySelector(".person-age").value);
    const heightInches = Number.parseFloat(row.querySelector(".person-height").value);
    const weightLbs = Number.parseFloat(row.querySelector(".person-weight").value);
    const targetLossLbsPerWeek = Number.parseFloat(row.querySelector(".person-loss").value);

    if (!Number.isFinite(ageYears) || ageYears <= 0) {
      return { error: `Person ${index + 1}: age is required and must be > 0.` };
    }
    if (!Number.isFinite(heightInches) || heightInches <= 0) {
      return { error: `Person ${index + 1}: height is required and must be > 0.` };
    }
    if (!Number.isFinite(weightLbs) || weightLbs <= 0) {
      return { error: `Person ${index + 1}: weight is required and must be > 0.` };
    }
    if (!Number.isFinite(targetLossLbsPerWeek) || targetLossLbsPerWeek < 0) {
      return { error: `Person ${index + 1}: target loss must be >= 0.` };
    }

    people.push({
      personId,
      sex: sex.toUpperCase(),
      ageYears,
      heightInches,
      weightLbs,
      targetLossLbsPerWeek
    });
  }

  return { people };
}

function saveActiveProfile(payload) {
  const profiles = getStoredProfiles();
  const index = profiles.findIndex((profile) => profile.profileId === payload.profileId);
  if (index >= 0) {
    profiles[index] = payload;
  } else {
    profiles.push(payload);
  }
  saveStoredProfiles(profiles);
  renderProfileOptions();
}

function createNewProfile() {
  activeProfileId = createUniqueProfileId();
  activeProfileIdEl.textContent = `Profile: ${activeProfileId}`;
  setStatus(mealIntakeStatusEl, "Created a new unique profile.");
}

function loadSelectedProfile() {
  const profileId = existingProfileSelect ? existingProfileSelect.value : "";
  if (!profileId) {
    setStatus(mealIntakeStatusEl, "Select a saved profile to load.", true);
    return;
  }

  const profile = getStoredProfiles().find((item) => item.profileId === profileId);
  if (!profile) {
    setStatus(mealIntakeStatusEl, "Selected profile was not found.", true);
    return;
  }

  activeProfileId = profile.profileId;
  peopleCountInput.value = String(profile.peopleCount || 1);
  mealsPerDayInput.value = String(profile.mealsPerDay || 3);
  if (mealPlanZipInput) {
    mealPlanZipInput.value = profile.zip || mealPlanZipInput.value;
  }
  if (profile.zip) {
    zipInput.value = profile.zip;
  }
  optimizationGoalInput.value = profile.optimizationGoal || "BALANCED";
  maxPrepMinutesInput.value = String(profile.maxPrepMinutesPerMeal || 20);
  maxCookMinutesInput.value = String(profile.maxCookMinutesPerMeal || 30);
  proteinsInput.value = (profile.preferences && profile.preferences.proteins || []).join(", ");
  veggiesInput.value = (profile.preferences && profile.preferences.veggies || []).join(", ");
  carbsInput.value = (profile.preferences && profile.preferences.carbs || []).join(", ");
  allergiesInput.value = (profile.allergies && profile.allergies.excludedIngredients || []).join(", ");

  renderPersonRows(profile.peopleCount || 1, profile.people || []);
  activeProfileIdEl.textContent = `Profile: ${activeProfileId}`;
  setStatus(mealIntakeStatusEl, `Loaded profile ${activeProfileId}.`);
}

function buildMealIntakePayload() {
  const peopleCount = Number.parseInt(peopleCountInput.value, 10);
  const mealsPerDay = Number.parseInt(mealsPerDayInput.value, 10);
  const maxPrepMinutesPerMeal = Number.parseInt(maxPrepMinutesInput.value, 10);
  const maxCookMinutesPerMeal = Number.parseInt(maxCookMinutesInput.value, 10);

  if (!Number.isInteger(peopleCount) || peopleCount <= 0) {
    return { error: "How many people must be a positive integer." };
  }
  if (!Number.isInteger(mealsPerDay) || mealsPerDay <= 0) {
    return { error: "Meals per day must be a positive integer." };
  }
  if (!Number.isInteger(maxPrepMinutesPerMeal) || maxPrepMinutesPerMeal <= 0) {
    return { error: "Max prep minutes must be a positive integer." };
  }
  if (!Number.isInteger(maxCookMinutesPerMeal) || maxCookMinutesPerMeal <= 0) {
    return { error: "Max cook minutes must be a positive integer." };
  }

  const peopleResult = collectPeopleFromRows();
  if (peopleResult.error) {
    return { error: peopleResult.error };
  }

  const proteins = parseListInput(proteinsInput.value);
  const veggies = parseListInput(veggiesInput.value);
  const carbs = parseListInput(carbsInput.value);
  const excludedIngredients = parseListInput(allergiesInput.value);

  if (proteins.length === 0 || veggies.length === 0 || carbs.length === 0) {
    return { error: "Please enter at least one protein, veggie, and carb." };
  }

  if (!activeProfileId) {
    activeProfileId = createUniqueProfileId();
    activeProfileIdEl.textContent = `Profile: ${activeProfileId}`;
  }

  const normalizedPeople = peopleResult.people.map((person, index) => ({
    ...person,
    personId: `${activeProfileId}-p${index + 1}`
  }));

  const payload = {
    people: normalizedPeople,
    preferences: {
      proteins,
      veggies,
      carbs
    },
    allergies: {
      excludedIngredients
    },
    optimizationGoal: optimizationGoalInput.value || "BALANCED",
    days: 7,
    mealsPerDay,
    maxPrepMinutesPerMeal,
    maxCookMinutesPerMeal
  };

  return { payload };
}

async function handleMealIntakeSubmit(event) {
  event.preventDefault();

  const result = buildMealIntakePayload();
  if (result.error) {
    setStatus(mealIntakeStatusEl, result.error, true);
    return;
  }

  const payload = result.payload;
  const peopleCount = payload.people.length;
  mealIntakeJsonEl.textContent = JSON.stringify(payload, null, 2);
  profileRawJsonEl.textContent = "{}";
  clearGeneratedPlanUi();

  const profileToSave = {
    profileId: activeProfileId,
    peopleCount,
    mealsPerDay: payload.mealsPerDay,
    people: payload.people,
    preferences: payload.preferences,
    allergies: payload.allergies,
    optimizationGoal: payload.optimizationGoal,
    maxPrepMinutesPerMeal: payload.maxPrepMinutesPerMeal,
    maxCookMinutesPerMeal: payload.maxCookMinutesPerMeal
  };

  generateMealIntakeJsonBtn.disabled = true;
  setStatus(mealIntakeStatusEl, "Generating weekly plan...");

  try {
    const response = await fetch("/api/v1/meal-planner/generate", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });

    const text = await response.text();
    let generated;
    try {
      generated = text ? JSON.parse(text) : {};
    } catch {
      generated = { raw: text };
    }

    profileRawJsonEl.textContent = JSON.stringify(generated, null, 2);

    if (!response.ok) {
      const message = generated && (generated.error || generated.message)
        ? (generated.error || generated.message)
        : `Weekly plan generation failed with status ${response.status}`;
      setStatus(mealIntakeStatusEl, message, true);
      return;
    }

    saveActiveProfile(profileToSave);
    renderGeneratedPlan(generated);
    setStatus(mealIntakeStatusEl, "Weekly plan generated via OpenAI and profile saved.");
  } catch (error) {
    profileRawJsonEl.textContent = JSON.stringify({ error: String(error) }, null, 2);
    setStatus(mealIntakeStatusEl, "Could not reach the meal planner/OpenAI API. Make sure Spring Boot is running on port 8080.", true);
  } finally {
    generateMealIntakeJsonBtn.disabled = false;
  }
}

function initializeMealIntakeForm() {
  if (!mealIntakeForm) {
    return;
  }

  renderProfileOptions();
  renderPersonRows(Number.parseInt(peopleCountInput.value, 10) || 1);
  activeProfileIdEl.textContent = "Profile: not created";
  mealIntakeJsonEl.textContent = "{}";
  profileRawJsonEl.textContent = "{}";
  clearGeneratedPlanUi();
  if (mealPlanZipInput && !mealPlanZipInput.value.trim() && zipInput.value.trim()) {
    mealPlanZipInput.value = zipInput.value.trim();
  }

  peopleCountInput.addEventListener("change", () => {
    const count = Number.parseInt(peopleCountInput.value, 10);
    if (!Number.isInteger(count) || count <= 0) {
      return;
    }
    const existing = collectPeopleFromRows();
    renderPersonRows(count, existing.people || []);
  });

  if (mealPlanZipInput) {
    mealPlanZipInput.addEventListener("change", () => {
      if (!zipInput.value.trim()) {
        zipInput.value = mealPlanZipInput.value.trim();
      }
    });
  }
  zipInput.addEventListener("change", () => {
    if (mealPlanZipInput && !mealPlanZipInput.value.trim()) {
      mealPlanZipInput.value = zipInput.value.trim();
    }
  });

  createProfileBtn.addEventListener("click", createNewProfile);
  loadIntakeProfileBtn.addEventListener("click", loadSelectedProfile);
  mealIntakeForm.addEventListener("submit", handleMealIntakeSubmit);
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

  if (mealPlanZipInput && !mealPlanZipInput.value.trim()) {
    mealPlanZipInput.value = zip;
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

initializeMealIntakeForm();
updateSelectionUi();
renderAppCart();
autoConnectKrogerOnAppLoad();
