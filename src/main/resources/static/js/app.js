// =====================================================================
// app.js - Menu and Cart page (index.html)
// The cart talks to the Spring Boot API (/api/cart).
// If the backend is not running, the page switches to demo data.
// Shared helpers (money, toast, request...) are in common.js
// =====================================================================

const API_URL = "/api/cart";
const MAX_QTY = 50;     // Same rule as the backend

// ---------------------------------------------------------------------
// 1. Dummy menu data (will come from Member 3's Product API later)
// ---------------------------------------------------------------------
const MENU = [
  { id: 101, category: "Cakes",    name: "Royal Strawberry Velvet Cake", price: 4500, desc: "Velvet sponge, fresh strawberries, vanilla buttercream." },
  { id: 102, category: "Cakes",    name: "Belgian Truffle Chocolate Cake", price: 5200, desc: "Dark Belgian cocoa in rich truffle layers." },
  { id: 301, category: "Cakes",    name: "Lavender Honey Cheesecake", price: 3200, desc: "Baked cheesecake with a light honey and lavender note." },
  { id: 201, category: "Pastries", name: "Almond Croissant", price: 650, desc: "Buttery, flaky pastry filled with almond cream." },
  { id: 103, category: "Pastries", name: "Salted Caramel Macarons (6)", price: 1850, desc: "Hand-piped shells with a salted caramel filling." },
  { id: 302, category: "Pastries", name: "Chocolate Eclair", price: 550, desc: "Choux pastry, vanilla cream, dark chocolate glaze." },
  { id: 202, category: "Bread and savoury", name: "Chicken Brioche Pie", price: 980, desc: "Tender chicken in a golden brioche crust." },
  { id: 203, category: "Bread and savoury", name: "Classic Sourdough Loaf", price: 900, desc: "Slow-fermented for 24 hours, baked on stone." }
];

// ---------------------------------------------------------------------
// 2. Demo cart (only used when the backend is not reachable)
// ---------------------------------------------------------------------
let demoItems = [
  { id: 1, userId: USER_ID, productId: 201, productName: "Almond Croissant", quantity: 2, unitPrice: 650 },
  { id: 2, userId: USER_ID, productId: 102, productName: "Belgian Truffle Chocolate Cake", quantity: 1, unitPrice: 5200 },
  { id: 3, userId: USER_ID, productId: 203, productName: "Classic Sourdough Loaf", quantity: 1, unitPrice: 900 }
];
let demoNextId = 4;

function demoCartResponse(search) {
  const text = (search || "").trim().toLowerCase();
  const items = demoItems.filter(i => i.productName.toLowerCase().includes(text));
  const total = items.reduce((sum, i) => sum + i.quantity * i.unitPrice, 0);
  return { userId: USER_ID, items: items, total: total };
}

const demoApi = {
  async getCart(search) { return demoCartResponse(search); },
  async add(item) {
    const old = demoItems.find(i => i.productId === item.productId);
    if (old) {
      if (old.quantity + item.quantity > MAX_QTY) {
        throw new Error("You can only have 50 of the same item in the cart.");
      }
      old.quantity += item.quantity;
      return old;
    }
    const created = { ...item, id: demoNextId++ };
    demoItems.push(created);
    return created;
  },
  async setQuantity(id, quantity) {
    const item = demoItems.find(i => i.id === id);
    if (!item) throw new Error("Cart item not found with id " + id);
    item.quantity = quantity;
    return item;
  },
  async remove(id) { demoItems = demoItems.filter(i => i.id !== id); },
  async clear() { demoItems = []; }
};

// ---------------------------------------------------------------------
// 3. Real cart API calls (Spring Boot)
// ---------------------------------------------------------------------
const liveApi = {
  getCart(search) {
    const q = search ? "?search=" + encodeURIComponent(search) : "";
    return request("GET", API_URL + "/user/" + USER_ID + q);
  },
  add(item)                { return request("POST", API_URL, item); },
  setQuantity(id, quantity) { return request("PUT", API_URL + "/" + id, { quantity: quantity }); },
  remove(id)               { return request("DELETE", API_URL + "/" + id); },
  clear()                  { return request("DELETE", API_URL + "/user/" + USER_ID); }
};

let api = liveApi;                 // switches to demoApi if the backend is down
let cart = { items: [], total: 0 };
let searchText = "";
let cartQuantity = 0;              // number of pieces in the whole cart

// ---------------------------------------------------------------------
// 4. Draw the menu and the cart
// ---------------------------------------------------------------------
function renderMenu() {
  const categories = [...new Set(MENU.map(p => p.category))];
  $("menu").innerHTML = categories.map(cat => `
    <div class="menu-group">
      <h3>${esc(cat)}</h3>
      ${MENU.filter(p => p.category === cat).map(p => `
        <div class="menu-item">
          <span class="name">${esc(p.name)}</span>
          <span class="desc">${esc(p.desc)}</span>
          <span class="side">
            <span class="price">${money(p.price)}</span>
            <button class="btn" type="button" data-add="${p.id}" aria-label="Add ${esc(p.name)} to cart">[ Add ]</button>
          </span>
        </div>`).join("")}
    </div>`).join("");
}

