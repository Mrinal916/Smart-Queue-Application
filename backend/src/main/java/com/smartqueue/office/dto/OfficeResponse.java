package com.smartqueue.office.dto;

import java.util.UUID;

public record OfficeResponse(
    UUID publicId, String code, String name, String address, String category, boolean active) {}
