package com.example.weatherapp.dto;

public record Request(
        String type,
        String query,
        String language,
        String unit
) {
}
