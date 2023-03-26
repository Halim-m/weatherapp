package com.example.weatherapp.service;

import com.example.weatherapp.dto.WeatherDto;
import com.example.weatherapp.dto.WeatherResponse;
import com.example.weatherapp.exception.ErrorResponse;
import com.example.weatherapp.exception.WeatherStackApiException;
import com.example.weatherapp.model.Weather;
import com.example.weatherapp.repository.WeatherRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.example.weatherapp.constants.Constant.*;

@Service
@CacheConfig(cacheNames = "weather")
public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock clock;


    public WeatherService(WeatherRepository weatherRepository,
                          RestTemplate restTemplate,
                          Clock clock){
        this.weatherRepository = weatherRepository;
        this.restTemplate = restTemplate;
        this.clock = clock;
    }

    @Cacheable(key = "#city")
    public WeatherDto getWeather(String city) {

        Optional<Weather> weatherEntityOptional = weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(city);

        return weatherEntityOptional.map(weather -> {
            if (weather.getUpdatedTime().isBefore(getLocalDateTimeNow().minusMinutes(API_CALL_LIMIT))) {
                logger.info(String.format("Creating a new city weather from weather stack api for %s due to the current one is not up-to-date", city));
                return createCityWeather(city);
            }
            logger.info(String.format("Getting weather from database for %s due to it is already up-to-date", city));
            return WeatherDto.convert(weather);
        }).orElseGet(() -> createCityWeather(city));
    }

    @CachePut(key = "#city")
    public WeatherDto createCityWeather(String city) {
        logger.info("Requesting weather stack api for city: " + city);
        String url = getWeatherStackUrl(city);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);

        try {
            WeatherResponse weatherResponse = mapper.readValue(responseEntity.getBody(), WeatherResponse.class);
            return WeatherDto.convert(saveWeatherEntity(city, weatherResponse));
        } catch (JsonProcessingException e) {
            try {
                ErrorResponse errorResponse = mapper.readValue(responseEntity.getBody(), ErrorResponse.class);
                throw new WeatherStackApiException(errorResponse);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex.getMessage());
            }
        }
    }

    @CacheEvict(allEntries = true)
    @PostConstruct
    @Scheduled(fixedRateString = "${weather-stack.cache-ttl}")
    public void clearCache(){
        logger.info("Caches are cleared");
    }

    private Weather saveWeatherEntity(String city, WeatherResponse response) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Weather weatherEntity = new Weather(city,
                response.location().name(),
                response.location().country(),
                response.current().temperature(),
                getLocalDateTimeNow(),
                LocalDateTime.parse(response.location().localtime(), formatter));

        return weatherRepository.save(weatherEntity);
    }

    private String getWeatherStackUrl(String city) {
        return WEATHER_STACK_API_BASE_URL + WEATHER_STACK_API_ACCESS_KEY_PARAM + API_KEY + WEATHER_STACK_API_QUERY_PARAM + city;
    }

    private LocalDateTime getLocalDateTimeNow() {
        Instant instant = clock.instant();
        return LocalDateTime.ofInstant(
                instant,
                Clock.systemDefaultZone().getZone());
    }
}
