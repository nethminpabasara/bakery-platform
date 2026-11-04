// =====================================================================
// admin-orders.js - Admin order list (admin-orders.html)
// The admin sees every order, filters by status, moves an order one
// step forward, or cancels a pending order.
// (Role-based access will be added when Member 2's login is ready.)
// =====================================================================

const NEXT_STATUS = { PENDING: "PREPARING", PREPARING: "READY", READY: "DELIVERED" };
const NEXT_BUTTON = { PENDING: "[ Start preparing ]", PREPARING: "[ Mark ready ]", READY: "[ Mark delivered ]" };

let orders = [];
let filter = "";

function render() {
  if (orders.length === 0) {
    $("orderRows").innerHTML = `<tr><td colspan="7" class="empty">No orders found.</td></tr>`;
    return;
  }
  const newestFirst = orders.slice().sort((a, b) => b.id - a.id);
  $("orderRows").innerHTML = newestFirst.map(o => `
    <tr>
      <td data-label="Order">#${o.id}</td>
      <td data-label="Placed">${formatDate(o.createdAt)}</td>
      <td data-label="Customer">User ${o.userId}</td>
      <td data-label="Items">${esc(o.itemsSummary)}${o.note ? `<div class="muted">Note: ${esc(o.note)}</div>` : ""}</td>
      <td data-label="Total" class="num">${money(o.totalAmount)}</td>
      <td data-label="Status"><span class="pill pill-${o.status.toLowerCase()}">${STATUS_LABEL[o.status]}</span></td>
      <td data-label="Action"><div class="action-box">
        ${NEXT_STATUS[o.status]
          ? `<button class="btn" type="button" data-next="${o.id}" data-status="${NEXT_STATUS[o.status]}">${NEXT_BUTTON[o.status]}</button>`
          : `<span class="muted">Completed</span>`}
        ${o.status === "PENDING" ? `<button class="btn" type="button" data-cancel="${o.id}">[ Cancel ]</button>` : ""}
      </div></td>
    </tr>`).join("");
}

async function loadOrders(firstLoad) {
  try {
    orders = await orderApi.getAll(filter);
  } catch (err) {
    if (!firstLoad) { toast(err.message, true); return; }
    orderApi = demoOrderApi;               // backend not reachable: use demo data
    $("demoBanner").hidden = false;
    orders = await orderApi.getAll(filter);
  }
  render();
}

async function moveForward(id, status) {
  try {
    await orderApi.updateStatus(id, status);
    toast("Order #" + id + " is now " + STATUS_LABEL[status] + ".");
    await loadOrders();
  } catch (err) {
    toast(err.message, true);
  }
}

async function cancelOrder(id) {
  if (!confirm("Cancel order #" + id + "? This cannot be undone.")) return;
  try {
    await orderApi.cancel(id);
    toast("Order #" + id + " cancelled.");
    await loadOrders();
  } catch (err) {
    toast(err.message, true);
  }
}

$("orderRows").addEventListener("click", e => {
  const next = e.target.closest("[data-next]");
  const cancel = e.target.closest("[data-cancel]");
  if (next)   moveForward(Number(next.dataset.next), next.dataset.status);
  if (cancel) cancelOrder(Number(cancel.dataset.cancel));
});

$("statusFilter").addEventListener("change", e => {
  filter = e.target.value;
  loadOrders();
});

loadOrders(true);
