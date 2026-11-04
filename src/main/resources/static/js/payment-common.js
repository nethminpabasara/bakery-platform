// =====================================================================
// payment-common.js - Payment helpers shared by the payment pages
// (payment.html, payments.html, admin-payments.html and orders.js)
// Load it AFTER common.js, because it uses request(), money(), demoOrders...
// =====================================================================

const PAYMENT_URL = "/api/payments";

const PAYMENT_TYPE_LABEL = { CARD: "Card", CASH_ON_DELIVERY: "Cash on delivery" };
const PAYMENT_STATUS_LABEL = { PAID: "Paid", REFUNDED: "Refunded" };

// Only a PREVIEW for the customer. The real fee is calculated by Spring Boot
// (CardPayment.calculateFee = 2%, CashOnDelivery.calculateFee = flat 100).
function previewFee(type, amount) {
  if (type === "CARD") return Math.round(Number(amount) * 2) / 100;   // 2% with 2 decimals
  return 100;
}

// ---------------------------------------------------------------------
// Payment API (real backend)
// ---------------------------------------------------------------------
const livePaymentApi = {
  create(body)         { return request("POST", PAYMENT_URL, body); },
  getAll(status)       { return request("GET", PAYMENT_URL + (status ? "?status=" + status : "")); },
  getByUser(userId)    { return request("GET", PAYMENT_URL + "/user/" + userId); },
  getByOrder(orderId)  { return request("GET", PAYMENT_URL + "/order/" + orderId); },
  getById(id)          { return request("GET", PAYMENT_URL + "/" + id); },
  refund(id)           { return request("PUT", PAYMENT_URL + "/" + id + "/status", { status: "REFUNDED" }); },
  remove(id)           { return request("DELETE", PAYMENT_URL + "/" + id); }
};

// ---------------------------------------------------------------------
// Payment API (demo data, only used when the backend is not reachable)
// It follows the same rules as PaymentService in Spring Boot.
// ---------------------------------------------------------------------
let demoPayments = [
  { id: 3, createdAt: "2026-10-23T14:20:00", orderId: 3, amount: 2200, fee: 44.00, totalCharged: 2244.00, status: "REFUNDED", paymentType: "CARD", cardHolderName: "A. Perera", cardLast4: "4242", receiptNote: "Rs. 2244.00 charged to the card ending 4242." },
  { id: 2, createdAt: "2026-10-24T16:10:00", orderId: 2, amount: 3200, fee: 100.00, totalCharged: 3300.00, status: "PAID", paymentType: "CASH_ON_DELIVERY", receiptNote: "Please pay Rs. 3300.00 in cash when the order is delivered." },
  { id: 1, createdAt: "2026-10-22T11:35:00", orderId: 1, amount: 4790, fee: 95.80, totalCharged: 4885.80, status: "PAID", paymentType: "CARD", cardHolderName: "A. Perera", cardLast4: "1111", receiptNote: "Rs. 4885.80 charged to the card ending 1111." }
];
let demoPaymentSeq = 3;

function findDemoPayment(id) {
  const payment = demoPayments.find(p => p.id === id);
  if (!payment) throw new Error("Payment not found with id " + id);
  return payment;
}

const demoPaymentApi = {
  async create(body) {
    const order = demoOrders.find(o => o.id === body.orderId);
    if (!order) throw new Error("Order not found with id " + body.orderId);
    if (demoPayments.some(p => p.orderId === order.id && p.status === "PAID")) {
      throw new Error("This order is already paid.");
    }
    const fee = previewFee(body.paymentType, order.totalAmount);
    const payment = {
      id: ++demoPaymentSeq, createdAt: new Date().toISOString(), orderId: order.id,
      amount: order.totalAmount, fee: fee, totalCharged: order.totalAmount + fee,
      status: "PAID", paymentType: body.paymentType
    };
    if (body.paymentType === "CARD") {
      if (!body.cardHolderName || !body.cardNumber) {
        throw new Error("Card holder name and a 16-digit card number are required for card payments.");
      }
      if (!/^\d{16}$/.test(body.cardNumber)) throw new Error("Card number must be 16 digits");
      payment.cardHolderName = body.cardHolderName;
      payment.cardLast4 = body.cardNumber.slice(-4);
      payment.receiptNote = "Rs. " + payment.totalCharged.toFixed(2) + " charged to the card ending " + payment.cardLast4 + ".";
    } else {
      payment.receiptNote = "Please pay Rs. " + payment.totalCharged.toFixed(2) + " in cash when the order is delivered.";
    }
    demoPayments.push(payment);
    return payment;
  },
  async getAll(status)      { return demoPayments.filter(p => !status || p.status === status); },
  async getByOrder(orderId) { return demoPayments.filter(p => p.orderId === orderId); },
  async getByUser(userId) {
    const ids = demoOrders.filter(o => o.userId === userId).map(o => o.id);
    return demoPayments.filter(p => ids.indexOf(p.orderId) !== -1);
  },
  async getById(id) { return findDemoPayment(id); },
  async refund(id) {
    const payment = findDemoPayment(id);
    if (payment.status !== "PAID") throw new Error("A payment cannot move from " + payment.status + " to REFUNDED.");
    payment.status = "REFUNDED";
    return payment;
  },
  async remove(id) {
    const payment = findDemoPayment(id);
    if (payment.status !== "REFUNDED") throw new Error("Only refunded payments can be deleted.");
    demoPayments = demoPayments.filter(p => p.id !== id);
  }
};

let paymentApi = livePaymentApi;       // switches to demoPaymentApi if the backend is down

// Both APIs must switch together, because demo payments read the demo orders
function useDemoData() {
  orderApi = demoOrderApi;
  paymentApi = demoPaymentApi;
  $("demoBanner").hidden = false;
}
