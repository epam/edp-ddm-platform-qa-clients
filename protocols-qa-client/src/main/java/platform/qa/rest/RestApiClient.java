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

package platform.qa.rest;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import platform.qa.entities.IEntity;
import platform.qa.entities.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.message.ParameterizedMessage;
import com.github.javafaker.Faker;

/**
 * Client to work with REST API requests
 */
@Log4j2
public class RestApiClient extends BaseServiceClient {

    @Getter
    private HashMap<String, String> headers = new HashMap<>();

    private Boolean isSignatureSetted = false;
    private Boolean isBusinessProcessIdSetted = false;
    private Faker faker = new Faker();
    private RequestSpecification rs;

    private String xSourceSystem = "X-Source-System";
    private String xSourceApplication = "X-Source-Application";
    private String xSourceBusinessProcess = "X-Source-Business-Process";
    private String xSourceBusinessActivity = "X-Source-Business-Activity";
    private String xSourceBusinessProcessDefinitionId = "X-Source-Business-Process-Definition-Id";
    private String xSourceBusinessProcessInstanceId = "X-Source-Business-Process-Instance-Id";
    private String xSourceRootBusinessProcessInstanceId = "X-Source-Root-Business-Process-Instance-Id";
    private String xSourceBusinessActivityInstanceId = "X-Source-Business-Activity-Instance-Id";
    private String xAccessToken = "X-Access-Token";
    private String xDigitalSignature = "X-Digital-Signature";
    private String xDigitalSignatureDerived = "X-Digital-Signature-Derived";
    private String xXsrfToken = "X-XSRF-TOKEN";
    private String cookie = "Cookie";


    private final HashMap<String, String> mandatoryHeaders = new HashMap<>() {{
        put(xSourceSystem, faker.letterify("??????????"));
        put(xSourceApplication, faker.letterify("??????????"));
        put(xXsrfToken, "Token");
        put(cookie, "XSRF-TOKEN=Token");

        if (!isSignatureSetted) {
            put(xDigitalSignature, faker.letterify("??????????"));
            put(xDigitalSignatureDerived, faker.letterify("??????????"));
        }
    }};

    private final HashMap<String, String> nonMandatoryHeaders = new HashMap<>() {{
        put(xSourceBusinessProcess, faker.letterify("??????????"));
        put(xSourceBusinessActivity, faker.letterify("??????????"));
        put(xSourceBusinessProcessDefinitionId, faker.letterify("??????????"));
        put(xSourceBusinessActivityInstanceId, UUID.randomUUID().toString());

        if (!isBusinessProcessIdSetted) {
            String id = UUID.randomUUID().toString();
            put(xSourceBusinessProcessInstanceId, id);
            put(xSourceRootBusinessProcessInstanceId, id);
        }
    }};

    public RestApiClient(Service dataFactory) {
        headers.putAll(mandatoryHeaders);
        headers.putAll(nonMandatoryHeaders);
        rs = init(dataFactory.getUrl(), dataFactory.getUser().getToken());
        rs = addHeaders(headers);
    }


    public RestApiClient(Service dataFactory, Map<String, String> customHeaders) {
        headers.putAll(customHeaders);
        headers.putAll(mandatoryHeaders);
        headers.putAll(nonMandatoryHeaders);
        rs = init(dataFactory.getUrl(), dataFactory.getUser().getToken());
        rs = addHeaders(headers);
    }

    public RestApiClient(String url, String digitalSignature) {
        rs = init(url);
        mandatoryHeaders.put(xDigitalSignature, digitalSignature);
        mandatoryHeaders.put(xDigitalSignatureDerived, digitalSignature);
        headers.putAll(mandatoryHeaders);
        headers.putAll(nonMandatoryHeaders);
        rs = addHeaders(headers);
    }

    public RestApiClient(Service datFactory, String digitalSignature, Map<String, String> additionalHeaders) {
        rs = init(datFactory.getUrl());
        headers.put(xDigitalSignatureDerived, digitalSignature);
        headers.put(xDigitalSignature, digitalSignature);

        if (additionalHeaders != null && !additionalHeaders.isEmpty()) {
            headers.putAll(additionalHeaders);
        }
        rs = addHeaders(headers);
    }

    public RestApiClient(Service dataFactory, String digitalSignature) {
        rs = init(dataFactory.getUrl(), dataFactory.getUser().getToken());

        setDigitalSignatureValue(digitalSignature);
        mandatoryHeaders.put(xDigitalSignatureDerived, digitalSignature);

        headers.putAll(mandatoryHeaders);
        headers.putAll(nonMandatoryHeaders);
        rs = addHeaders(headers);
    }

