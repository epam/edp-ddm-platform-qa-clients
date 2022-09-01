package platform.qa.rest.client.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.extern.log4j.Log4j2;
import platform.qa.entities.IEntity;
import platform.qa.rest.client.RestClient;
import platform.qa.rest.utils.JsonUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

@Log4j2
public class RestClientImpl implements RestClient {
    private RequestSpecification requestSpecification;

    public RestClientImpl(String baseUrl, @Nullable  String accessToken) {
        requestSpecification = getRequestSpecification(baseUrl, accessToken);
    }

    @Override
    public <Response> Response get(String path, @Nullable Map<String, String> pathParams, Type type, int statusCode) {
        log.info(MessageFormat.format("Performing GET request on {0} with path params {1}", path, pathParams));

        return prepareRequestSpecification(pathParams)
                .get(path)
                .then()
                .statusCode(statusCode)
                .extract()
                .as(type);
    }

    @Override
    public <Request extends IEntity, Response> Response post(String path,
                                                             @Nullable Map<String, String> pathParams,
                                                             Request body,
                                                             Type type,
                                                             int statusCode) {
        log.info(MessageFormat.format("Performing POST request on {0} with path params {1} with body {2}",
                path, pathParams, JsonUtils.toJson(body))
        );

        return prepareRequestSpecification(pathParams)
                .body(body)
                .post(path)
                .then()
                .statusCode(statusCode)
                .extract()
                .as(type);
    }

    @Override
    public <Request extends IEntity, Response> Response put(String path,
                                                            @Nullable Map<String, String> pathParams,
                                                            Request body,
                                                            Type type,
                                                            int statusCode) {

        log.info(MessageFormat.format(
                "Performing PUT request on {0} with path params {1} with body {2}",
                path, pathParams, JsonUtils.toJson(body))
        );

        return prepareRequestSpecification(pathParams)
                .body(body)
                .put(path)
                .then()
                .statusCode(statusCode)
                .extract()
                .as(type);
    }

    @Override
    public void delete(String path, Type type, int statusCode) {
        log.info(MessageFormat.format("Performing DELETE request on {0}", path));

        prepareRequestSpecification()
                .delete(path)
                .then()
                .statusCode(statusCode);
    }

    public RequestSpecification prepareRequestSpecification(@Nullable Map<String, String> pathParams) {
        if (pathParams != null && !pathParams.isEmpty())
            requestSpecification.pathParams(pathParams);

        return RestAssured.given().spec(requestSpecification);
    }

    public RequestSpecification prepareRequestSpecification() {
        return prepareRequestSpecification(null);
    }

    private RequestSpecification getRequestSpecification(String baseUrl, @Nullable String accessToken) {
        var requestSpecBuilder =  new RequestSpecBuilder()
                .setConfig(
                        RestAssuredConfig
                                .config()
                                .objectMapperConfig(
                                        new ObjectMapperConfig().jackson2ObjectMapperFactory(
                                                (cls, charset) -> new ObjectMapper()
                                                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                        .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false)
                                        )
                                )
                                .logConfig(
                                        LogConfig
                                                .logConfig()
                                                .enableLoggingOfRequestAndResponseIfValidationFails()
                                                .enablePrettyPrinting(true)
                                )
                )
                .setContentType(ContentType.JSON)
                .setBaseUri(baseUrl);


        if (!Objects.isNull(accessToken))
            requestSpecBuilder.addHeader("X-Access-Token", accessToken);

        return requestSpecBuilder.build().given();
    }

}
