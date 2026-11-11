package com.bakery.bakeryplatform.controller.billing;

import com.bakery.bakeryplatform.model.billing.Invoice;
import com.bakery.bakeryplatform.service.billing.InvoiceDocumentService;
import com.bakery.bakeryplatform.service.billing.InvoiceEmailService;
import com.bakery.bakeryplatform.service.billing.InvoiceService;

import com.bakery.bakeryplatform.exception.ResourceNotFoundException;
import com.bakery.bakeryplatform.model.payment.Payment;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.service.payment.PaymentService;
import com.bakery.bakeryplatform.model.user.User;
import com.bakery.bakeryplatform.repository.user.UserRepository;
import com.bakery.bakeryplatform.model.billing.Invoice.EmailStatus;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final InvoiceDocumentService documents;
    private final InvoiceEmailService emails;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public InvoiceController(InvoiceService invoiceService, PaymentService paymentService,
                             InvoiceDocumentService documents, InvoiceEmailService emails,
                             OrderRepository orderRepository, UserRepository userRepository) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.documents = documents;
        this.emails = emails;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/invoices")
    public List<Invoice> viewMyInvoices(HttpSession session) {
        Long userId = requireCustomerId(session);
        paymentService.getByUser(userId).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID
                        || payment.getStatus() == PaymentStatus.REFUNDED)
                .forEach(payment -> invoiceService.generateInvoice(payment, userId));
        return invoiceService.getInvoicesByUserId(userId);
    }

    @GetMapping("/invoices/{id}")
    public Invoice viewInvoice(@PathVariable Long id, HttpSession session) {
        Long userId = requireCustomerId(session);
        Invoice invoice = invoiceService.getInvoiceByIdAndUserId(id, userId);
        if (invoice == null) {
            throw new ResourceNotFoundException("Invoice not found with id " + id);
        }
        return invoice;
    }

    @GetMapping(value = "/invoices/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, HttpSession session) throws IOException {
        Invoice invoice = viewInvoice(id, session);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(invoice.getInvoiceNumber() + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF).body(documents.pdf(invoice));
    }

    @PostMapping("/invoices/{id}/email")
    public ResponseEntity<Map<String, EmailStatus>> retryEmail(@PathVariable Long id, HttpSession session) {
        Invoice invoice = viewInvoice(id, session);
        return ResponseEntity.accepted().body(Map.of("status", emails.queue(invoice.getId())));
    }

    @GetMapping("/admin/invoices")
    public List<AdminInvoice> getAllInvoices() {
        List<Payment> payments = paymentService.getAll(null);
        Map<Long, Payment> paymentsById = payments.stream()
                .collect(Collectors.toMap(Payment::getId, payment -> payment));

        payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.PAID
                        || payment.getStatus() == PaymentStatus.REFUNDED)
                .forEach(payment -> orderRepository.findById(payment.getOrderId())
                .ifPresent(order -> invoiceService.generateInvoice(payment, order.getUserId())));

        List<Invoice> invoices = invoiceService.getAllInvoices();
        Set<Long> userIds = invoices.stream()
                .map(Invoice::getUserId)
                .filter(userId -> userId != null)
                .collect(Collectors.toSet());
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return invoices.stream()
                .map(invoice -> toAdminInvoice(invoice, paymentsById, usersById))
                .toList();
    }

    private static AdminInvoice toAdminInvoice(
            Invoice invoice,
            Map<Long, Payment> paymentsById,
            Map<Long, User> usersById) {
        User user = usersById.get(invoice.getUserId());
        Payment payment = paymentsById.get(invoice.getPaymentId());
        return new AdminInvoice(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getPaymentId(),
                invoice.getOrderId(),
                invoice.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getEmail(),
                invoice.getPaymentType(),
                invoice.getAmount(),
                payment == null ? null : payment.getStatus());
    }

    public record AdminInvoice(
            Long id,
            String invoiceNumber,
            LocalDate invoiceDate,
            Long paymentId,
            Long orderId,
            Long userId,
            String customerName,
            String customerEmail,
            String paymentType,
            BigDecimal amount,
            PaymentStatus paymentStatus) {
    }

    private static Long requireCustomerId(HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer login required.");
        }
        return userId;
    }
}
