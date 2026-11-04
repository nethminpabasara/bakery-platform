// =====================================================================
// payments.js - "My payments" page (payments.html)
// Payment history of one customer. [ View details ] reads one payment
// from the server (GET /api/payments/{id}).
// =====================================================================

let payments = [];
const detailsById = {};        // payment id -> details loaded from the server

function detailsHtml(p) {
  const rows = [
    ["Order", "#" + p.orderId],
    ["Method", PAYMENT_TYPE_LABEL[p.paymentType] || p.paymentType],
    ["Order amount", money(p.amount)],
    ["Fee", money(p.fee)],
    ["Total charged", money(p.totalCharged)]
  ];
  if (p.paymentType === "CARD") {
    rows.push(["Name on card", esc(p.cardHolderName)]);
    rows.push(["Card ending", esc(p.cardLast4)]);
  }
  rows.push(["Recorded", formatDate(p.createdAt)]);
  return `<dl class="facts">${rows.map(r => `<div><dt>${r[0]}</dt><dd>${r[1]}</dd></div>`).join("")}</dl>`;
}

function render() {
  if (payments.length === 0) {
    $("paymentList").innerHTML = `<p class="empty">You have no payments yet. <a href="payment.html">Pay for an order</a>.</p>`;
    return;
  }
  const newestFirst = payments.slice().sort((a, b) => b.id - a.id);
  $("paymentList").innerHTML = newestFirst.map(p => `
    <article class="order">
      <header class="order-head">
        <div>
          <h3>Payment #${p.id}</h3>
          <div class="muted">Order #${p.orderId} &middot; ${PAYMENT_TYPE_LABEL[p.paymentType] || esc(p.paymentType)} &middot; ${formatDate(p.createdAt)}</div>
        </div>
        <span class="pill pill-${p.status.toLowerCase()}">${PAYMENT_STATUS_LABEL[p.status]}</span>
      </header>
      <p class="items">${esc(p.receiptNote)}</p>
      ${detailsById[p.id] ? detailsHtml(detailsById[p.id]) : ""}
      <footer class="order-foot">
        <span class="total">${money(p.totalCharged)}</span>
        <button class="btn" type="button" data-details="${p.id}">${detailsById[p.id] ? "[ Hide details ]" : "[ View details ]"}</button>
      </footer>
    </article>`).join("");
}

async function loadPayments(firstLoad) {
  try {
    payments = await paymentApi.getByUser(USER_ID);
  } catch (err) {
    if (!firstLoad) { toast(err.message, true); return; }
    useDemoData();                         // backend not reachable: use demo data
    payments = await paymentApi.getByUser(USER_ID);
  }
  render();
}

async function toggleDetails(id) {
  if (detailsById[id]) {                   // second click hides the details
    delete detailsById[id];
    render();
    return;
  }
  try {
    detailsById[id] = await paymentApi.getById(id);
    render();
  } catch (err) {
    toast(err.message, true);
  }
}

$("paymentList").addEventListener("click", e => {
  const btn = e.target.closest("[data-details]");
  if (btn) toggleDetails(Number(btn.dataset.details));
});

loadPayments(true);
