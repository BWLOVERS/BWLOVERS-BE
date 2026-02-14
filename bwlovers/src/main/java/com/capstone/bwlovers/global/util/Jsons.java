package com.capstone.bwlovers.global.util;

import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Jsons {

    private static final ObjectMapper OM = new ObjectMapper().registerModule(new JavaTimeModule());

    public static <T> T read(String json, Class<T> clazz) {
        try {
            return OM.readValue(json, clazz);
        } catch (Exception e) {
            throw new CustomException(ExceptionCode.JSON_DESERIALIZATION_FAILED);
        }
    }
}