function renderCart() {
  const items = cart.items;

  if (items.length === 0) {
    $("cartList").innerHTML = `<p class="empty">${
      searchText ? "No items match your search." : "Your cart is empty. Add something from the menu."
    }</p>`;
  } else {
    $("cartList").innerHTML = items.map(item => `
      <div class="cart-row">
        <div class="info">
          <div class="item-name">${esc(item.productName)}</div>
          <div class="unit">${money(item.unitPrice)} each</div>
        </div>
        <div class="stepper">
          <button type="button" data-dec="${item.id}" aria-label="Decrease quantity" ${item.quantity <= 1 ? "disabled" : ""}>&minus;</button>
          <span>${item.quantity}</span>
          <button type="button" data-inc="${item.id}" aria-label="Increase quantity" ${item.quantity >= MAX_QTY ? "disabled" : ""}>+</button>
        </div>
        <div class="line">${money(item.quantity * item.unitPrice)}</div>
        <button class="remove" type="button" data-remove="${item.id}">Remove</button>
      </div>`).join("");
  }

  $("cartTotal").textContent = money(cart.total);
  $("clearBtn").disabled = items.length === 0;

  // the number in the navbar counts the whole cart (not the search result)
  if (!searchText) {
    cartQuantity = items.reduce((sum, i) => sum + i.quantity, 0);
    $("cartCount").textContent = cartQuantity;
  }
  $("orderBtn").disabled = cartQuantity === 0;
}

// ---------------------------------------------------------------------
// 5. Actions (each one calls the API, then reloads the cart)
// ---------------------------------------------------------------------
async function loadCart(firstLoad) {
  try {
    cart = await api.getCart(searchText);
  } catch (err) {
    if (!firstLoad) { toast(err.message, true); return; }
    api = demoApi;                       // backend not reachable: use demo data
    $("demoBanner").hidden = false;
    cart = await api.getCart(searchText);
  }
  renderCart();
}

async function addToCart(productId) {
  const product = MENU.find(p => p.id === productId);
  try {
    await api.add({
      userId: USER_ID,
      productId: product.id,
      productName: product.name,
      quantity: 1,
      unitPrice: product.price
    });
    toast(product.name + " added to your cart.");
    await loadCart();
  } catch (err) {
    toast(err.message, true);
  }
}

async function changeQuantity(id, change) {
  const item = cart.items.find(i => i.id === id);
  const newQty = item.quantity + change;
  if (newQty < 1 || newQty > MAX_QTY) return;
  try {
    await api.setQuantity(id, newQty);
    await loadCart();
  } catch (err) {
    toast(err.message, true);
  }
}

async function removeItem(id) {
  try {
    await api.remove(id);
    toast("Item removed.");
    await loadCart();
  } catch (err) {
    toast(err.message, true);
  }
}

async function clearCart() {
  if (!confirm("Remove everything from your cart?")) return;
  try {
    await api.clear();
    toast("Cart cleared.");
    await loadCart();
  } catch (err) {
    toast(err.message, true);
  }
}

// Place the order: the backend turns the whole cart into an order and empties the cart
async function placeOrder() {
  if (api === demoApi) {
    toast("Demo mode: start the Spring Boot backend to place a real order.", true);
    return;
  }
  try {
    const order = await liveOrderApi.place(USER_ID, $("orderNote").value.trim());
    window.location.href = "orders.html?placed=" + order.id;   // order confirmation
  } catch (err) {
    toast(err.message, true);
  }
}

// ---------------------------------------------------------------------
// 6. Connect the buttons (event delegation: one listener per area)
// ---------------------------------------------------------------------
$("menu").addEventListener("click", e => {
  const btn = e.target.closest("[data-add]");
  if (btn) addToCart(Number(btn.dataset.add));
});

$("cartList").addEventListener("click", e => {
  const dec = e.target.closest("[data-dec]");
  const inc = e.target.closest("[data-inc]");
  const rem = e.target.closest("[data-remove]");
  if (dec) changeQuantity(Number(dec.dataset.dec), -1);
  if (inc) changeQuantity(Number(inc.dataset.inc), 1);
  if (rem) removeItem(Number(rem.dataset.remove));
});

$("clearBtn").addEventListener("click", clearCart);
$("orderBtn").addEventListener("click", placeOrder);

let searchTimer;
$("search").addEventListener("input", e => {   // wait a moment while the user types
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    searchText = e.target.value.trim();
    loadCart();
  }, 250);
});

// ---------------------------------------------------------------------
// 7. Start
// ---------------------------------------------------------------------
renderMenu();
loadCart(true);
