// =====================================================================
// payment.js - Payment page (payment.html)
// The customer picks an unpaid order and a payment method (Card or Cash
// on delivery). The amount is NOT sent: the server reads it from the Order.
// =====================================================================

let unpaidOrders = [];
let selectedOrderId = null;

const methodInputs = document.querySelectorAll("input[name='method']");

function selectedMethod() {
  return document.querySelector("input[name='method']:checked").value;
}

function selectedOrder() {
  return unpaidOrders.find(o => o.id === selectedOrderId) || null;
}

function renderOrders() {
  if (unpaidOrders.length === 0) {
    $("orderChoices").innerHTML =
      `<p class="empty">You have no unpaid orders. <a href="index.html">Go to the menu</a> to place one.</p>`;
    return;
  }
  $("orderChoices").innerHTML = unpaidOrders.map(o => `
    <label class="choice">
      <input type="radio" name="order" value="${o.id}" ${o.id === selectedOrderId ? "checked" : ""}>
      <span class="choice-body">
        <span class="choice-title">Order #${o.id} <span class="muted">${formatDate(o.createdAt)}</span></span>
        <span class="items">${esc(o.itemsSummary)}</span>
        <span class="price">${money(o.totalAmount)}</span>
      </span>
    </label>`).join("");
}

// Shows or hides the card form and updates the three summary lines
function renderSummary() {
  const method = selectedMethod();
  $("cardFields").hidden = method !== "CARD";

  const order = selectedOrder();
  if (!order) {
    $("sumAmount").textContent = money(0);
    $("sumFee").textContent = money(0);
    $("sumTotal").textContent = money(0);
    $("payBtn").disabled = true;
    return;
  }
  const fee = previewFee(method, order.totalAmount);
  $("sumAmount").textContent = money(order.totalAmount);
  $("sumFee").textContent = money(fee);
  $("sumTotal").textContent = money(Number(order.totalAmount) + fee);
  $("payBtn").disabled = false;
}

// Orders that do not have a PAID payment yet
async function loadUnpaidOrders(firstLoad) {
  let allOrders, payments;
  try {
    allOrders = await orderApi.getByUser(USER_ID);
    payments = await paymentApi.getByUser(USER_ID);
  } catch (err) {
    if (!firstLoad) { toast(err.message, true); return; }
    useDemoData();                         // backend not reachable: use demo data
    allOrders = await orderApi.getByUser(USER_ID);
    payments = await paymentApi.getByUser(USER_ID);
  }
  const paidIds = payments.filter(p => p.status === "PAID").map(p => p.orderId);
  unpaidOrders = allOrders.filter(o => paidIds.indexOf(o.id) === -1).sort((a, b) => b.id - a.id);

  // ?order=ID comes from the [ Pay now ] link on the My orders page
  const wanted = Number(new URLSearchParams(window.location.search).get("order"));
  if (selectedOrderId === null || !selectedOrder()) {
    selectedOrderId = unpaidOrders.some(o => o.id === wanted) ? wanted
                    : (unpaidOrders.length ? unpaidOrders[0].id : null);
  }
  renderOrders();
  renderSummary();
}

// Simple checks before we call the server (the server checks again)
function buildRequest() {
  const body = { orderId: selectedOrderId, paymentType: selectedMethod() };
  if (body.paymentType === "CARD") {
    const holder = $("holder").value.trim();
    const number = $("cardNumber").value.replace(/[\s-]/g, "");
    if (!holder) throw new Error("Please enter the name on the card.");
    if (!/^\d{16}$/.test(number)) throw new Error("Card number must be 16 digits.");
    body.cardHolderName = holder;
    body.cardNumber = number;
  }
  return body;
}

async function pay() {
  $("payBtn").disabled = true;
  try {
    const payment = await paymentApi.create(buildRequest());
    $("holder").value = "";                // never keep the card number on the page
    $("cardNumber").value = "";
    $("receiptText").textContent = "Payment #" + payment.id + " for order #" + payment.orderId + ". " + payment.receiptNote;
    $("receipt").hidden = false;
    $("receipt").scrollIntoView({ behavior: "smooth", block: "start" });
    selectedOrderId = null;
    await loadUnpaidOrders();
  } catch (err) {
    toast(err.message, true);
    renderSummary();
  }
}

$("orderChoices").addEventListener("change", e => {
  if (e.target.name === "order") { selectedOrderId = Number(e.target.value); renderSummary(); }
});
methodInputs.forEach(input => input.addEventListener("change", renderSummary));

// Card number: show groups of 4 digits while typing
$("cardNumber").addEventListener("input", e => {
  const digits = e.target.value.replace(/\D/g, "").slice(0, 16);
  e.target.value = digits.replace(/(\d{4})(?=\d)/g, "$1 ");
});

$("payBtn").addEventListener("click", pay);

loadUnpaidOrders(true);
