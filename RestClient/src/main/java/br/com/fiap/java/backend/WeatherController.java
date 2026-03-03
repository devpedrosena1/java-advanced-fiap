package br.com.fiap.java.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feign")
public class WeatherController {

    private FeignWeatherClient client;

    public WeatherController(FeignWeatherClient client) {
        this.client = client;
    }

    @GetMapping("/weather")
    public String getWeather(@RequestParam double lat,
                             @RequestParam double lon) {
        return this.client.getWeather(lat, lon, true, "America/Sao_Paulo");
    }

    // "http://localhost:8080/feign/weather?lat=-23.5505&lon=-46.6333"

}
