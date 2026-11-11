(() => {
  const byId = (id) => document.getElementById(id);
  const urlParams = new URLSearchParams(window.location.search);
  const paymentIdParam = urlParams.get('paymentId');
  const orderIdParam = urlParams.get('orderId');

  let currentConfirmation = null;
  let currentOrder = null;
  let pollCount = 0;
  let pollTimer = null;

  // Image lookup fallback for common bakery items and cakes
  const KNOWN_BAKERY_IMAGES = {
    'chicken patty': '/images/bakery-items/Chicken Patty.png',
    'classic chicken burger': '/images/bakery-items/Burger.png',
    'burger': '/images/bakery-items/Burger.png',
    'cream bun': '/images/bakery-items/Cream Bun.png',
    'glazed donut': '/images/bakery-items/Donut.png',
    'donut': '/images/bakery-items/Donut.png',
    'egg bun': '/images/bakery-items/Egg Bun.png',
    'sri lankan fish bun': '/images/bakery-items/Fish Bun.png',
    'fish bun': '/images/bakery-items/Fish Bun.png',
    'fish cutlet': '/images/bakery-items/Fish Cutlet.png',
    'fish roll': '/images/bakery-items/Fish Roll.png',
    'classic hot dog': '/images/bakery-items/Hot Dog.png',
    'hot dog': '/images/bakery-items/Hot Dog.png',
    'jam bun': '/images/bakery-items/Jam Bun.png',
    'vegetable samosa': '/images/bakery-items/Samosa.png',
    'samosa': '/images/bakery-items/Samosa.png',
    'fresh chicken sandwich': '/images/bakery-items/Sandwich.png',
    'sandwich': '/images/bakery-items/Sandwich.png',
    'sausage bun': '/images/bakery-items/Sausage Bun.png',
    'seeni sambol bun': '/images/bakery-items/Seeni Sambol Bun.png',
    'vegetable roti': '/images/bakery-items/Vegitable Roti.png',
    'vegitable roti': '/images/bakery-items/Vegitable Roti.png',
    'classic butter croissant': '/images/bakery-items/Classic Butter Croissant.png'
  };

  const money = (value) => {
    const num = Number(value) || 0;
    return 'LKR ' + num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  async function request(url, options = {}) {
    const response = await fetch(url, {
      credentials: 'same-origin',
      ...options,
      headers: { Accept: 'application/json', ...options.headers }
    });
    if (!response.ok) {
      const error = new Error(
        response.status === 401
          ? 'Please sign in to see your order confirmation.'
          : response.status === 403 || response.status === 404
            ? 'This order confirmation is not available for your account.'
            : 'We could not load the order. Please try again from My orders.'
      );
      error.status = response.status;
      throw error;
    }
    return response.json();
  }

  function resolveItemImage(name, explicitUrl) {
    if (explicitUrl && explicitUrl.trim() && explicitUrl !== '/images/placeholder.svg') {
      return explicitUrl.trim();
    }
    const clean = (name || '').toLowerCase().trim();
    if (KNOWN_BAKERY_IMAGES[clean]) {
      return KNOWN_BAKERY_IMAGES[clean];
    }
    // Partial search in map
    for (const [key, path] of Object.entries(KNOWN_BAKERY_IMAGES)) {
      if (clean.includes(key) || key.includes(clean)) {
        return path;
      }
    }
    return '/images/placeholder.svg';
  }

  function parseItemsFromSummary(summary) {
    if (!summary || typeof summary !== 'string') return [];
    const parts = summary.split(',').map(s => s.trim()).filter(Boolean);
    return parts.map(part => {
      // Matches pattern: "1 x Chicken Patty" or "2 x Cream Bun" or "Chicken Patty"
      const match = part.match(/^(\d+)\s*[xX×]\s*(.+)$/);
      if (match) {
        const qty = parseInt(match[1], 10) || 1;
        const name = match[2].trim();
        return {
          productName: name,
          quantity: qty,
          unitPrice: null,
          subtotal: null,
          imageUrl: resolveItemImage(name, null)
        };
      }
      return {
        productName: part,
        quantity: 1,
        unitPrice: null,
        subtotal: null,
        imageUrl: resolveItemImage(part, null)
      };
    });
  }

  function renderOrderedItems(items) {
    const container = byId('orderItemsContainer');
    if (!container) return;
    container.innerHTML = '';

    if (!items || items.length === 0) {
      container.innerHTML = `
        <div class="receipt-item-row">
          <div class="item-thumb-wrapper">
            <img class="item-thumbnail" src="/images/placeholder.svg" alt="Bakery Order" />
          </div>
          <div class="item-info-col">
            <h4 class="item-name-heading">Your Bakery Order</h4>
            <div class="item-quantity-text">Fresh handcrafted bakery delicacies</div>
          </div>
        </div>
      `;
      return;
    }

    items.forEach(item => {
      const name = item.productName || item.name || 'Bakery Item';
      const quantity = item.quantity || item.qty || 1;
      const imageUrl = resolveItemImage(name, item.imageUrl || item.imagePath);
      const size = item.selectedSize || item.size;
      const unitPriceStr = item.unitPrice != null ? money(item.unitPrice) : null;
      const subtotalStr = item.subtotal != null ? money(item.subtotal) : (item.unitPrice != null ? money(item.unitPrice * quantity) : null);

      const row = document.createElement('div');
      row.className = 'receipt-item-row';

      // Custom cake specs if present
      let cakeSpecsHtml = '';
      const isCustomCake = item.shape || item.flavor || item.decoration || item.filling || item.cakeMessage;
      if (isCustomCake) {
        const specs = [];
        if (item.shape) specs.push(`Shape: ${item.shape}`);
        if (item.flavor) specs.push(`Flavor: ${item.flavor}`);
        if (item.selectedSize) specs.push(`Size: ${item.selectedSize}`);
        if (item.decoration) specs.push(`Decoration: ${item.decoration}`);
        if (item.cakeMessage) specs.push(`Message: "${item.cakeMessage}"`);
        if (specs.length > 0) {
          cakeSpecsHtml = `<div class="cake-custom-specs">${specs.join(' · ')}</div>`;
        }
      }

      row.innerHTML = `
        <div class="item-thumb-wrapper">
          <img class="item-thumbnail" src="${encodeURI(imageUrl)}" alt="${name}" onerror="this.src='/images/placeholder.svg';" />
        </div>
        <div class="item-info-col">
          <h4 class="item-name-heading">${name}</h4>
          <div class="item-quantity-text">
            <span>${quantity} × ${unitPriceStr || name}</span>
            ${size && size !== 'Regular' ? `<span class="item-size-badge">${size}</span>` : ''}
          </div>
          ${cakeSpecsHtml}
        </div>
        ${subtotalStr ? `<div class="item-subtotal-val">${subtotalStr}</div>` : ''}
      `;

      container.appendChild(row);
    });
  }

  function render(details, orderData = null) {
    currentConfirmation = details;
    currentOrder = orderData;

    // Clear local shopping carts upon reaching receipt
    ['sweet_bloom_cart', 'sweet_bloom_bakery_cart', 'sweet_bloom_cake_cart'].forEach(key => {
      try { localStorage.removeItem(key); } catch (e) { }
    });

    const isPending = details.status === 'PENDING';
    const isRefunded = details.status === 'REFUNDED';
    const isPaid = details.status === 'PAID';
    const isCash = details.paymentType === 'CASH_ON_DELIVERY' || (orderData && orderData.paymentMethod && orderData.paymentMethod.includes('PAY_AFTER_DELIVERY'));

    // Reveal container
    byId('loading').hidden = true;
    byId('pageError').hidden = true;
    byId('confirmation').hidden = false;
    byId('nextSteps').hidden = false;

    // 1. Order Number & Status
    const orderId = details.orderId || (orderData ? orderData.orderId : '');
    byId('orderNumber').textContent = `Order #${orderId}`;

    // 2. Order Status Badge (Tab)
    const orderStatusBadge = byId('orderStatusBadge');
    const rawOrderStatus = (orderData && orderData.status) ? orderData.status : (isPaid ? 'PAID' : (isPending ? 'PENDING' : 'CONFIRMED'));
    orderStatusBadge.dataset.status = rawOrderStatus;

    // Human readable order status map
    const orderStatusLabels = {
      'PENDING': isCash ? 'Awaiting Cash' : 'Payment Pending',
      'PAID': 'Paid',
      'CONFIRMED': 'Confirmed',
      'PREPARING': 'Preparing',
      'READY': 'Ready',
      'COMPLETED': 'Completed',
      'CANCELLED': 'Cancelled',
      'REFUNDED': 'Refunded'
    };
    orderStatusBadge.textContent = orderStatusLabels[rawOrderStatus] || rawOrderStatus;

    // 3. Payment Status Pill
    const paymentStatusEl = byId('paymentStatus');
    paymentStatusEl.dataset.status = details.status;
    paymentStatusEl.textContent = isPending
      ? (isCash ? 'Awaiting Cash' : 'Payment Pending')
      : isRefunded ? 'Refunded' : 'Paid';

    // 4. Subtext / Invoice info
    byId('invoiceNumber').textContent = details.invoiceNumber
      ? `Invoice ${details.invoiceNumber}`
      : (isPending ? 'Your paid invoice follows after cash is received.' : 'Payment received. Your order is confirmed.');

    // 5. Dynamic Confirmation Message on LEFT CARD
    const orderMessageEl = byId('orderMessage');
    const finalTotal = details.totalCharged != null ? details.totalCharged : (orderData ? orderData.totalAmount : 0);

    if (isPending) {
      if (isCash) {
        orderMessageEl.innerHTML = `Your order is saved. Please pay <strong>${money(finalTotal)}</strong> when it arrives.`;
      } else {
        orderMessageEl.textContent = 'Your order has been received. Your payment is currently pending.';
      }
    } else if (isRefunded) {
      orderMessageEl.textContent = 'This payment has been marked refunded. Your original invoice remains available.';
    } else {
      // Successful online payment
      orderMessageEl.textContent = 'Your order has been successfully placed. Thank you for choosing Sweet Bloom!';
    }

    // 6. Ordered Items
    let itemsToRender = [];
    if (orderData && Array.isArray(orderData.items) && orderData.items.length > 0) {
      itemsToRender = orderData.items;
    } else if (details.itemsSummary) {
      itemsToRender = parseItemsFromSummary(details.itemsSummary);
    }
    renderOrderedItems(itemsToRender);

    // Keep legacy orderItems element updated for compatibility
    if (byId('orderItems')) {
      byId('orderItems').textContent = details.itemsSummary || (orderData ? orderData.itemsSummary : '');
    }

    // 7. Price Breakdown
    const subtotal = details.amount != null ? details.amount : (orderData ? orderData.subtotal || orderData.totalAmount : 0);
    byId('subtotal').textContent = money(subtotal);

    // Fee line
    const feeRow = byId('feeRow');
    if (details.fee && Number(details.fee) > 0) {
      feeRow.style.display = 'flex';
      byId('fee').textContent = money(details.fee);
    } else {
      feeRow.style.display = 'none';
    }

    // Delivery fee line (if applicable)
    const deliveryFeeRow = byId('deliveryFeeRow');
    if (orderData && orderData.deliveryFee && Number(orderData.deliveryFee) > 0) {
      deliveryFeeRow.style.display = 'flex';
      byId('deliveryFee').textContent = money(orderData.deliveryFee);
    } else {
      deliveryFeeRow.style.display = 'none';
    }

    // Discount line (if applicable)
    const discountRow = byId('discountRow');
    if (orderData && orderData.discount && Number(orderData.discount) > 0) {
      discountRow.style.display = 'flex';
      byId('discount').textContent = `-${money(orderData.discount)}`;
    } else {
      discountRow.style.display = 'none';
    }

    // Highlight Total banner
    byId('total').textContent = money(finalTotal);
    byId('totalLabel').textContent = isPending
      ? 'Due on delivery'
      : isRefunded ? 'Original payment' : 'Total paid';

    // 8. Payment Method
    byId('paymentMethod').textContent = isCash ? 'Cash on delivery' : 'Card Payment';

    // 9. Demo Notice
    byId('demoNotice').hidden = !details.demoPayment;

    // 10. Download Invoice Link
    const downloadContainer = byId('downloadInvoiceContainer');
    if (details.invoiceId) {
      downloadContainer.hidden = false;
      byId('downloadInvoice').href = `/api/invoices/${encodeURIComponent(details.invoiceId)}/pdf`;
    } else {
      downloadContainer.hidden = true;
    }

    // 11. Invoice Email Status Notification
    byId('retryEmail').hidden = true;
    if (isPending) {
      byId('emailHeading').textContent = 'Your invoice follows payment';
      byId('emailMessage').textContent = 'We’ll prepare your paid invoice after the cash has been collected.';
    } else if (details.emailStatus === 'SENT') {
      byId('emailHeading').textContent = 'Your invoice has been emailed';
      byId('emailMessage').textContent = `Sent to ${details.customerEmail}. Check your inbox or spam folder.`;
    } else if (details.emailStatus === 'PENDING') {
      byId('emailHeading').textContent = 'Your invoice is on its way';
      byId('emailMessage').textContent = `We’re preparing the invoice email for ${details.customerEmail}. You can also download it here.`;
    } else if (details.invoiceId) {
      byId('emailHeading').textContent = 'Your invoice is ready to download';
      byId('emailMessage').textContent = details.emailStatus === 'FAILED'
        ? 'We couldn’t send the invoice email. Your payment is saved; download your invoice or try email again.'
        : 'Email delivery is currently unavailable. Your invoice is saved and ready below.';
      byId('retryEmail').hidden = isRefunded;
    } else {
      byId('emailNoticeBox').style.display = 'none';
    }
  }

  async function load(background = false) {
    try {
      let details = null;
      let orderData = null;

      if (paymentIdParam && /^\d+$/.test(paymentIdParam)) {
        // Load payment confirmation
        details = await request(`/api/payments/${encodeURIComponent(paymentIdParam)}/confirmation`);

        // Next, try to fetch rich order data using orderId from payment details
        if (details && details.orderId) {
          try {
            orderData = await request(`/api/site/orders/${encodeURIComponent(details.orderId)}`);
          } catch (e) {
            console.log('Order detail lookup:', e.message);
          }
        }
      } else if (orderIdParam && /^\d+$/.test(orderIdParam)) {
        // Look up payment by order id
        try {
          const paymentRecord = await request(`/api/site/payments/order/${encodeURIComponent(orderIdParam)}`);
          if (paymentRecord && paymentRecord.paymentId) {
            details = await request(`/api/payments/${encodeURIComponent(paymentRecord.paymentId)}/confirmation`);
          }
        } catch (e) { }

        // Look up order directly
        try {
          orderData = await request(`/api/site/orders/${encodeURIComponent(orderIdParam)}`);
        } catch (e) { }

        if (!details && orderData) {
          // Construct fallback details from orderData
          const isCash = (orderData.paymentMethod || '').includes('PAY_AFTER_DELIVERY') || (orderData.paymentMethod || '').includes('CASH');
          details = {
            paymentId: orderData.id,
            orderId: orderData.id,
            status: orderData.paymentStatus || 'PENDING',
            paymentType: isCash ? 'CASH_ON_DELIVERY' : 'CARD',
            amount: orderData.totalAmount || orderData.subtotal || 0,
            fee: isCash ? 100 : 0,
            totalCharged: (orderData.totalAmount || 0) + (isCash ? 100 : 0),
            itemsSummary: orderData.itemsSummary || '',
            invoiceId: null,
            invoiceNumber: null,
            customerEmail: orderData.customerEmail || '',
            emailStatus: 'NOT_REQUESTED',
            demoPayment: !isCash
          };
        }
      }

      if (!details) {
        throw new Error('Please select an order from your payment or order history.');
      }

      render(details, orderData);

      // Poll if invoice email is still pending
      if (currentConfirmation && currentConfirmation.emailStatus === 'PENDING' && pollCount++ < 30) {
        pollTimer = window.setTimeout(() => load(true), 2500);
      }
    } catch (error) {
      if (background) {
        byId('emailHeading').textContent = 'Your invoice status is unavailable';
        byId('emailMessage').textContent = 'Your payment remains saved. Reopen this page from My payments to check delivery.';
        return;
      }
      byId('loading').hidden = true;
      byId('confirmation').hidden = true;
      byId('nextSteps').hidden = true;
      byId('pageError').hidden = false;
      byId('errorText').textContent = error.message;
      if (error.status === 401) {
        byId('errorLink').href = `/login.html?returnUrl=${encodeURIComponent(window.location.pathname + window.location.search)}`;
        byId('errorLink').textContent = 'Sign In';
      }
    }
  }

  byId('retryEmail').addEventListener('click', async () => {
    const button = byId('retryEmail');
    button.disabled = true;
    window.clearTimeout(pollTimer);
    try {
      await request(`/api/invoices/${encodeURIComponent(currentConfirmation.invoiceId)}/email`, { method: 'POST' });
      pollCount = 0;
      await load(true);
    } catch (error) {
      byId('emailMessage').textContent = error.message;
    } finally {
      button.disabled = false;
    }
  });

  load();
})();
