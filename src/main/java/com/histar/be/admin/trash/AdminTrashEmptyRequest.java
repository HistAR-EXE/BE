package com.histar.be.admin.trash;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminTrashEmptyRequest(
        @NotBlank @Size(min = 10, max = 2000) String reason,
        @Pattern(regexp = "DISCOVERY_POINT|ARTIFACT|QUEST") String type) {}
