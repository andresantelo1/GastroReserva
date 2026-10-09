package com.example.gastroreservabackend1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RestaurantClockConfig {
    @Bean
    Clock restaurantClock(@Value("${app.restaurant.time-zone:America/La_Paz}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
