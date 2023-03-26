package com.example.weatherapp.repository;

import com.example.weatherapp.model.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeatherRepository extends JpaRepository<Weather,String> {
    //select * from entity where requestedCityName order by updateTime desc limit 1
    Optional<Weather> findFirstByRequestedCityNameOrderByUpdatedTimeDesc(String city);
}
