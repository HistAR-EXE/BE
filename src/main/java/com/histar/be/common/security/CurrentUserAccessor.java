package com.histar.be.common.security;

import java.util.Optional;
import java.util.UUID;

public interface CurrentUserAccessor {

    Optional<String> getEmail();

    Optional<UUID> getUserId();
}