    public RestApiClient(Service dataFactory, String digitalSignature, String businessProcessInstanceId) {
        rs = init(dataFactory.getUrl(), dataFactory.getUser().getToken());

        setDigitalSignatureValue(digitalSignature);
        mandatoryHeaders.put(xDigitalSignatureDerived, digitalSignature);

        headers.putAll(mandatoryHeaders);
        nonMandatoryHeaders.put(xSourceBusinessProcessInstanceId, businessProcessInstanceId);
        nonMandatoryHeaders.put(xSourceRootBusinessProcessInstanceId, businessProcessInstanceId);
        headers.putAll(nonMandatoryHeaders);
        rs = addHeaders(headers);
    }

    private void setDigitalSignatureValue(String digitalSignature) {
        if (Boolean.parseBoolean(System.getProperty("CA_ISOLATION"))) {
            mandatoryHeaders.put(xDigitalSignature, "Key-6.dat");
            return;
        }

        mandatoryHeaders.put(xDigitalSignature, digitalSignature);
    }

    public RestApiClient(String url, Map<String, String> headers) {
        rs = init(url);
        this.headers.putAll(headers);
        rs = addHeaders(this.headers);
    }

    public RestApiClient(String url, String token, String businessProcessInstanceId) {
        rs = init(url, token);
        headers.putAll(mandatoryHeaders);
        nonMandatoryHeaders.put(xSourceBusinessProcessInstanceId, businessProcessInstanceId);
        nonMandatoryHeaders.put(xSourceRootBusinessProcessInstanceId, businessProcessInstanceId);
        headers.putAll(nonMandatoryHeaders);
        rs = addHeaders(headers);
    }

    public RestApiClient(String url) {
        rs = init(url);
    }

    public RestApiClient setMandatoryHeaders() {
        headers.putAll(mandatoryHeaders);
        rs = addHeaders(headers);
        return this;
    }

    public RestApiClient setNonMandatoryHeaders() {
        headers.putAll(nonMandatoryHeaders);
        rs = addHeaders(headers);
        return this;
    }

    private RequestSpecification init(String url, String token) {
        return init(url)
                .header(xAccessToken, token);
    }

    private RequestSpecification initWithBusinessProcessId(String url, String token, String businessProcessInstanceId) {
        isBusinessProcessIdSetted = true;
        return init(url)
                .header(xAccessToken, token)
                .header(xSourceRootBusinessProcessInstanceId, businessProcessInstanceId)
                .header(xSourceBusinessProcessInstanceId, businessProcessInstanceId);
    }

    private RequestSpecification init(String url, String token, String digitalSignature) {
        isSignatureSetted = true;
        return init(url)
                .header(xAccessToken, token)
                .header(xDigitalSignature, digitalSignature)
                .header(xDigitalSignatureDerived, digitalSignature);
    }

    private RequestSpecification init(String url) {
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        rs = requestSpecBuilder
                .setConfig(
                        config()
                                .logConfig(logConfig()
                                        .enableLoggingOfRequestAndResponseIfValidationFails()
                                        .enablePrettyPrinting(Boolean.TRUE))
                )
                .build();

        return given().spec(rs).baseUri(url);
    }

    private RequestSpecification addHeaders(Map headers) {
        return rs
                .headers(headers);
    }

    public RestApiClient addBusinessProcessId(String businessProcessId) {
        isBusinessProcessIdSetted = true;
        rs.header(xSourceBusinessProcessInstanceId, businessProcessId);
        rs.header(xSourceRootBusinessProcessInstanceId, businessProcessId);
        return this;
    }

    public RestApiClient setToken(String token) {
        rs
                .header(xAccessToken, token);
        return this;
    }

    public RestApiClient setDigitalSignature(String digitalSignature) {
        isSignatureSetted = true;
        rs.header(xDigitalSignature, digitalSignature);
        return this;
    }

    public RestApiClient setDigitalSignatureDerived(String digitalSignatureDerived) {
        isSignatureSetted = true;
        rs.header(xDigitalSignatureDerived, digitalSignatureDerived);
        return this;
    }

    public HashMap<String, String> postAndReturnHeaders(IEntity payload, String url) {
        log.info(new ParameterizedMessage("POST до відповідного url та повернення переліку заголовків запиту: {}",
                url));
        post(payload, url);
        return headers;
    }

    public ValidatableResponse sendGetWithParams(String url, Map<String, String> listParams) {
        log.info(new ParameterizedMessage("GET до відповідного url: {} з параметрами {}", url, listParams));
        return waitFor(rs
                .queryParams(listParams)
                .when(), Method.GET, url)
                .then().statusCode(200);
    }

