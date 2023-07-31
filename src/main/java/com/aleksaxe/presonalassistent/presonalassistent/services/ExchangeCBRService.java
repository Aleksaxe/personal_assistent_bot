package com.aleksaxe.presonalassistent.presonalassistent.services;

import com.aleksaxe.presonalassistent.presonalassistent.model.ExchangeRate;
import com.aleksaxe.presonalassistent.presonalassistent.services.intefaces.ExchangeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeCBRService implements ExchangeService {

    @Value("${cbr.api.url}")
    String cbrApiUrl;
    private final WebClient webClient;

    private ExchangeRate getExchangeRate() {
        ExchangeRate response = null;
        try {
            String block = webClient.get()
                    .uri(cbrApiUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper mapper = new ObjectMapper();
            response = mapper.readValue(block, ExchangeRate.class);

            // теперь вы можете использовать `response` для доступа к данным в JSON

        } catch (Exception e) {
            e.getStackTrace();
        }
        return response;  // возможно, вы захотите изменить это, чтобы возвращать response или другой результат
    }


    //todo store in db by user preferences
    private static final List<String> myRates = List.of("KZT", "USD", "EUR", "KGS");

    public String getUserRates() {
        ExchangeRate exchangeRate = getExchangeRate();
        Map<String, BigDecimal> userRates = new HashMap<>();
        for(String key : exchangeRate.getRates().keySet()) {
            if(myRates.contains(key)) {
                userRates.put(key, exchangeRate.getRates().get(key));
            }
        }

        StringBuilder sb = new StringBuilder();
        for(String key : userRates.keySet()) {
            BigDecimal rate = userRates.get(key);
            if(rate.compareTo(BigDecimal.ZERO) != 0) {
                String rateInfo = """
                RUB - %s %s / %s - RUB %s
                """.formatted(key, rate, key, BigDecimal.ONE.divide(rate, 2, RoundingMode.HALF_UP));
                sb.append(rateInfo);
            }
        }
        return sb.toString();
    }
}
