package com.bakery.bakeryplatform.service.billing;

import com.bakery.bakeryplatform.model.billing.Invoice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class InvoiceDocumentService {
    public String plainText(Invoice invoice) {
        return "Thank you for your order.\n\nInvoice: " + invoice.getInvoiceNumber()
                + "\nOrder: #" + invoice.getOrderId() + "\nDate: " + invoice.getInvoiceDate()
                + "\nItems: " + text(invoice.getItemsSummary())
                + "\nSubtotal: " + money(invoice.getSubtotal())
                + "\nPayment fee: " + money(invoice.getPaymentFee())
                + "\nTotal paid: " + money(invoice.getAmount())
                + "\nPayment method: " + text(invoice.getPaymentType()).replace('_', ' ')
                + "\n\nYour PDF invoice is attached.\n" + demoNote(invoice) + "\nSweet Bloom bakery";
    }

    public String emailHtml(Invoice invoice) {
        return """
                <!doctype html><html lang="en"><body style="margin:0;background:#faf7f1;color:#382b24;font-family:Arial,sans-serif">
                <div style="max-width:580px;margin:32px auto;padding:36px;background:white;border:1px solid #e8dfd3">
                <p style="font-size:13px;letter-spacing:2px">SWEET BLOOM / BAKERY</p>
                <h1 style="font-family:Georgia,serif;font-weight:normal;font-size:36px">Thank you for your order.</h1>
                <p>Hello %s, your payment has been recorded. Your PDF invoice is attached.</p>
                <p><strong>%s</strong><br>Order #%s &middot; %s</p>
                <p style="padding:20px;background:#faf7f1;white-space:pre-wrap">%s</p>
                <table style="width:100%%;line-height:2"><tr><td>Subtotal</td><td align="right">%s</td></tr>
                <tr><td>Payment fee</td><td align="right">%s</td></tr>
                <tr><td><strong>Total paid</strong></td><td align="right"><strong>%s</strong></td></tr></table>
                <p>Payment method: %s</p><p style="font-size:12px;color:#73675c">%s</p>
                <p style="margin-top:28px">A little sweetness, made for you.<br>Sweet Bloom bakery</p>
                </div></body></html>
                """.formatted(escape(invoice.getCustomerName()), escape(invoice.getInvoiceNumber()),
                invoice.getOrderId(), invoice.getInvoiceDate(), escape(invoice.getItemsSummary()),
                money(invoice.getSubtotal()), money(invoice.getPaymentFee()), money(invoice.getAmount()),
                escape(text(invoice.getPaymentType()).replace('_', ' ')), escape(demoNote(invoice)));
    }

    public byte[] pdf(Invoice invoice) throws IOException {
        try (var document = new PDDocument(); var bytes = new ByteArrayOutputStream()) {
            try (var page = new InvoicePage(document)) {
                page.line("Sweet Bloom", 22, true);
                page.line("ARTISAN BAKERY / PAYMENT RECEIPT", 10, false);
                page.gap(24);
                page.line("Thank you for your order.", 25, true);
                page.gap(18);
                page.line("Invoice " + invoice.getInvoiceNumber(), 12, true);
                page.line("Date: " + invoice.getInvoiceDate() + "    Order: #" + invoice.getOrderId(), 10, false);
                page.gap(16);
                page.line("BILLED TO", 10, true);
                page.line(text(invoice.getCustomerName()), 11, false);
                page.line(text(invoice.getCustomerEmail()), 10, false);
                page.gap(20);
                page.line("YOUR ORDER", 10, true);
                page.line(text(invoice.getItemsSummary()), 11, false);
                page.gap(24);
                page.row("Subtotal", money(invoice.getSubtotal()), false);
                page.row("Payment fee", money(invoice.getPaymentFee()), false);
                page.row("TOTAL PAID", money(invoice.getAmount()), true);
                page.gap(18);
                page.line("Payment method: " + text(invoice.getPaymentType()).replace('_', ' '), 10, false);
                page.gap(24);
                page.line(demoNote(invoice), 9, false);
                page.line("Please keep this invoice for your records.", 9, false);
                page.line("A little sweetness, made for you.", 10, false);
            }
            document.save(bytes);
            return bytes.toByteArray();
        }
    }

    private static String demoNote(Invoice invoice) {
        return "CARD".equals(invoice.getPaymentType())
                ? "University project demo. No real card charge was made." : "Cash payment received.";
    }

    private static String text(String value) { return value == null ? "" : value; }
    private static String escape(String value) { return HtmlUtils.htmlEscape(text(value)); }
    private static String money(BigDecimal value) {
        return "LKR " + (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static final class InvoicePage implements AutoCloseable {
        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private PDPageContentStream stream;
        private float y;

        InvoicePage(PDDocument document) throws IOException {
            this.document = document;
            regular = loadFont(document, "DejaVuSans.ttf");
            bold = loadFont(document, "DejaVuSans-Bold.ttf");
            newPage();
        }

        private static PDFont loadFont(PDDocument document, String filename) throws IOException {
            try (var input = InvoiceDocumentService.class.getResourceAsStream("/invoice-fonts/" + filename)) {
                if (input == null) throw new IOException("The bundled invoice font is missing.");
                return PDType0Font.load(document, input);
            }
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            var page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            stream.setNonStrokingColor(0.22f, 0.17f, 0.14f);
            y = 785;
        }

        void gap(float height) { y -= height; }

        void line(String value, float size, boolean strong) throws IOException {
            PDFont font = strong ? bold : regular;
            String safe = printable(value, font);
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < safe.length(); i++) {
                char next = safe.charAt(i);
                if (next == '\n' || font.getStringWidth(current.toString() + next) / 1000 * size > 487) {
                    write(current.toString(), 54, size, font);
                    y -= size + 8;
                    current.setLength(0);
                    if (next == '\n') continue;
                }
                current.append(next);
            }
            if (!current.isEmpty()) {
                write(current.toString(), 54, size, font);
                y -= size + 8;
            }
        }

        void row(String label, String amount, boolean strong) throws IOException {
            PDFont font = strong ? bold : regular;
            float size = strong ? 13 : 11;
            write(label, 54, size, font);
            write(amount, 541 - font.getStringWidth(amount) / 1000 * size, size, font);
            y -= 27;
        }

        private void write(String text, float x, float size, PDFont font) throws IOException {
            if (y < 60) newPage();
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
        }

        // Full Unicode is retained in the HTML email; any characters missing from
        // the bundled PDF font are replaced rather than failing the invoice download.
        private static String printable(String value, PDFont font) throws IOException {
            StringBuilder result = new StringBuilder();
            for (int code : text(value).codePoints().toArray()) {
                if (code == '\n') { result.append('\n'); continue; }
                if (Character.isISOControl(code)) { result.append(' '); continue; }
                String character = new String(Character.toChars(code));
                try { font.getStringWidth(character); result.append(character); }
                catch (IllegalArgumentException unsupported) { result.append('?'); }
            }
            return result.toString();
        }

        @Override public void close() throws IOException { if (stream != null) stream.close(); }
    }
}
