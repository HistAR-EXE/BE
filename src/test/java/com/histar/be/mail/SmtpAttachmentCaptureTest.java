package com.histar.be.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.config.HistarMailProperties;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class SmtpAttachmentCaptureTest {
    @Test
    @Timeout(15)
    void realSmtpDeliversCompletePassportToLoopbackCaptureOnly() throws Exception {
        byte[] png;
        var image = new BufferedImage(1200, 1500, BufferedImage.TYPE_INT_RGB);
        image.setRGB(1199, 1499, 0x1256AB);
        try (var output = new ByteArrayOutputStream()) {
            assertThat(ImageIO.write(image, "png", output)).isTrue();
            png = output.toByteArray();
        }
        var connection = new AtomicReference<Socket>();
        var executor = Executors.newSingleThreadExecutor(task -> {
            var thread = new Thread(task, "passport-local-smtp-capture");
            thread.setDaemon(true);
            return thread;
        });
        try (var listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(5000);
            var captured = executor.submit(() -> capture(listener, connection));
            try {
                var transport = new JavaMailSenderImpl();
                transport.setHost("127.0.0.1");
                transport.setPort(listener.getLocalPort());
                transport.getJavaMailProperties().setProperty("mail.smtp.auth", "false");
                transport.getJavaMailProperties().setProperty("mail.smtp.starttls.enable", "false");
                var configuration = new HistarMailProperties();
                configuration.setFrom("otp@example.invalid");
                new SmtpEmailSender(transport, configuration).sendAttachment(
                        "TimeLens <histar.timelens@gmail.com>", "QA@example.invalid", "Passport capture",
                        "<p>Your complete passport is attached.</p>", "passport.png", png);

                var result = captured.get(5, TimeUnit.SECONDS);
                assertThat(result.envelopeFrom()).isEqualTo("MAIL FROM:<histar.timelens@gmail.com>");
                assertThat(result.envelopeTo()).isEqualTo("RCPT TO:<QA@example.invalid>");
                var message = new MimeMessage(Session.getInstance(new Properties()),
                        new ByteArrayInputStream(result.mime()));
                var from = (InternetAddress) message.getFrom()[0];
                assertThat(from.getAddress()).isEqualTo("histar.timelens@gmail.com");
                assertThat(from.getPersonal()).isEqualTo("TimeLens");
                assertThat(message.getRecipients(Message.RecipientType.TO)).hasSize(1);
                assertThat(((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress())
                        .isEqualTo("QA@example.invalid");
                var attachment = findAttachment(message);
                assertThat(attachment).isNotNull();
                assertThat(attachment.getFileName()).isEqualTo("passport.png");
                assertThat(attachment.isMimeType("image/png")).isTrue();
                byte[] received;
                try (var input = attachment.getInputStream()) {
                    received = input.readAllBytes();
                }
                assertThat(received).isEqualTo(png);
                var decoded = ImageIO.read(new ByteArrayInputStream(received));
                assertThat(decoded.getWidth()).isEqualTo(1200);
                assertThat(decoded.getHeight()).isEqualTo(1500);
                assertThat(decoded.getRGB(1199, 1499) & 0xFFFFFF).isEqualTo(0x1256AB);
                assertThat(configuration.getFrom()).isEqualTo("otp@example.invalid");
                assertThat(transport.getJavaMailProperties()).doesNotContainKey("mail.smtp.timeout");
            } finally {
                var socket = connection.get();
                if (socket != null) socket.close();
                captured.cancel(true);
            }
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static Part findAttachment(Part part) throws Exception {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) return part;
        if (part.isMimeType("multipart/*")) {
            var multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                var found = findAttachment(multipart.getBodyPart(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Capture capture(ServerSocket listener, AtomicReference<Socket> connection) throws Exception {
        try (var socket = listener.accept()) {
            connection.set(socket);
            socket.setSoTimeout(5000);
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            var writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII);
            reply(writer, "220 localhost test capture");
            String from = null;
            String to = null;
            var mime = new ByteArrayOutputStream();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("EHLO ") || line.startsWith("HELO ")) {
                    reply(writer, "250 localhost");
                } else if (line.startsWith("MAIL FROM:")) {
                    from = line;
                    reply(writer, "250 accepted");
                } else if (line.startsWith("RCPT TO:")) {
                    to = line;
                    reply(writer, "250 accepted");
                } else if (line.equals("DATA")) {
                    reply(writer, "354 end with dot");
                    while ((line = reader.readLine()) != null && !line.equals(".")) {
                        if (line.startsWith("..")) line = line.substring(1);
                        mime.write((line + "\r\n").getBytes(StandardCharsets.US_ASCII));
                        if (mime.size() > 8 * 1024 * 1024) throw new IllegalStateException("Capture size exceeded");
                    }
                    if (line == null) throw new IllegalStateException("Incomplete SMTP DATA");
                    reply(writer, "250 captured locally");
                } else if (line.equals("QUIT")) {
                    reply(writer, "221 bye");
                    return new Capture(from, to, mime.toByteArray());
                } else {
                    reply(writer, "500 unsupported command");
                }
            }
            throw new IllegalStateException("SMTP session ended before QUIT");
        }
    }

    private static void reply(OutputStreamWriter writer, String line) throws Exception {
        writer.write(line + "\r\n");
        writer.flush();
    }

    private record Capture(String envelopeFrom, String envelopeTo, byte[] mime) {}
}
