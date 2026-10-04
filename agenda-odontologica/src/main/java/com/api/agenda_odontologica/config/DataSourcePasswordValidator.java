package com.api.agenda_odontologica.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class DataSourcePasswordValidator {
    @Bean
    static BeanFactoryPostProcessor requirePostgresPassword(Environment environment) {
        return beanFactory -> {
            String url = environment.getProperty("spring.datasource.url", "");
            if (url.startsWith("jdbc:postgresql:")
                    && !StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_PASSWORD"))) {
                throw new BeanDefinitionStoreException(
                        "Falta SPRING_DATASOURCE_PASSWORD; configurá la contraseña de PostgreSQL.");
            }
        };
    }
}
