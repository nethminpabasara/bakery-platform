package com.bakery.bakeryplatform;

import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.repository.payment.PaymentRepository;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.repository.user.AdminUserRepository;
import com.bakery.bakeryplatform.repository.billing.InvoiceRepository;
import com.bakery.bakeryplatform.repository.cake.CakeDesignRepository;
import com.bakery.bakeryplatform.repository.cake.CustomCakeBookingRepository;
import com.bakery.bakeryplatform.repository.catalogue.SiteStockRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:website-integration;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
 "spring.jpa.show-sql=false","app.items.file=target/website-test-data/items.json",
 "app.items.images=target/website-test-data/images","app.website.seed-catalogue=true",
 "app.admin.username=website-admin","app.admin.email=admin@example.invalid","app.admin.password=test-password",
 "app.invoice-email.enabled=false","app.invoice-email.dispatch-enabled=false"
})
@AutoConfigureMockMvc
class WebsiteIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired AdminUserRepository admins;
 @Autowired OrderRepository orders;@Autowired PaymentRepository payments;@Autowired InvoiceRepository invoices;
 @Autowired SiteStockRepository stocks;@Autowired CustomCakeBookingRepository bookings;@Autowired CakeDesignRepository designs;
 MockHttpSession customer;MockHttpSession admin;Long customerId;
 @BeforeEach void setup()throws Exception{
  customer=register(new MockHttpSession());customerId=(Long)customer.getAttribute("loggedInUserId");
  admin=new MockHttpSession();admin.setAttribute("loggedInAdminId",admins.findByUsername("website-admin").getId());
 }
 MockHttpSession register(MockHttpSession initial)throws Exception{
  var r=mvc.perform(post("/api/site/auth/register").session(initial).contentType("application/json")
   .content(mapper.writeValueAsString(Map.of("fullName","Website Customer","email","site-"+System.nanoTime()+"@example.invalid","phone","+94772345678","password","test-password"))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andReturn();
  return (MockHttpSession)r.getRequest().getSession(false);
 }
 Map<String,Object> json(MvcResult r)throws Exception{return mapper.readValue(r.getResponse().getContentAsString(),new tools.jackson.core.type.TypeReference<>(){});}
 Long checkout(String token)throws Exception{
  var r=mvc.perform(post("/website/order").session(customer).param("action","create")
    .param("checkoutToken",token).param("cartPayload","[{\"id\":\"PROD-001\",\"qty\":2,\"price\":1}]")
    .param("customerName","Website Customer").param("customerPhone","0772345678").param("deliveryAddress","Pickup at bakery")
    .param("paymentMethod","Card Payment").param("cardHolderName","Test Customer").param("cardNumber","4111 1111 1111 1111"))
   .andExpect(status().is3xxRedirection()).andReturn();
  return Long.valueOf(r.getResponse().getRedirectedUrl().split("paymentId=")[1]);
 }
 @Test void catalogueAndGuestCartUseBackendPricesAndSurviveLogin()throws Exception{
  mvc.perform(get("/api/site/bakery-products")).andExpect(status().isOk()).andExpect(jsonPath("$[0].productCode").value("PROD-001"));
  MockHttpSession guest=new MockHttpSession();
  mvc.perform(post("/api/site/cart/add").session(guest).contentType("application/json").content("{\"productId\":\"PROD-001\",\"quantity\":1,\"unitPrice\":1}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.item.unitPrice").value(750));
  MockHttpSession signedIn=register(guest);
  mvc.perform(get("/api/site/cart").session(signedIn)).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.total").value(750));
 }
 @Test void checkoutRepricesLocksStockAndIsIdempotent()throws Exception{
  int before=stocks.findById("PROD-001").orElseThrow().getQuantity();String token=UUID.randomUUID().toString();
  Long id=checkout(token);assertEquals(id,checkout(token));
  var payment=payments.findById(id).orElseThrow();assertEquals(PaymentStatus.PAID,payment.getStatus());
  assertEquals(0,payment.getAmount().compareTo(new java.math.BigDecimal("1500.00")));
  assertEquals(1,orders.findByUserId(customerId).size());assertTrue(invoices.findByPaymentId(id).isPresent());
  assertEquals(before-2,stocks.findById("PROD-001").orElseThrow().getQuantity());
  mvc.perform(get("/api/site/account/orders").session(customer)).andExpect(status().isOk()).andExpect(jsonPath("$[0].paymentStatus").value("PAID"));
 }
 @Test void categoriesAndProductsSupportAdminCrud()throws Exception{
  String category="Test "+System.nanoTime();
  var created=mvc.perform(post("/api/site/categories").session(admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("name",category,"description","Test","section","DAILY_BAKERY"))))
   .andExpect(status().isOk()).andReturn();Long categoryId=((Number)json(created).get("id")).longValue();
  var p=mvc.perform(post("/api/site/products").session(admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("productType","BAKERY","name","Test bun","category",category,"price",100,"stockQuantity",5,"imageUrl","/images/placeholder.svg"))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andReturn();
  String productId=((Map<?,?>)json(p).get("product")).get("productId").toString();
  mvc.perform(post("/api/site/products/update").session(admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("productId",productId,"productType","BAKERY","name","Updated bun","category",category,"price",120,"stockQuantity",7))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.product.stockQuantity").value(7));
  mvc.perform(post("/api/site/products/update").session(admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("productId",productId,"productType","BAKERY","name","Invalid update","category",category,"price",1,"stockQuantity",-1))))
   .andExpect(status().isBadRequest());
  mvc.perform(get("/api/site/products/{id}",productId)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated bun")).andExpect(jsonPath("$.price").value(120));
  mvc.perform(post("/api/site/products/delete").session(admin).param("productId",productId)).andExpect(status().isOk());
  mvc.perform(delete("/api/site/categories/{id}",categoryId).session(admin)).andExpect(status().isOk());
  mvc.perform(post("/api/site/products").session(customer).contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
 }
 @Test void customerBookingEditsAndCancellationArePersistentAndOwned()throws Exception{
  String date=LocalDate.now(java.time.ZoneId.of("Asia/Colombo")).plusDays(8).toString();
  mvc.perform(post("/website/cake-booking").session(customer).param("action","create").param("customerName","Cake Customer")
   .param("contactNumber","0772345678").param("requiredDate",date).param("shape","Round").param("flavor","Vanilla")
   .param("decoration","Strawberry").param("cakeSize","1.5 kg").param("estimatedPrice","1")).andExpect(status().is3xxRedirection());
  var b=bookings.findAll().stream().filter(x->customerId.equals(x.getUserId())).findFirst().orElseThrow();assertTrue(b.getEstimatedPrice()>1);
  MockHttpSession other=register(new MockHttpSession());
  mvc.perform(get("/api/site/bookings/my").session(other)).andExpect(status().isOk()).andExpect(content().json("[]"));
  mvc.perform(put("/api/site/bookings/{id}",b.getBookingId()).session(other).contentType("application/json").content("{\"notes\":\"stolen\"}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/site/bookings/{id}",b.getBookingId()).session(customer).contentType("application/json").content("{\"notes\":\"Less sugar\"}")).andExpect(status().isOk());
  assertEquals("Less sugar",bookings.findById(b.getBookingId()).orElseThrow().getNotes());
  mvc.perform(delete("/api/site/bookings/{id}",b.getBookingId()).session(customer)).andExpect(status().isOk());
 }
 @Test void savedDesignsAndCakeOptionsUseTheReviewedEntities()throws Exception{
  mvc.perform(post("/api/site/cake-designs").session(customer).contentType("application/json").content("{\"shape\":\"Round\",\"flavor\":\"Vanilla\",\"message\":\"Happy Birthday\",\"estimatedPrice\":1}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
  var design=designs.findAll().stream().filter(d->customerId.equals(d.getUserId())).findFirst().orElseThrow();
  mvc.perform(put("/api/site/cake-designs/{id}",design.getId()).session(customer).contentType("application/json").content("{\"message\":\"Updated message\"}"))
   .andExpect(status().isOk());assertEquals("Updated message",designs.findById(design.getId()).orElseThrow().getCakeMessage());
  mvc.perform(delete("/api/site/cake-designs/{id}",design.getId()).session(customer)).andExpect(status().isOk());
  var option=mvc.perform(post("/api/site/cake-options").session(admin).contentType("application/json").content("{\"optionType\":\"FLAVOUR\",\"name\":\"Coffee\",\"extraPrice\":250}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.option.extraPrice").value(250)).andReturn();
  Long id=((Number)((Map<?,?>)json(option).get("option")).get("id")).longValue();
  mvc.perform(put("/api/site/cake-options/{id}",id).session(admin).contentType("application/json").content("{\"name\":\"Coffee Deluxe\",\"extraPrice\":300}")).andExpect(status().isOk());
  mvc.perform(delete("/api/site/cake-options/{id}",id).session(admin)).andExpect(status().isOk());
 }
 @Test void refundsAndInvoiceAdministrationUpdatePersistedRecords()throws Exception{
  Long paymentId=checkout(UUID.randomUUID().toString());var payment=payments.findById(paymentId).orElseThrow();
  var r=mvc.perform(post("/api/site/refund-requests").session(customer).contentType("application/json").content("{\"orderId\":\""+payment.getOrderId()+"\",\"amount\":999999,\"reason\":\"Changed plans\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(1530)).andReturn();
  Long refundId=((Number)json(r).get("id")).longValue();
  mvc.perform(put("/api/site/refund-requests/{id}/approve",refundId).session(admin).contentType("application/json").content("{\"adminNotes\":\"Approved\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
  assertEquals(PaymentStatus.REFUNDED,payments.findById(paymentId).orElseThrow().getStatus());
  Long invoiceId=invoices.findByPaymentId(paymentId).orElseThrow().getId();
  mvc.perform(put("/api/site/invoices/{id}",invoiceId).session(admin).contentType("application/json").content("{\"customerName\":\"Corrected name\",\"billingAddress\":\"Kurunegala\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.customerName").value("Corrected name"));
  mvc.perform(put("/api/site/invoices/{id}/void",invoiceId).session(admin)).andExpect(status().isOk());
  mvc.perform(delete("/api/site/invoices/{id}",invoiceId).session(admin)).andExpect(status().isOk());
 }
 @Test void adminUserCrudAndPageRequireRealSession()throws Exception{
  mvc.perform(get("/admin.html")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/api/site/users").session(customer)).andExpect(status().isUnauthorized());
  var c=mvc.perform(post("/api/site/users/create").session(admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("fullName","Managed Customer","email","managed-"+System.nanoTime()+"@example.invalid","phone","0772345678","password","test-password","role","CUSTOMER"))))
   .andExpect(status().isOk()).andReturn();
  var user=(Map<?,?>)json(c).get("user");String id=user.get("userId").toString();String email=user.get("email").toString();
  mvc.perform(post("/api/site/users/update").session(admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("userId",id,"fullName","Updated Customer","email",email,"phone","0779999999","role","CUSTOMER")))).andExpect(status().isOk());
  mvc.perform(post("/api/site/users/delete").session(admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("userId",id)))).andExpect(status().isOk());
 }
}
