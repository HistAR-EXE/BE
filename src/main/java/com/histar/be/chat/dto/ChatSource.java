package com.histar.be.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ChatSource(
        String title,
        String excerpt,
        String url,
        @JsonInclude(JsonInclude.Include.NON_NULL) String scope) {

    public ChatSource(String title, String excerpt, String url) {
        this(title, excerpt, url, null);
    }
}
