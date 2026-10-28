package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.billing.dto.B2b2cInquiryRequest;
import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import com.histar.be.billing.repository.HeritageDigitizationInquiryRepository;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.config.TestHookProperties;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.mail.SmtpEmailSender;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.util.HtmlUtils;

class B2b2cInquirySmtpDeliveryTest {
    @Test
    @Timeout(20)
    void submittingLeadDeliversCustomerAndSalesMailToLocalSmtp() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var listener = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(5000);
            var captured = executor.submit(() -> {
                var messages = new ArrayList<byte[]>();
                for (int i = 0; i < 2; i++) messages.add(receive(listener));
                return messages;
            });

            var transport = new JavaMailSenderImpl();
            transport.setHost("127.0.0.1");
            transport.setPort(listener.getLocalPort());
            transport.getJavaMailProperties().setProperty("mail.smtp.auth", "false");
            transport.getJavaMailProperties().setProperty("mail.smtp.starttls.enable", "false");
            var mailSettings = new HistarMailProperties();
            mailSettings.setEnabled(true);
            mailSettings.setFrom("noreply@example.invalid");
            var mail = new HistarEmailService(
                    mailSettings, new TestHookProperties(), new SmtpEmailSender(transport, mailSettings));
            var orgSettings = new HistarOrgProperties();
            orgSettings.setSalesInbox("sales@example.invalid");
            var repository = mock(HeritageDigitizationInquiryRepository.class);
            when(repository.save(any(HeritageDigitizationInquiry.class))).thenAnswer(call -> {
                HeritageDigitizationInquiry lead = call.getArgument(0);
                lead.setId(UUID.randomUUID());
                return lead;
            });
            var service = new B2b2cInquiryService(repository, mail, orgSettings,
                    mock(AdminAuditService.class), mock(CurrentUserAccessor.class));

            var response = service.submit(new B2b2cInquiryRequest(
                    "Di tích thử nghiệm", "Lan", "Lan@example.invalid", "0901234567",
                    "UNDECIDED", "Cần tour <script>alert(1)</script>", null, null, null,
                    "Ban quản lý", "Phòng truyền thông", "Hà Nội",
                    "Ảnh tư liệu", "Quý III/2027"));

            assertThat(response.status()).isEqualTo("NEW");
            List<byte[]> delivered = captured.get(8, TimeUnit.SECONDS);
            assertThat(delivered).hasSize(2);
            var customer = mime(delivered.get(0));
            var sales = mime(delivered.get(1));
            assertThat(recipient(customer)).isEqualTo("lan@example.invalid");
            assertThat(customer.getSubject()).contains("Đã nhận yêu cầu");
            assertThat(HtmlUtils.htmlUnescape(html(customer))).contains("Di tích thử nghiệm");
            assertThat(recipient(sales)).isEqualTo("sales@example.invalid");
            assertThat(sales.getSubject()).contains("Yêu cầu tư vấn");
            assertThat(HtmlUtils.htmlUnescape(html(sales))).contains("Ban quản lý", "Ảnh tư liệu",
                    "Quý III/2027", response.id().toString());
            assertThat(html(sales)).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    private static MimeMessage mime(byte[] data) throws Exception {
        return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(data));
    }

    private static String recipient(MimeMessage message) throws Exception {
        return ((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress();
    }

    private static String html(Part part) throws Exception {
        if (part.isMimeType("text/html")) return (String) part.getContent();
        if (part.isMimeType("multipart/*")) {
            Multipart multi = (Multipart) part.getContent();
            for (int i = 0; i < multi.getCount(); i++) {
                String body = html(multi.getBodyPart(i));
                if (body != null) return body;
            }
        }
        return null;
    }

    private static byte[] receive(ServerSocket listener) throws Exception {
        try (var socket = listener.accept()) {
            socket.setSoTimeout(5000);
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            var writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII);
            reply(writer, "220 local test SMTP");
            var data = new ByteArrayOutputStream();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("EHLO ") || line.startsWith("HELO ") || line.startsWith("MAIL FROM:")
                        || line.startsWith("RCPT TO:")) reply(writer, "250 accepted");
                else if (line.equals("DATA")) {
                    reply(writer, "354 send data");
                    while ((line = reader.readLine()) != null && !line.equals(".")) {
                        if (line.startsWith("..")) line = line.substring(1);
                        data.write((line + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
                        if (data.size() > 1024 * 1024) throw new IllegalStateException("SMTP test message too large");
                    }
                    if (line == null) throw new IllegalStateException("Incomplete SMTP DATA");
                    reply(writer, "250 captured");
                } else if (line.equals("QUIT")) {
                    reply(writer, "221 bye");
                    return data.toByteArray();
                } else reply(writer, "500 unsupported");
            }
            throw new IllegalStateException("SMTP connection closed before QUIT");
        }
    }

    private static void reply(OutputStreamWriter writer, String line) throws Exception {
        writer.write(line + "\r\n");
        writer.flush();
    }
}
