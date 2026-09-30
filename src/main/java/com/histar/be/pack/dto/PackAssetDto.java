package com.histar.be.pack.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** One downloadable file in an offline pack. {@code bytes} is an optional size hint (null when unknown). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PackAssetDto(String path, String url, Long bytes, boolean required) {}
