package com.histar.be.mail;

public interface EmailSender {

    void sendHtml(String to, String subject, String html);

    void sendText(String to, String subject, String text);

    default void sendHtmlBounded(String to, String subject, String html) {
        throw new EmailDeliveryException("Bounded HTML delivery is not supported");
    }

    default void sendAttachment(String from, String to, String subject, String html, String filename, byte[] png) {
        throw new EmailDeliveryException("Attachment delivery is not supported");
    }
}
