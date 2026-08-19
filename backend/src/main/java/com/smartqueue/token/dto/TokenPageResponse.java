package com.smartqueue.token.dto;

import java.util.List;

public record TokenPageResponse(
    List<TokenResponse> content, int page, int size, long totalElements, int totalPages) {}
