package br.com.fiap.java.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class SimpleWeatherController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/weather")
    public String getWeather(@RequestParam double lat,
                             @RequestParam double lon) {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&current_weather=true"
                + "&timezone=America/Sao_Paulo";

        System.out.println("Requisição para URL: " + url);
        return this.restTemplate.getForObject(url, String.class);

        // wget -O - "http://localhost:8080/weather?lat=-23.5505&lon=-46.6333"
    }
}
