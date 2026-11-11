(() => {
  document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('form[action="/website/order"]').forEach(form => {
      const token = document.createElement('input');
      token.type = 'hidden'; token.name = 'checkoutToken'; token.value = crypto.randomUUID();
      form.appendChild(token);
      const notice = document.createElement('p');
      notice.textContent = 'University demo: no real card charge. Card processing fee: 2%. Cash-on-delivery fee: LKR 100.';
      notice.style.cssText = 'font-size:12px;line-height:1.7;color:#715f55;margin:12px 0';
      form.prepend(notice);
      form.addEventListener('submit', event => {
        const payload = form.querySelector('[name="cartPayload"]');
        if (payload && typeof cartItems !== 'undefined') payload.value = JSON.stringify(cartItems);
        // Keep the same token on resubmission. Only a saved payment clears the UI cache.
        setTimeout(() => {
          if (!event.defaultPrevented) form.querySelectorAll('[type="submit"]').forEach(button => button.disabled = true);
        }, 0);
      }, true);
    });
    ['editOrderStatus', 'editCakeStatus', 'editCakePrice'].forEach(id => {
      const input = document.getElementById(id); if (input) input.disabled = true;
    });
    if (typeof updateLiveTicket === 'function') {
      const original = updateLiveTicket;
      let timer, sequence = 0;
      window.updateLiveTicket = function (...args) {
        original.apply(this, args);
        clearTimeout(timer);
        const request = ++sequence;
        timer = setTimeout(async () => {
          const form = document.getElementById('customCakeForm');
          if (!form) return;
          const values = Object.fromEntries(new FormData(form));
          const data = {};
          ['shape','flavor','decoration','filling','cakeSize','pickupOrDelivery','servings'].forEach(key => {
            if (values[key]) data[key] = values[key];
          });
          try {
            const response = await fetch('/api/site/bookings/quote', {
              method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(data)
            });
            if (!response.ok) return;
            const quote = await response.json();
            if (request !== sequence) return;
            const value = Number(quote.estimatedPrice);
            document.getElementById('formEstimatedPrice').value = value.toFixed(2);
            document.getElementById('ticketPriceAmount').textContent = `Rs. ${value.toLocaleString()}`;
          } catch (error) { console.warn('The bakery quote is temporarily unavailable.'); }
        }, 180);
      };
      window.updateLiveTicket();
    }
  });
})();
