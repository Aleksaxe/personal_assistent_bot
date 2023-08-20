package com.aleksaxe.presonalassistent.presonalassistent.services;

import com.aleksaxe.presonalassistent.presonalassistent.model.ExchangeRate;
import com.aleksaxe.presonalassistent.presonalassistent.services.intefaces.ExchangeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

        } catch (Exception e) {
            e.getStackTrace();
        }
        return response;
    }


    //todo store in db by user preferences
    private static final List<String> myRates = List.of("USD", "EUR", "KGS");

    public InlineKeyboardMarkup createInlineKeyboardForRates() {
        ExchangeRate exchangeRate = getExchangeRate();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for(String key : exchangeRate.getRates().keySet()) {
            if(myRates.contains(key)) {
                BigDecimal rate = exchangeRate.getRates().get(key);
                if(rate.compareTo(BigDecimal.ZERO) != 0) {
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    String buttonText = key + " " + BigDecimal.ONE.divide(rate, 2, RoundingMode.HALF_UP);
                    String callbackData = "info_about_" + key;
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(buttonText);
                    button.setCallbackData(callbackData);
                    row.add(button);
                    keyboard.add(row);
                }
            }
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }
}
