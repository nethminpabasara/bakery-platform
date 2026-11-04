// =====================================================================
// admin-payments.js - Admin payment list (admin-payments.html)
// The admin sees every payment, filters by status, finds the payments of
// one order, refunds a PAID payment, or deletes a REFUNDED payment.
// (Role-based access will be added when Member 2's login is ready.)
// =====================================================================

let payments = [];
let filter = "";
let searchOrderId = null;      // when set, only the payments of this order are shown

function render() {
  if (payments.length === 0) {
    $("paymentRows").innerHTML = `<tr><td colspan="9" class="empty">No payments found.</td></tr>`;
    return;
  }
  const newestFirst = payments.slice().sort((a, b) => b.id - a.id);
  $("paymentRows").innerHTML = newestFirst.map(p => `
    <tr>
      <td data-label="Payment">#${p.id}</td>
      <td data-label="Date">${formatDate(p.createdAt)}</td>
      <td data-label="Order">#${p.orderId}</td>
      <td data-label="Method">${PAYMENT_TYPE_LABEL[p.paymentType] || esc(p.paymentType)}${p.cardLast4 ? `<div class="muted">Card ending ${esc(p.cardLast4)}</div>` : ""}</td>
      <td data-label="Amount" class="num">${money(p.amount)}</td>
      <td data-label="Fee" class="num">${money(p.fee)}</td>
      <td data-label="Total" class="num">${money(p.totalCharged)}</td>
      <td data-label="Status"><span class="pill pill-${p.status.toLowerCase()}">${PAYMENT_STATUS_LABEL[p.status]}</span></td>
      <td data-label="Action"><div class="action-box">
        ${p.status === "PAID" ? `<button class="btn" type="button" data-refund="${p.id}">[ Refund ]</button>` : ""}
        ${p.status === "REFUNDED" ? `<button class="btn" type="button" data-delete="${p.id}">[ Delete ]</button>` : ""}
      </div></td>
    </tr>`).join("");
}

async function fetchPayments() {
  const list = searchOrderId === null ? await paymentApi.getAll(filter) : await paymentApi.getByOrder(searchOrderId);
  // the status filter also applies to the order search
  return filter ? list.filter(p => p.status === filter) : list;
}

async function loadPayments(firstLoad) {
  try {
    payments = await fetchPayments();
  } catch (err) {
    if (!firstLoad) { toast(err.message, true); return; }
    useDemoData();                         // backend not reachable: use demo data
    payments = await fetchPayments();
  }
  render();
}

async function refund(id) {
  if (!confirm("Refund payment #" + id + "? A refunded payment cannot become paid again.")) return;
  try {
    await paymentApi.refund(id);
    toast("Payment #" + id + " refunded.");
    await loadPayments();
  } catch (err) {
    toast(err.message, true);
  }
}

async function removePayment(id) {
  if (!confirm("Delete payment #" + id + "? This cannot be undone.")) return;
  try {
    await paymentApi.remove(id);
    toast("Payment #" + id + " deleted.");
    await loadPayments();
  } catch (err) {
    toast(err.message, true);
  }
}

function findByOrder() {
  const value = Number($("orderSearch").value);
  if (!value || value < 1) { toast("Enter an order number.", true); return; }
  searchOrderId = value;
  $("clearBtn").hidden = false;
  loadPayments();
}

function showAll() {
  searchOrderId = null;
  $("orderSearch").value = "";
  $("clearBtn").hidden = true;
  loadPayments();
}

$("paymentRows").addEventListener("click", e => {
  const refundBtn = e.target.closest("[data-refund]");
  const deleteBtn = e.target.closest("[data-delete]");
  if (refundBtn) refund(Number(refundBtn.dataset.refund));
  if (deleteBtn) removePayment(Number(deleteBtn.dataset.delete));
});

$("statusFilter").addEventListener("change", e => { filter = e.target.value; loadPayments(); });
$("findBtn").addEventListener("click", findByOrder);
$("orderSearch").addEventListener("keydown", e => { if (e.key === "Enter") findByOrder(); });
$("clearBtn").addEventListener("click", showAll);

loadPayments(true);
