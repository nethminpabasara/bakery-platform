package com.bakery.bakeryplatform.service.billing;

import com.bakery.bakeryplatform.model.billing.Invoice;
import com.bakery.bakeryplatform.repository.billing.InvoiceRepository;

import com.bakery.bakeryplatform.exception.BusinessRuleException;
import com.bakery.bakeryplatform.exception.ResourceNotFoundException;
import com.bakery.bakeryplatform.model.payment.Payment;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.repository.payment.PaymentRepository;
import com.bakery.bakeryplatform.model.billing.Invoice.EmailStatus;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@EnableScheduling
public class InvoiceEmailService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceEmailService.class);
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final InvoiceDocumentService documents;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final TransactionTemplate deliveryTransaction;
    private final boolean enabled;
    private final boolean dispatchEnabled;
    private final String from;
    private final String host;

    public InvoiceEmailService(InvoiceRepository invoices, PaymentRepository payments,
                               InvoiceDocumentService documents, ObjectProvider<JavaMailSender> mailSender,
                               PlatformTransactionManager transactionManager,
                               @Value("${app.invoice-email.enabled:false}") boolean enabled,
                               @Value("${app.invoice-email.dispatch-enabled:true}") boolean dispatchEnabled,
                               @Value("${app.invoice-email.from:}") String from,
                               @Value("${spring.mail.host:}") String host) {
        this.invoices = invoices;
        this.payments = payments;
        this.documents = documents;
        this.mailSender = mailSender;
        this.deliveryTransaction = new TransactionTemplate(transactionManager);
        this.deliveryTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.enabled = enabled;
        this.dispatchEnabled = dispatchEnabled;
        this.from = from;
        this.host = host;
    }

    private boolean isConfigured() {
        return enabled && !from.isBlank() && !host.isBlank() && mailSender.getIfAvailable() != null;
    }

    // This status is committed in the same transaction as the payment and invoice.
    @Transactional
    public EmailStatus queue(Long invoiceId) {
        Invoice invoice = invoices.findByIdForEmail(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
        requirePaidPayment(invoice);
        if (invoice.getEmailStatus() == EmailStatus.SENT || invoice.getEmailStatus() == EmailStatus.PENDING) {
            return invoice.getEmailStatus();
        }
        EmailStatus status = EmailStatus.NOT_CONFIGURED;
        if (isConfigured()) {
            String recipient = invoice.getCustomerEmail();
            status = recipient == null || recipient.isBlank() ? EmailStatus.FAILED : EmailStatus.PENDING;
        }
        invoice.setEmailStatus(status);
        invoices.save(invoice);
        return status;
    }

    // Pending messages are stored in H2 so delivery can resume after a restart.
    @Scheduled(initialDelayString = "${app.invoice-email.poll-delay-ms:5000}",
            fixedDelayString = "${app.invoice-email.poll-delay-ms:5000}")
    public void deliverPending() {
        if (!dispatchEnabled || !isConfigured()) {
            return;
        }
        for (Invoice invoice : invoices.findTop20ByEmailStatusOrderByIdAsc(EmailStatus.PENDING)) {
            try {
                deliver(invoice.getId());
            } catch (RuntimeException failure) {
                log.warn("Invoice {} queue processing failed ({})", invoice.getId(),
                        failure.getClass().getSimpleName());
            }
        }
    }

    // Each delivery reads committed data and locks its invoice against other workers.
    public void deliver(Long invoiceId) {
        deliveryTransaction.executeWithoutResult(transaction -> deliverInvoice(invoiceId));
    }

    private void deliverInvoice(Long invoiceId) {
        Invoice invoice = invoices.findByIdForEmail(invoiceId).orElse(null);
        if (invoice == null || invoice.getEmailStatus() != EmailStatus.PENDING) {
            return;
        }
        if (!isConfigured()) {
            invoice.setEmailStatus(EmailStatus.NOT_CONFIGURED);
            return;
        }
        try {
            requirePaidPayment(invoice);
            JavaMailSender sender = mailSender.getObject();
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            var recipient = new InternetAddress(invoice.getCustomerEmail(), true);
            recipient.validate();
            helper.setFrom(new InternetAddress(from, true));
            // The recipient comes from the customer's saved invoice snapshot, never a request field.
            helper.setTo(recipient);
            helper.setSubject("Your bakery invoice " + invoice.getInvoiceNumber());
            helper.setText(documents.plainText(invoice), documents.emailHtml(invoice));
            helper.addAttachment(invoice.getInvoiceNumber() + ".pdf",
                    new ByteArrayResource(documents.pdf(invoice)), "application/pdf");
            sender.send(message);
            invoice.setEmailStatus(EmailStatus.SENT);
            invoice.setEmailSentAt(LocalDateTime.now());
        } catch (Exception failure) {
            // Email failure must never undo a paid order or expose SMTP credentials.
            invoice.setEmailStatus(EmailStatus.FAILED);
            log.warn("Invoice {} email delivery failed ({})", invoiceId, failure.getClass().getSimpleName());
        }
        invoices.save(invoice);
    }

    private void requirePaidPayment(Invoice invoice) {
        Long paymentId = invoice.getPaymentId();
        Payment payment = paymentId == null ? null : payments.findById(paymentId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.PAID) {
            throw new BusinessRuleException("Invoice email is available only after payment is confirmed.");
        }
    }
}
