package com.example.weatherapp.dto;

public record WeatherResponse(
        Request request,
        Location location,
        Current current
) {
}
