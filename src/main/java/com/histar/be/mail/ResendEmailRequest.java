package com.histar.be.mail;

public record ResendEmailRequest(String from, String[] to, String subject, String html) {}
