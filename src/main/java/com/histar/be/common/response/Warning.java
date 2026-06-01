package com.histar.be.common.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Warning {

    private final WarningCode code;
    private final String message;
}
