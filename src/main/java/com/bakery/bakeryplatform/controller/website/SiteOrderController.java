package com.bakery.bakeryplatform.controller.website;

import com.bakery.bakeryplatform.service.order.SiteOrders;
import com.bakery.bakeryplatform.service.user.SiteIdentity;

import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.repository.payment.PaymentRepository;
import com.bakery.bakeryplatform.model.order.OrderStatus;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.service.payment.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.*;

@RestController @RequestMapping("/api/site")
public class SiteOrderController {
    private final SiteOrders site;private final SiteIdentity identity;private final OrderRepository orders;private final PaymentRepository payments;private final PaymentService paymentService;
    public SiteOrderController(SiteOrders site,SiteIdentity identity,OrderRepository orders,PaymentRepository payments,PaymentService paymentService){this.site=site;this.identity=identity;this.orders=orders;this.payments=payments;this.paymentService=paymentService;}
    @GetMapping({"/orders","/orders/my","/account/orders"}) public List<Map<String,Object>> orders(HttpSession session){return (identity.admin(session)?orders.findAll():orders.findByUserId(identity.customer(session).getId())).stream().map(site::dto).toList();}
    @GetMapping("/orders/{id}") public Map<String,Object> order(@PathVariable Long id,HttpSession session){return site.dto(site.owned(id,session));}
    @PutMapping("/orders/{id}") public Map<String,Object> edit(@PathVariable Long id,@RequestBody Map<String,Object> data,HttpSession session){var order=site.owned(id,session);if(!identity.admin(session)&&order.getStatus()!=OrderStatus.PENDING&&order.getStatus()!=OrderStatus.CONFIRMED)throw error(HttpStatus.CONFLICT,"Preparation has already started.");order.setNote(text(data,"notes",order.getNote()));order.setCustomerName(text(data,"customerName",order.getCustomerName()));order.setCustomerPhone(text(data,"customerPhone",order.getCustomerPhone()));order.setDeliveryAddress(text(data,"deliveryAddress",order.getDeliveryAddress()));return site.dto(orders.save(order));}
    @PostMapping("/admin/updateOrderStatus") public Map<String,Object> status(@RequestBody Map<String,Object> data,HttpSession session){
        identity.requireAdmin(session);var order=site.owned(id(data.get("orderId").toString()),session);OrderStatus next;try{next=OrderStatus.valueOf(text(data,"status","").toUpperCase().replace(' ','_'));}catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"Invalid order status.");}
        if(next==OrderStatus.CANCELLED)site.cancel(order);else{if(!order.getStatus().canMoveTo(next))throw error(HttpStatus.CONFLICT,"That order status transition is not allowed.");order.setStatus(next);orders.save(order);}return map("success",true,"order",site.dto(order));
    }
    @GetMapping("/payments") public List<Map<String,Object>> payments(HttpSession session){identity.requireAdmin(session);return payments.findAll().stream().map(site::payment).toList();}
    @GetMapping("/payments/order/{id}") public Map<String,Object> payment(@PathVariable Long id,HttpSession session){site.owned(id,session);return payments.findByOrderId(id).stream().reduce((a,b)->b).map(site::payment).orElse(map());}
    @RequestMapping(value="/payments/{id}/status",method={RequestMethod.PUT,RequestMethod.POST}) public Map<String,Object> paymentStatus(@PathVariable Long id,@RequestParam String status,HttpSession session){identity.requireAdmin(session);PaymentStatus target;try{target=PaymentStatus.valueOf(status);}catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"Invalid payment status.");}return site.payment(paymentService.updateStatus(id,target));}
    @PostMapping("/payments/{id}/delete") public Map<String,Object> deletePayment(@PathVariable Long id,HttpSession session){identity.requireAdmin(session);paymentService.delete(id);return map("success",true);}
}
