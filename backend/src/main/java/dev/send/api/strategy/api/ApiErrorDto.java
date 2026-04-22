package dev.send.api.strategy.api;

import java.util.List;

public record ApiErrorDto(String code, String message, List<String> details) {}
