package com.histar.be.mail;

public interface EmailSender {

    void sendHtml(String to, String subject, String html);

    void sendText(String to, String subject, String text);
}
