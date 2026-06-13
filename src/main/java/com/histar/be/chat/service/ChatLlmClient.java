package com.histar.be.chat.service;

public interface ChatLlmClient {

    String name();

    boolean isAvailable();

    String generate(String prompt);
}
