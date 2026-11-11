package com.bakery.bakeryplatform;

import com.bakery.bakeryplatform.dto.payment.PaymentRequest;
import com.bakery.bakeryplatform.model.order.Order;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.repository.payment.PaymentRepository;
import com.bakery.bakeryplatform.service.payment.PaymentService;
import com.bakery.bakeryplatform.model.user.AdminUser;
import com.bakery.bakeryplatform.model.user.Customer;
import com.bakery.bakeryplatform.repository.user.AdminUserRepository;
import com.bakery.bakeryplatform.repository.user.UserRepository;
import com.bakery.bakeryplatform.model.billing.Invoice;
import com.bakery.bakeryplatform.service.billing.InvoiceDocumentService;
import com.bakery.bakeryplatform.service.billing.InvoiceEmailService;
import com.bakery.bakeryplatform.repository.billing.InvoiceRepository;
import com.bakery.bakeryplatform.model.billing.Invoice.EmailStatus;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.website.seed-catalogue=false",
        "spring.datasource.url=jdbc:h2:mem:checkout-tests;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.show-sql=false",
        "app.items.file=target/checkout-test-data/items.json",
        "app.items.images=target/checkout-test-data/images",
        "app.invoice-email.enabled=true",
        "app.invoice-email.dispatch-enabled=false",
        "app.invoice-email.from=bakery@example.invalid",
        "spring.mail.host=smtp.example.invalid"
})
@AutoConfigureMockMvc
@Import(CheckoutInvoiceTests.MailConfiguration.class)
class CheckoutInvoiceTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AdminUserRepository admins;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired InvoiceRepository invoices;
    @Autowired InvoiceEmailService emails;
    @Autowired InvoiceDocumentService documents;
    @Autowired PaymentService paymentService;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired RecordingMailSender sender;
    private Customer customer;
    private MockHttpSession session;

    @BeforeEach void setUp() {
        customer = customer("Owner");
        session = customerSession(customer.getId());
        sender.messages.clear();
        sender.failSend = false;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MailConfiguration {
        @Bean RecordingMailSender recordingMailSender() { return new RecordingMailSender(); }
    }

    static class RecordingMailSender extends JavaMailSenderImpl {
        final List<MimeMessage> messages = new CopyOnWriteArrayList<>();
        boolean failSend;
        @Override public void send(MimeMessage message) {
            if (failSend) throw new MailSendException("Test SMTP failure");
            messages.add(message);
        }
    }

    private Customer customer(String name) {
        Customer value = new Customer();
        value.setUsername(name);
        value.setEmail("customer-" + System.nanoTime() + "@example.invalid");
        value.setPassword("test-only-password");
        return users.save(value);
    }

    private MockHttpSession customerSession(Long id) {
        var result = new MockHttpSession();
        result.setAttribute("loggedInUserId", id);
        return result;
    }

    private Order order() {
        return orders.save(new Order(customer.getId(), "Thank you", "2 x Chocolate Cake", new BigDecimal("2500.00")));
    }

    private String cardBody(Order order) {
        return """
                {"orderId":%d,"paymentType":"CARD","cardHolderName":"Test Customer","cardNumber":"4111111111111111"}
                """.formatted(order.getId());
    }

    private Invoice pay(Order order) throws Exception {
        mvc.perform(post("/api/payments").session(session).contentType("application/json").content(cardBody(order)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PAID"));
        return invoices.findByPaymentId(payments.findByOrderId(order.getId()).getFirst().getId()).orElseThrow();
    }

    @Test void paymentRequiresCustomerLogin() throws Exception {
        mvc.perform(post("/api/payments").contentType("application/json").content(cardBody(order())))
                .andExpect(status().isUnauthorized());
        assertTrue(sender.messages.isEmpty());
    }

    @Test void customerCannotPayForAnotherCustomersOrder() throws Exception {
        Order order = order();
        mvc.perform(post("/api/payments").session(customerSession(customer("Other").getId()))
                        .contentType("application/json").content(cardBody(order)))
                .andExpect(status().isForbidden());
        assertTrue(payments.findByOrderId(order.getId()).isEmpty());
    }

    @Test void confirmationAndPdfUseStoredTotalsAndRequireOwnership() throws Exception {
        Invoice invoice = pay(order());
        mvc.perform(get("/api/payments/{id}/confirmation", invoice.getPaymentId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(invoice.getId()))
                .andExpect(jsonPath("$.totalCharged").value(2550))
                .andExpect(jsonPath("$.emailStatus").value("PENDING"))
                .andExpect(jsonPath("$.customerEmail").value(customer.getEmail()));
        var other = customerSession(customer("Other").getId());
        mvc.perform(get("/api/payments/{id}/confirmation", invoice.getPaymentId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/payments/{id}/confirmation", invoice.getPaymentId()).session(other)).andExpect(status().isForbidden());
        mvc.perform(get("/api/invoices/{id}/pdf", invoice.getId()).session(other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/invoices/{id}/email", invoice.getId()).session(other)).andExpect(status().isNotFound());
        byte[] pdf = mvc.perform(get("/api/invoices/{id}/pdf", invoice.getId()).session(session))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains(invoice.getInvoiceNumber()));
            assertTrue(text.contains("2550.00"));
            assertFalse(text.contains("4111111111111111"));
        }
        Files.createDirectories(Path.of("target/review-preview"));
        Files.write(Path.of("target/review-preview/sample-invoice.pdf"), pdf);
    }

    @Test void emailContainsPdfAndUsesRegisteredCustomerAddress() throws Exception {
        Invoice invoice = pay(order());
        emails.deliver(invoice.getId());
        assertEquals(1, sender.messages.size());
        MimeMessage message = sender.messages.getFirst();
        assertEquals(customer.getEmail(), message.getAllRecipients()[0].toString());
        Part attachment = attachment(message);
        assertNotNull(attachment);
        assertEquals(invoice.getInvoiceNumber() + ".pdf", attachment.getFileName());
        try (var document = Loader.loadPDF(attachment.getInputStream().readAllBytes())) {
            assertTrue(new PDFTextStripper().getText(document).contains("2550.00"));
        }
        assertEquals(EmailStatus.SENT, invoices.findById(invoice.getId()).orElseThrow().getEmailStatus());
        assertNotNull(invoices.findById(invoice.getId()).orElseThrow().getEmailSentAt());
        emails.deliver(invoice.getId());
        mvc.perform(post("/api/invoices/{id}/email", invoice.getId()).session(session)).andExpect(status().isAccepted());
        emails.deliver(invoice.getId());
        assertEquals(1, sender.messages.size());
    }

    private Part attachment(Part part) throws Exception {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) return part;
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                Part found = attachment(multipart.getBodyPart(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test void failedEmailKeepsPaymentAndCanBeRetried() throws Exception {
        Invoice invoice = pay(order());
        sender.failSend = true;
        emails.deliver(invoice.getId());
        assertEquals(PaymentStatus.PAID, payments.findById(invoice.getPaymentId()).orElseThrow().getStatus());
        assertEquals(EmailStatus.FAILED, invoices.findById(invoice.getId()).orElseThrow().getEmailStatus());
        mvc.perform(get("/api/invoices/{id}/pdf", invoice.getId()).session(session)).andExpect(status().isOk());
        sender.failSend = false;
        mvc.perform(post("/api/invoices/{id}/email", invoice.getId()).session(session))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PENDING"));
        emails.deliver(invoice.getId());
        assertEquals(EmailStatus.SENT, invoices.findById(invoice.getId()).orElseThrow().getEmailStatus());
    }

    @Test void cashInvoiceAndEmailWaitUntilAdminConfirmsCollection() throws Exception {
        Order order = order();
        mvc.perform(post("/api/payments").session(session).contentType("application/json")
                        .content("{\"orderId\":" + order.getId() + ",\"paymentType\":\"CASH_ON_DELIVERY\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
        Long paymentId = payments.findByOrderId(order.getId()).getFirst().getId();
        assertTrue(invoices.findByPaymentId(paymentId).isEmpty());
        mvc.perform(get("/api/invoices").session(session)).andExpect(status().isOk());
        assertTrue(invoices.findByPaymentId(paymentId).isEmpty());
        mvc.perform(put("/api/payments/{id}/status", paymentId).session(session)
                        .contentType("application/json").content("{\"status\":\"PAID\"}"))
                .andExpect(status().isUnauthorized());
        var admin = admins.save(new AdminUser("checkout-admin-" + System.nanoTime(),
                "admin-" + System.nanoTime() + "@example.invalid", "test-only-password"));
        var adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInAdminId", admin.getId());
        mvc.perform(put("/api/payments/{id}/status", paymentId).session(adminSession)
                        .contentType("application/json").content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        Invoice invoice = invoices.findByPaymentId(paymentId).orElseThrow();
        assertEquals(new BigDecimal("2600.00"), invoice.getAmount());
        emails.deliver(invoice.getId());
        assertEquals(1, sender.messages.size());
    }

    @Test void concurrentSubmissionsRecordOnlyOnePayment() throws Exception {
        Order order = order();
        var gate = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> submit = () -> {
                assertTrue(gate.await(5, TimeUnit.SECONDS));
                return mvc.perform(post("/api/payments").session(customerSession(customer.getId()))
                        .contentType("application/json").content(cardBody(order)))
                        .andReturn().getResponse().getStatus();
            };
            var first = pool.submit(submit);
            var second = pool.submit(submit);
            gate.countDown();
            assertEquals(java.util.Set.of(201, 400), java.util.Set.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)));
        }
        assertEquals(1, payments.findByOrderId(order.getId()).size());
    }

    @Test void rollbackLeavesNoPaymentInvoiceOrEmailQueue() {
        Order order = order();
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).execute(status -> {
            paymentService.create(new PaymentRequest(order.getId(), "CARD", "Test Customer", "4111111111111111"), customer.getId());
            throw new IllegalStateException("Force rollback");
        }));
        assertTrue(payments.findByOrderId(order.getId()).isEmpty());
        assertTrue(invoices.findByUserIdOrderByInvoiceDateDesc(customer.getId()).isEmpty());
        assertTrue(sender.messages.isEmpty());
    }

    @Test void shortCardNumberIsRejectedWithoutCreatingPayment() throws Exception {
        Order order = order();
        mvc.perform(post("/api/payments").session(session).contentType("application/json")
                        .content(cardBody(order).replace("4111111111111111", "12")))
                .andExpect(status().isBadRequest());
        assertTrue(payments.findByOrderId(order.getId()).isEmpty());
    }

    @Test void invoiceEscapesHtmlAndWrapsLongPdfText() throws Exception {
        Invoice invoice = pay(order());
        invoice.setCustomerName("<script>alert('test')</script>");
        invoice.setItemsSummary("Chocolate <img src=x> " + "W".repeat(900) + "\nCake");
        assertFalse(documents.emailHtml(invoice).contains("<script>"));
        assertTrue(documents.emailHtml(invoice).contains("&lt;script&gt;"));
        try (var document = Loader.loadPDF(documents.pdf(invoice))) {
            assertTrue(new PDFTextStripper().getText(document).contains("Chocolate"));
        }
    }
}
