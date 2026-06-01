package com.histar.be.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WarningCode {
    DEPRECATED_FIELD("DEPRECATED_FIELD"),
    PARTIAL_DATA("PARTIAL_DATA");

    private final String code;
}
