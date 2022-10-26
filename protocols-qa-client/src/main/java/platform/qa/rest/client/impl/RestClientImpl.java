/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.qa.rest.client.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import lombok.extern.log4j.Log4j2;
import platform.qa.rest.client.RestClient;
import platform.qa.rest.utils.JsonUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link  RestClient}
 */
@Log4j2
public class RestClientImpl implements RestClient {
    private RequestSpecification requestSpecification;

    public RestClientImpl(String baseUrl, @Nullable  String accessToken) {
        requestSpecification = getRequestSpecification(baseUrl, accessToken, ContentType.JSON);
    }

    public RestClientImpl(String baseUrl, @Nullable String accessToken, ContentType contentType) {
        requestSpecification = getRequestSpecification(baseUrl, accessToken, contentType);
    }

    @Override
    public <Response> Response get(String path, @Nullable Map<String, String> pathParams, Type type, int statusCode) {
        log.info(MessageFormat.format("Performing GET request on {0} with path params {1}", path, pathParams));

        return extractResult(
                prepareRequestSpecification(pathParams)
                        .get(path)
                        .then()
                        .statusCode(statusCode)
                        .extract(),
                type);
    }

    @Override
    public <Request, Response> Response post(String path,
                                             @Nullable Map<String, String> pathParams,
                                             Request body,
                                             Type type,
                                             int statusCode) {
        log.info(MessageFormat.format("Performing POST request on {0} with path params {1} with body {2}",
                path, pathParams, JsonUtils.toJson(body))
        );

        var rs = prepareRequestSpecification(pathParams);

        if (!Objects.isNull(body))
            rs.body(body);

        return extractResult(rs
                        .post(path)
                        .then()
                        .statusCode(statusCode)
                        .extract(),
                type);
    }

    @Override
    public <Request, Response> Response put(String path,
                                            @Nullable Map<String, String> pathParams,
                                            Request body,
                                            Type type,
                                            int statusCode) {

        log.info(MessageFormat.format(
                "Performing PUT request on {0} with path params {1} with body {2}",
                path, pathParams, JsonUtils.toJson(body))
        );

        return extractResult(prepareRequestSpecification(pathParams)
                        .body(body)
                        .put(path)
                        .then()
                        .statusCode(statusCode)
                        .extract(),
                type);
    }

    @Override
    public void delete(String path, int statusCode) {
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

    public <T> T extractResult(ExtractableResponse response, Type type) {
        if (Void.class.getTypeName().equals(type.getTypeName()) || String.class.getTypeName().equals(type.getTypeName())) {
            return (T) response.asString();
        }

        return response.as(type);
    }

    private RequestSpecification getRequestSpecification(String baseUrl, @Nullable String accessToken, ContentType contentType) {
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
                .setContentType(contentType)
                .setBaseUri(baseUrl);

        if (!Objects.isNull(accessToken))
            requestSpecBuilder.addHeader("X-Access-Token", accessToken);

        return requestSpecBuilder.build().given();
    }
}
