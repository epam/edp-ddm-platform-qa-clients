package platform.qa.rest.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.lang.reflect.Type;

public final class JsonUtils {

    private JsonUtils() {
        throw new IllegalStateException("This is utility class!");
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return new Gson().fromJson(json, clazz);
    }

    public static  <T> T fromJson(BufferedReader json, Class<T> clazz) {
        return new Gson().fromJson(json, clazz);
    }

    @SneakyThrows
    public static <T> T mapFromJson(BufferedReader json, Class<T> clazz) {
        var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper.readValue(json, clazz);
    }

    public static <T> T fromJson(String json, Type type) {
        return new Gson().fromJson(json, type);
    }

    public static  <T> String toJson(T object) {
        return new Gson().toJson(object);
    }
}