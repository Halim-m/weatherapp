package com.example.weatherapp.exception;

public record RestTemplateError(
        String timestamp,
        String status,
        String error,
        String path
) {
}
