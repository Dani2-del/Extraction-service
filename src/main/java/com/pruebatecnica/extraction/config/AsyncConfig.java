package com.pruebatecnica.extraction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Pool de hilos dedicado al procesamiento de extracciones.
 * Se limita el tamaño para no lanzar peticiones ilimitadas contra
 * la fuente externa (Automation Exercise) de forma simultanea.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "extractionExecutor")
    public Executor extractionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("extraction-");
        executor.initialize();
        return executor;
    }
}
