package br.com.fiap.java.backend;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "openMeteoClient", url = "https://api.open-meteo.com")
public interface FeignWeatherClient {

    @GetMapping("/v1/forecast")
    public String getWeather(
            @RequestParam("latitude") double lat,
            @RequestParam("longitude") double lon,
            @RequestParam("current_weather") boolean currentWeather,
            @RequestParam("timezone") String timezone
    );
}
