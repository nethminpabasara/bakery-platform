package com.bakery.bakeryplatform.controller.website;

import com.bakery.bakeryplatform.service.order.SiteOrders;
import com.bakery.bakeryplatform.service.user.SiteIdentity;

import com.bakery.bakeryplatform.model.billing.Invoice;
import com.bakery.bakeryplatform.repository.billing.InvoiceRepository;
import com.bakery.bakeryplatform.service.billing.InvoiceService;
import com.bakery.bakeryplatform.repository.billing.RefundRepository;
import com.bakery.bakeryplatform.model.billing.RefundRequest;
import com.bakery.bakeryplatform.service.billing.RefundService;
import com.bakery.bakeryplatform.repository.payment.PaymentRepository;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.service.payment.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.*;

@RestController @RequestMapping("/api/site")
public class SiteBillingController {
    private final SiteIdentity identity;private final SiteOrders orders;private final OrderRepository orderRows;private final PaymentRepository payments;
    private final PaymentService paymentService;private final InvoiceRepository invoices;private final InvoiceService invoiceService;private final RefundRepository refunds;private final RefundService refundService;
    public SiteBillingController(SiteIdentity identity,SiteOrders orders,OrderRepository orderRows,PaymentRepository payments,PaymentService paymentService,InvoiceRepository invoices,InvoiceService invoiceService,RefundRepository refunds,RefundService refundService){this.identity=identity;this.orders=orders;this.orderRows=orderRows;this.payments=payments;this.paymentService=paymentService;this.invoices=invoices;this.invoiceService=invoiceService;this.refunds=refunds;this.refundService=refundService;}
    private Invoice ownedInvoice(Long id,HttpSession session){var invoice=invoices.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Invoice not found."));identity.owner(invoice.getUserId(),session);return invoice;}
    public Map<String,Object> invoice(Invoice i){return map("id",i.getId(),"invoiceNumber",i.getInvoiceNumber(),"orderId",i.getOrderId()==null?null:i.getOrderId().toString(),"customerName",i.getCustomerName(),"customerEmail",i.getBillingEmail()==null?i.getCustomerEmail():i.getBillingEmail(),"customerPhone",i.getCustomerPhone(),"billingAddress",i.getBillingAddress(),"itemsSummary",i.getItemsSummary(),"totalAmount",i.getAmount(),"subtotal",i.getSubtotal(),"paymentFee",i.getPaymentFee(),"invoiceDate",i.getInvoiceDate(),"status",i.getStatus()==null?"PAID":i.getStatus(),"notes",i.getNotes());}
    @GetMapping("/invoices") public List<Map<String,Object>> invoices(HttpSession session){return (identity.admin(session)?invoices.findAll():invoices.findByUserIdOrderByInvoiceDateDesc(identity.customer(session).getId())).stream().map(this::invoice).toList();}
    @GetMapping("/invoices/{id}") public Map<String,Object> invoice(@PathVariable Long id,HttpSession session){return invoice(ownedInvoice(id,session));}
    @GetMapping("/invoices/order/{id}") public Map<String,Object> orderInvoice(@PathVariable Long id,HttpSession session){orders.owned(id,session);return invoices.findAll().stream().filter(i->id.equals(i.getOrderId())).findFirst().map(this::invoice).orElseThrow(()->error(HttpStatus.NOT_FOUND,"A paid invoice is not available yet."));}
    @PostMapping("/invoices/generate/{id}") public Map<String,Object> generate(@PathVariable Long id,HttpSession session){var order=orders.owned(id,session);var p=payments.findByOrderId(id).stream().filter(pay->pay.getStatus()==PaymentStatus.PAID).findFirst().orElseThrow(()->error(HttpStatus.CONFLICT,"An invoice is issued after payment is received."));return invoice(invoiceService.generateInvoice(p,order.getUserId()));}
    @PutMapping("/invoices/{id}") public Map<String,Object> edit(@PathVariable Long id,@RequestBody Map<String,Object> input,HttpSession session){identity.requireAdmin(session);var i=ownedInvoice(id,session);i.setCustomerName(text(input,"customerName",i.getCustomerName()));i.setBillingEmail(text(input,"customerEmail",i.getCustomerEmail()));i.setCustomerPhone(text(input,"customerPhone",i.getCustomerPhone()));i.setBillingAddress(text(input,"billingAddress",i.getBillingAddress()));i.setNotes(text(input,"notes",i.getNotes()));return invoice(invoices.save(i));}
    @PutMapping("/invoices/{id}/void") public Map<String,Object> voidInvoice(@PathVariable Long id,HttpSession session){identity.requireAdmin(session);var i=ownedInvoice(id,session);i.setStatus("VOID");return invoice(invoices.save(i));}
    @DeleteMapping("/invoices/{id}") public Map<String,Object> delete(@PathVariable Long id,HttpSession session){identity.requireAdmin(session);var i=ownedInvoice(id,session);if(!"VOID".equals(i.getStatus()))throw error(HttpStatus.CONFLICT,"Void the invoice before deleting it.");invoices.delete(i);return map("success",true);}
    private RefundRequest ownedRefund(Long id,HttpSession session){var r=refunds.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Refund request not found."));orders.owned(r.getOrderId(),session);return r;}
    private Map<String,Object> refund(RefundRequest r){var order=orderRows.findById(r.getOrderId()).orElse(null);var payment=payments.findByOrderId(r.getOrderId()).stream().reduce((a,b)->b).orElse(null);return map("id",r.getId(),"requestId","REF-"+r.getId(),"orderId",r.getOrderId().toString(),"customerName",order==null?"Customer":order.getCustomerName(),"customerEmail",order==null?null:order.getCustomerEmail(),"amount",payment==null?0:payment.getTotalCharged(),"reason",r.getReason(),"status",r.getStatus(),"adminNotes",r.getAdminNotes(),"requestDate",r.getRequestDate());}
    @GetMapping("/refund-requests") public List<Map<String,Object>> refunds(HttpSession session){Long user=identity.admin(session)?null:identity.customer(session).getId();return refunds.findAll().stream().filter(r->user==null||orderRows.findById(r.getOrderId()).map(o->user.equals(o.getUserId())).orElse(false)).map(this::refund).toList();}
    @PostMapping("/refund-requests") public Map<String,Object> requestRefund(@RequestBody Map<String,Object> input,HttpSession session){Long orderId=id(text(input,"orderId",""));orders.owned(orderId,session);if(payments.findByOrderId(orderId).stream().noneMatch(p->p.getStatus()==PaymentStatus.PAID))throw error(HttpStatus.CONFLICT,"Only paid orders can be refunded.");if(refunds.findAll().stream().anyMatch(r->orderId.equals(r.getOrderId())&&"PENDING".equals(r.getStatus())))throw error(HttpStatus.CONFLICT,"A pending refund request already exists.");var r=new RefundRequest();r.setOrderId(orderId);r.setReason(reason(input));return refund(refundService.submitRefundRequest(r));}
    private String reason(Map<String,Object> input){String value=text(input,"reason","");if(value.isBlank()||value.length()>1000)throw error(HttpStatus.BAD_REQUEST,"Enter a reason of 1 to 1000 characters.");return value;}
    @PutMapping("/refund-requests/{id}") public Map<String,Object> editRefund(@PathVariable Long id,@RequestBody Map<String,Object> input,HttpSession session){var r=ownedRefund(id,session);if(!"PENDING".equals(r.getStatus()))throw error(HttpStatus.CONFLICT,"Only pending refund requests can be edited.");r.setReason(reason(input));return refund(refunds.save(r));}
    @PutMapping("/refund-requests/{id}/withdraw") public Map<String,Object> withdraw(@PathVariable Long id,HttpSession session){var r=ownedRefund(id,session);if(!"PENDING".equals(r.getStatus()))throw error(HttpStatus.CONFLICT,"Only pending requests can be withdrawn.");r.setStatus("WITHDRAWN");return refund(refunds.save(r));}
    @PutMapping("/refund-requests/{id}/{decision}") @Transactional public Map<String,Object> decision(@PathVariable Long id,@PathVariable String decision,@RequestBody(required=false) Map<String,Object> input,HttpSession session){identity.requireAdmin(session);if(!Set.of("approve","reject").contains(decision))throw error(HttpStatus.NOT_FOUND,"Action not found.");var r=ownedRefund(id,session);if(decision.equals("approve")){var p=payments.findByOrderId(r.getOrderId()).stream().filter(v->v.getStatus()==PaymentStatus.PAID).findFirst().orElseThrow(()->error(HttpStatus.CONFLICT,"The order no longer has a paid payment."));paymentService.updateStatus(p.getId(),PaymentStatus.REFUNDED);}r=refundService.updateRefundStatus(id,decision.equals("approve")?"APPROVED":"REJECTED");r.setAdminNotes(input==null?"":text(input,"adminNotes",""));return refund(refunds.save(r));}
    @DeleteMapping("/refund-requests/{id}") public Map<String,Object> deleteRefund(@PathVariable Long id,HttpSession session){identity.requireAdmin(session);refunds.delete(ownedRefund(id,session));return map("success",true);}
}