    public Response postNegative(IEntity payload, String url) {
        log.info(new ParameterizedMessage("POST до відповідного url: {}", url));
        return waitFor(rs
                .contentType(ContentType.JSON)
                .body(payload), Method.POST, url);
    }

    public Response postWithWrongContentType(String payload, String url) {
        log.info(new ParameterizedMessage("POST до відповідного url: {}", url));
        return waitFor(rs
                .contentType(ContentType.XML)
                .body(payload), Method.POST, url);
    }

    public Response getNegative(String id, String url) {
        log.info(new ParameterizedMessage("GET до відповідного url: {}", url));
        return waitFor(rs, Method.GET, url + id);
    }

    public Response get(String url) {
        log.info(new ParameterizedMessage("GET до відповідного url: {}", url));
        return waitFor(rs, Method.GET, url);
    }

    public Response getWithParams(String url, Map params) {
        log.info(new ParameterizedMessage("GET до відповідного url: {} з параметрами: {}", url, params));
        return this.rs.queryParams(params).when().get(url, new Object[0]);
    }

    public Response get(String id, String url) {
        log.info(new ParameterizedMessage("GET до відповідного url: {} з id {}", url, id));
        Response response = waitFor(rs, Method.GET, url + id);
        assertThat(response.getStatusCode()).as("Entity was not returned: " + response.body().asString()).isEqualTo(200);
        return response;
    }

    public Response post(IEntity payload, String url) {
        log.info(new ParameterizedMessage("POST до відповідного url: {}", url));
        Response response = waitFor(rs.contentType(ContentType.JSON).body(payload), Method.POST, url);
        assertThat(response.getStatusCode()).as("Entity was not inserted: "
                + response.body().asString()).isEqualTo(201);
        return response;
    }

    public Response postSearchCondition(IEntity payload, String url) {
        log.info(new ParameterizedMessage("POST до відповідного url: {}", url));
        Response response = waitFor(rs.contentType(ContentType.JSON).body(payload), Method.POST, url);
        assertThat(response.getStatusCode()).as("Entity was not inserted: "
                + response.body().asString()).isEqualTo(200);
        return response;
    }

    public Response post(String body, String url) {
        log.info(new ParameterizedMessage("POST до відповідного url: {}", url));
        return waitFor(rs.contentType(ContentType.JSON).body(body), Method.POST, url);
    }

    public void put(String id, IEntity payload, String url) {
        log.info(new ParameterizedMessage("PUT до відповідного url: {}", url));
        var rp = waitFor(rs
                .contentType(ContentType.JSON)
                .body(payload), Method.PUT, url + id)
                .then()
                .extract();
        assertThat(rp.statusCode()).as("Entity was not updated: " + rp.body().asString()).isEqualTo(204);
    }

    public void put(String id, String body, String url) {
        log.info(new ParameterizedMessage("PUT до відповідного url: {}", url));
        var rp = waitFor(rs
                .contentType(ContentType.JSON)
                .body(body), Method.PUT, url + id)
                .then()
                .extract();
        assertThat(rp.statusCode()).as("Entity was not updated: " + rp.body().asString()).isEqualTo(204);
    }

    public Response putUpsert(IEntity payload, String url) {
        log.info(new ParameterizedMessage("UPSERT до відповідного url: {}", url));
        return waitFor(rs
                .contentType(ContentType.JSON)
                .body(payload), Method.PUT, url);
    }

    public ExtractableResponse<Response> put(IEntity payload, String url) {
        log.info(new ParameterizedMessage("PUT до відповідного url: {}", url));
        var rp = waitFor(rs
                .contentType(ContentType.JSON)
                .body(payload), Method.PUT, url)
                .then()
                .extract();
        assertThat(rp.statusCode()).as("Entity was not updated: " + rp.body().asString()).isEqualTo(200);
        return rp;
    }

    public void delete(String id, String url) {
        log.info(new ParameterizedMessage("DELETE до відповідного url: {}", url));
        var rp = waitFor(rs, Method.DELETE, url + id)
                .then()
                .extract();
        assertThat(rp.statusCode()).as("Entity was not deleted: " + rp.body().asString()).isEqualTo(204);
    }

    public void patch(String id, IEntity payload, String url) {
        log.info(new ParameterizedMessage("PATCH до відповідного url: {}", url));
        var rp = waitFor(rs
                .contentType(ContentType.JSON)
                .body(payload), Method.PATCH, url + id)
                .then()
                .extract();
        assertThat(rp.statusCode()).as("Entity was not updated: " + rp.body().asString()).isEqualTo(204);
    }

}
