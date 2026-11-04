// =====================================================================
// common.js - Helpers shared by every page (menu, orders, admin orders)
// =====================================================================

const USER_ID = 1;                       // Only one customer for now (login comes from Member 2)
const ORDER_URL = "/api/orders";

const STATUS_ORDER = ["PENDING", "PREPARING", "READY", "DELIVERED"];
const STATUS_LABEL = { PENDING: "Pending", PREPARING: "Preparing", READY: "Ready", DELIVERED: "Delivered" };

// ---------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------
const $ = id => document.getElementById(id);

function money(value) {
  return "Rs. " + Number(value).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function esc(text) {                     // stops HTML from being injected through names or notes
  const div = document.createElement("div");
  div.textContent = text == null ? "" : text;
  return div.innerHTML;
}

// The backend may send the date as text ("2026-10-25T10:30:00") or as a list [2026,10,25,10,30]
function formatDate(value) {
  const d = Array.isArray(value)
    ? new Date(value[0], value[1] - 1, value[2], value[3] || 0, value[4] || 0)
    : new Date(value);
  if (isNaN(d.getTime())) return "";
  return d.toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })
    + ", " + d.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" });
}

let toastTimer;
function toast(message, isError) {
  const el = $("toast");
  el.textContent = message;
  el.className = "toast show" + (isError ? " error" : "");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.className = "toast"; }, 3000);
}

// ---------------------------------------------------------------------
// Talking to the Spring Boot API
// ---------------------------------------------------------------------
async function request(method, url, body) {
  const options = { method: method, headers: {} };
  if (body) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }
  const res = await fetch(url, options);
  if (!res.ok) throw new Error(await readError(res));
  if (res.status === 204) return null;      // delete returns no body
  return res.json();
}

// The backend sends errors as JSON, e.g. {"error": "..."} or {"note": "..."}
async function readError(res) {
  if (res.status >= 500) return "Server error. Check the Spring Boot console.";
  try {
    const data = await res.json();
    return Object.values(data).join(", ");
  } catch (e) {
    return "Something went wrong (" + res.status + ").";
  }
}

// ---------------------------------------------------------------------
// Order API (real backend)
// ---------------------------------------------------------------------
const liveOrderApi = {
  getAll(status)      { return request("GET", ORDER_URL + (status ? "?status=" + status : "")); },
  getByUser(userId)   { return request("GET", ORDER_URL + "/user/" + userId); },
  place(userId, note) { return request("POST", ORDER_URL, { userId: userId, note: note }); },
  updateStatus(id, status) { return request("PUT", ORDER_URL + "/" + id + "/status", { status: status }); },
  editNote(id, note)  { return request("PUT", ORDER_URL + "/" + id, { note: note }); },
  cancel(id)          { return request("DELETE", ORDER_URL + "/" + id); }
};

// ---------------------------------------------------------------------
// Order API (demo data, only used when the backend is not reachable)
// It follows the same rules as OrderService in Spring Boot.
// ---------------------------------------------------------------------
let demoOrders = [
  { id: 4, userId: USER_ID, status: "PENDING",   createdAt: "2026-10-25T09:40:00", note: "Please write Happy Birthday Amaya", itemsSummary: "1 x Royal Strawberry Velvet Cake, 6 x Chocolate Eclair", totalAmount: 7800 },
  { id: 3, userId: USER_ID, status: "PREPARING", createdAt: "2026-10-25T08:15:00", note: "", itemsSummary: "2 x Almond Croissant, 1 x Classic Sourdough Loaf", totalAmount: 2200 },
  { id: 2, userId: 2,       status: "READY",     createdAt: "2026-10-24T16:05:00", note: "No nuts, please", itemsSummary: "1 x Lavender Honey Cheesecake", totalAmount: 3200 },
  { id: 1, userId: USER_ID, status: "DELIVERED", createdAt: "2026-10-22T11:30:00", note: "", itemsSummary: "3 x Chicken Brioche Pie, 1 x Salted Caramel Macarons (6)", totalAmount: 4790 }
];

function findDemoOrder(id) {
  const order = demoOrders.find(o => o.id === id);
  if (!order) throw new Error("Order not found with id " + id);
  return order;
}

const demoOrderApi = {
  async getAll(status)    { return demoOrders.filter(o => !status || o.status === status); },
  async getByUser(userId) { return demoOrders.filter(o => o.userId === userId); },
  async updateStatus(id, status) {
    const order = findDemoOrder(id);
    if (STATUS_ORDER.indexOf(status) !== STATUS_ORDER.indexOf(order.status) + 1) {
      throw new Error("An order cannot move from " + order.status + " to " + status + ".");
    }
    order.status = status;
    return order;
  },
  async editNote(id, note) {
    const order = findDemoOrder(id);
    if (order.status !== "PENDING") throw new Error("Only pending orders can be edited.");
    order.note = note;
    return order;
  },
  async cancel(id) {
    const order = findDemoOrder(id);
    if (order.status !== "PENDING") throw new Error("Only pending orders can be cancelled.");
    demoOrders = demoOrders.filter(o => o.id !== id);
  }
};

let orderApi = liveOrderApi;           // switches to demoOrderApi if the backend is down
