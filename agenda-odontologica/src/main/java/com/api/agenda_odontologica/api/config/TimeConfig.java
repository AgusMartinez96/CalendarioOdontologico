package com.api.agenda_odontologica.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    ZoneId applicationZone(@Value("${app.time-zone:America/Argentina/Buenos_Aires}") String timeZone) {
        return ZoneId.of(timeZone);
    }
}
