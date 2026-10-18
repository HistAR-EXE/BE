package com.histar.be.admin.trash;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AdminTrashBulkRequest(
        @NotBlank @Pattern(regexp = "RESTORE|PURGE") String action,
        @NotEmpty @Size(max = 50) @Valid List<Item> items,
        @Size(max = 2000) String reason) {

    public record Item(
            @NotBlank @Pattern(regexp = "DISCOVERY_POINT|ARTIFACT|QUEST") String resourceType,
            @NotNull UUID id,
            @NotNull Long rowVersion) {}
}
