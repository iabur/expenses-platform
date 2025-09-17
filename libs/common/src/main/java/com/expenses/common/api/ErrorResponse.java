package com.expenses.common.api;

public record ErrorResponse(String type, String title, int status, String detail) {}
