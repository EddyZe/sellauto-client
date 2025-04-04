package com.example.application.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

    public static <T> T readFromJson(String json, Class<T> type) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка десериализации JSON", e);
        }
    }
}
