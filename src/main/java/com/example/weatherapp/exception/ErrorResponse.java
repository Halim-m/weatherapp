package com.example.weatherapp.exception;

public record ErrorResponse(
        String success,
        Error error
){
}
