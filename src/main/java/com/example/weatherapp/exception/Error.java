package com.example.weatherapp.exception;

public record Error(
        String code,
        String type,
        String info
) {
}
