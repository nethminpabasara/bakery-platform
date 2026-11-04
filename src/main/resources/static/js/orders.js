// =====================================================================
// orders.js - "My orders" page (orders.html)
// Shows the orders of one customer. The customer can edit the note or
// cancel an order, but only while it is still PENDING.
// =====================================================================

let orders = [];
let editingId = null;          // id of the order whose note is being edited
let paidOrderIds = [];         // orders that already have a PAID payment

function stepsHtml(status) {
  const current = STATUS_ORDER.indexOf(status);
  return STATUS_ORDER.map((s, i) =>
    `<li class="${i <= current ? "done" : ""}${i === current ? " current" : ""}">${STATUS_LABEL[s]}</li>`
  ).join("");
}

function noteHtml(order) {
  if (editingId === order.id) {
    return `
      <div class="note-edit">
        <input id="noteInput" type="text" maxlength="200" value="${esc(order.note)}" aria-label="Order note">
        <button class="btn" type="button" data-save="${order.id}">[ Save ]</button>
        <button class="btn" type="button" data-stop>[ Cancel ]</button>
      </div>`;
  }
  const text = order.note ? esc(order.note) : "No note";
  const edit = order.status === "PENDING"
    ? `<button class="link" type="button" data-edit="${order.id}">Edit note</button>` : "";
  return `<div class="note">Note: ${text} ${edit}</div>`;
}

function render() {
  if (orders.length === 0) {
    $("orderList").innerHTML = `<p class="empty">You have no orders yet. <a href="index.html">Go to the menu</a> to place one.</p>`;
    return;
  }
  const newestFirst = orders.slice().sort((a, b) => b.id - a.id);
  $("orderList").innerHTML = newestFirst.map(o => `
    <article class="order">
      <header class="order-head">
        <div>
          <h3>Order #${o.id}</h3>
          <div class="muted">${formatDate(o.createdAt)}</div>
        </div>
        <span class="pill pill-${o.status.toLowerCase()}">${STATUS_LABEL[o.status]}</span>
      </header>
      <ol class="steps" aria-label="Order progress">${stepsHtml(o.status)}</ol>
      <p class="items">${esc(o.itemsSummary)}</p>
      ${noteHtml(o)}
      <footer class="order-foot">
        <span class="total">${money(o.totalAmount)}</span>
        <div class="foot-actions">
          ${paidOrderIds.indexOf(o.id) !== -1
            ? `<span class="pill pill-paid">Paid</span>`
            : `<a class="btn" href="payment.html?order=${o.id}">[ Pay now ]</a>`}
          ${o.status === "PENDING" ? `<button class="btn" type="button" data-cancel="${o.id}">[ Cancel order ]</button>` : ""}
        </div>
      </footer>
    </article>`).join("");

  if (editingId !== null && $("noteInput")) $("noteInput").focus();
}

async function loadOrders(firstLoad) {
  try {
    orders = await orderApi.getByUser(USER_ID);
  } catch (err) {
    if (!firstLoad) { toast(err.message, true); return; }
    useDemoData();                         // backend not reachable: use demo data (orders and payments)
    orders = await orderApi.getByUser(USER_ID);
  }
  try {                                    // which orders are already paid?
    const payments = await paymentApi.getByUser(USER_ID);
    paidOrderIds = payments.filter(p => p.status === "PAID").map(p => p.orderId);
  } catch (err) {
    paidOrderIds = [];
  }
  render();
}

async function saveNote(id) {
  try {
    await orderApi.editNote(id, $("noteInput").value.trim());
    editingId = null;
    toast("Note updated.");
    await loadOrders();
  } catch (err) {
    toast(err.message, true);
  }
}

async function cancelOrder(id) {
  if (!confirm("Cancel this order? This cannot be undone.")) return;
  try {
    await orderApi.cancel(id);
    toast("Order cancelled.");
    await loadOrders();
  } catch (err) {
    toast(err.message, true);
  }
}

$("orderList").addEventListener("click", e => {
  const edit = e.target.closest("[data-edit]");
  const save = e.target.closest("[data-save]");
  const stop = e.target.closest("[data-stop]");
  const cancel = e.target.closest("[data-cancel]");
  if (edit)   { editingId = Number(edit.dataset.edit); render(); }
  if (save)   saveNote(Number(save.dataset.save));
  if (stop)   { editingId = null; render(); }
  if (cancel) cancelOrder(Number(cancel.dataset.cancel));
});

// Order confirmation: index.html sends us here with ?placed=ID after checkout
const placedId = new URLSearchParams(window.location.search).get("placed");
if (placedId) {
  $("confirm").hidden = false;
  $("confirmText").textContent = "Thank you. Your order #" + placedId + " has been placed and will be prepared soon.";
}

loadOrders(true);
