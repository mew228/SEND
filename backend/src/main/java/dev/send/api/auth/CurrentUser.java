package dev.send.api.auth;

import javax.annotation.Nullable;

public record CurrentUser(
        String id,
        @Nullable String email) {}
