package com.aleksaxe.presonalassistent.presonalassistent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter @Setter
public class ExchangeRate {
    private String disclaimer;
    private String date;
    private long timestamp;
    private String base;

    @JsonProperty("rates")
    private Map<String, BigDecimal> rates;
}
