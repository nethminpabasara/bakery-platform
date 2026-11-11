package com.bakery.bakeryplatform.service.order;

import com.bakery.bakeryplatform.model.catalogue.SiteStock;
import com.bakery.bakeryplatform.repository.catalogue.SiteStockRepository;
import com.bakery.bakeryplatform.service.catalogue.SiteCatalogue;
import com.bakery.bakeryplatform.service.user.SiteIdentity;

import com.bakery.bakeryplatform.model.CartItem;
import com.bakery.bakeryplatform.model.order.*;
import com.bakery.bakeryplatform.model.payment.*;
import com.bakery.bakeryplatform.dto.payment.PaymentRequest;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.repository.payment.PaymentRepository;
import com.bakery.bakeryplatform.service.CartService;

import com.bakery.bakeryplatform.service.payment.PaymentService;
import com.bakery.bakeryplatform.model.user.Customer;
import com.bakery.bakeryplatform.model.user.Delivery;
import com.bakery.bakeryplatform.repository.user.DeliveryRepository;
import com.bakery.bakeryplatform.repository.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import java.util.*;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.*;

@Service
public class SiteOrders {
    private final SiteIdentity identity;private final SiteCatalogue catalogue;private final SiteStockRepository stocks;
    private final CartService cart;private final OrderService service;private final OrderRepository orders;private final PaymentService payments;
    private final PaymentRepository paymentRows;private final DeliveryRepository deliveries;private final UserRepository users;private final ObjectMapper mapper;
    public SiteOrders(SiteIdentity identity,SiteCatalogue catalogue,SiteStockRepository stocks,CartService cart,OrderService service,OrderRepository orders,PaymentService payments,PaymentRepository paymentRows,DeliveryRepository deliveries,UserRepository users,ObjectMapper mapper){this.identity=identity;this.catalogue=catalogue;this.stocks=stocks;this.cart=cart;this.service=service;this.orders=orders;this.payments=payments;this.paymentRows=paymentRows;this.deliveries=deliveries;this.users=users;this.mapper=mapper;}
    public Order owned(Long id,HttpSession session){var order=orders.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Order not found."));identity.owner(order.getUserId(),session);return order;}
    public Map<String,Object> payment(Payment p){return map("id",p.getId(),"paymentId",p.getId().toString(),"orderId",p.getOrderId().toString(),"amount",p.getTotalCharged(),"paymentMethod",p.getPaymentType().equals("CARD")?"Card Payment":"PAY_AFTER_DELIVERY","paymentStatus",p.getStatus().name(),"paymentDate",p.getCreatedAt(),"transactionReference","PAY-"+p.getId());}
    public Map<String,Object> dto(Order order){
        var p=paymentRows.findByOrderId(order.getId()).stream().reduce((a,b)->b).orElse(null);
        var customer=users.findById(order.getUserId()).orElse(null);List<Map<String,Object>> items=List.of();
        if(order.getItemsJson()!=null)try{items=mapper.readValue(order.getItemsJson(),new TypeReference<>(){});}catch(Exception ignored){}
        return map("id",order.getId(),"orderId",order.getId().toString(),"customerName",order.getCustomerName()!=null?order.getCustomerName():customer==null?"Customer":customer.getUsername(),"customerEmail",order.getCustomerEmail(),"customerPhone",order.getCustomerPhone(),"deliveryAddress",order.getDeliveryAddress(),"deliveryType",order.getDeliveryType(),"orderDate",order.getCreatedAt().toString(),"status",order.getStatus().name(),"paymentStatus",p==null?"PENDING":p.getStatus().name(),"paymentMethod",order.getPaymentMethod(),"itemsSummaryDisplay",order.getItemsSummary(),"itemsSummary",order.getItemsSummary(),"items",items,"totalAmount",order.getTotalAmount(),"subtotal",order.getTotalAmount(),"discount",0,"notes",order.getNote());
    }
    @Transactional public Long checkout(Map<String,String> form,HttpSession session){
        Customer customer=identity.customer(session);users.lockForWebsiteCheckout(customer.getId()).orElseThrow();
        String token=form.getOrDefault("checkoutToken","");try{UUID.fromString(token);}catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"Please reopen checkout and try again.");}
        var existing=orders.findByCheckoutToken(token);
        if(existing.isPresent()){identity.owner(existing.get().getUserId(),session);return paymentRows.findByOrderId(existing.get().getId()).getFirst().getId();}
        String method=form.getOrDefault("paymentMethod","Card Payment");boolean cash=method.equals("PAY_AFTER_DELIVERY")||method.toUpperCase().contains("CASH");
        String number=form.getOrDefault("cardNumber","").replaceAll("\\s","");String holder=form.getOrDefault("cardHolderName","").trim();
        if(!cash&&(!number.matches("\\d{16}")||holder.isBlank()))throw error(HttpStatus.BAD_REQUEST,"A name and a 16-digit test card number are required.");
        List<Map<String,Object>> input;
        try{input=mapper.readValue(form.getOrDefault("cartPayload","[]"),new TypeReference<>(){});}catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"The basket could not be read.");}
        if(input.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Your basket is empty.");
        cart.clearCart(customer.getId());Map<String,Integer> required=new TreeMap<>();List<Map<String,Object>> snapshots=new ArrayList<>();
        for(var row:input){
            String id=text(row,"productId",text(row,"id",""));var product=catalogue.find(SiteCatalogue.baseProductId(id));
            String size=text(row,"selectedSize",catalogue.cake(product)?"1 kg":"Regular");
            int quantity=(int)SiteCatalogue.number(row.getOrDefault("quantity",row.getOrDefault("qty",1)));
            if(quantity<1||quantity>50)throw error(HttpStatus.BAD_REQUEST,"Choose quantities from 1 to 50.");
            var price=catalogue.price(product,size);var item=new CartItem(customer.getId(),product.getId()+"::"+size,product.getName(),quantity,price);
            item.setSelectedSize(size);item.setProductType(catalogue.cake(product)?"CAKE":"BAKERY");item.setImageUrl(product.getImageUrl());cart.addToCart(item);
            if(!catalogue.cake(product))required.merge(product.getId(),quantity,Integer::sum);
            snapshots.add(map("productId",product.getId(),"productName",product.getName(),"quantity",quantity,"selectedSize",size,"unitPrice",price,"subtotal",price.multiply(java.math.BigDecimal.valueOf(quantity)),"imageUrl",product.getImageUrl()));
        }
        // Lock inventory in product-ID order, in the same H2 transaction as payment.
        List<SiteStock> reserved=new ArrayList<>();for(var entry:required.entrySet()){
            var stock=stocks.lock(entry.getKey()).orElseThrow(()->error(HttpStatus.CONFLICT,"Product stock is unavailable."));
            if(stock.getQuantity()<entry.getValue())throw error(HttpStatus.CONFLICT,"There is not enough stock for this order.");
            stock.setQuantity(stock.getQuantity()-entry.getValue());reserved.add(stock);
        }
        Order order=service.placeOrder(customer.getId(),form.getOrDefault("notes",""));order.setCheckoutToken(token);
        order.setCustomerName(form.getOrDefault("customerName",customer.getUsername()));order.setCustomerPhone(form.getOrDefault("customerPhone",customer.getPhoneNumber()));
        order.setCustomerEmail(customer.getEmail());order.setDeliveryAddress(form.getOrDefault("deliveryAddress",customer.getAddress()));
        order.setDeliveryType(order.getDeliveryAddress()!=null&&order.getDeliveryAddress().toLowerCase().contains("pickup")?"Pickup":"Delivery");
        order.setPaymentMethod(cash?"PAY_AFTER_DELIVERY":"Card Payment");
        try{order.setItemsJson(mapper.writeValueAsString(snapshots));}catch(Exception e){throw new IllegalStateException(e);}
        orders.save(order);Payment payment=payments.create(new PaymentRequest(order.getId(),cash?"CASH_ON_DELIVERY":"CARD",holder,number.isBlank()?null:number),customer.getId());stocks.saveAll(reserved);
        if(order.getDeliveryType().equals("Delivery")){var delivery=new Delivery();delivery.setOrderId(order.getId());delivery.setCustomer(customer);delivery.setRecipientName(order.getCustomerName());delivery.setAddress(order.getDeliveryAddress());delivery.setStatus("ASSIGNED");deliveries.save(delivery);}
        return payment.getId();
    }
    @Transactional public void cancel(Order order){
        if(order.getStatus()!=OrderStatus.PENDING&&order.getStatus()!=OrderStatus.CONFIRMED)throw error(HttpStatus.CONFLICT,"Only orders awaiting preparation can be cancelled.");
        if(paymentRows.findByOrderId(order.getId()).stream().anyMatch(p->p.getStatus()==PaymentStatus.PAID))throw error(HttpStatus.CONFLICT,"Please request a refund before cancelling a paid order.");
        order.setStatus(OrderStatus.CANCELLED);orders.save(order);
    }
}
