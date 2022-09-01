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

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Method;
import io.restassured.response.ResponseBodyExtractionOptions;
import io.restassured.specification.RequestSpecification;
import lombok.extern.log4j.Log4j2;
import platform.qa.entities.Service;

import org.apache.logging.log4j.message.ParameterizedMessage;


/**
 * Client to work with SOAP API requests
 */
@Log4j2
public class SoapClient extends BaseServiceClient {

    private RequestSpecification rs;

    public SoapClient(Service service) {
        rs = init(service.getUrl());
    }

    public SoapClient(Service service, String contentType) {
        rs = init(service.getUrl(), contentType);
    }

    private RequestSpecification init(String url, String contentType) {
        return RestAssured.given()
                .filter(new ResponseLoggingFilter())
                .filter(new RequestLoggingFilter())
                .baseUri(url)
                .header(new Header("Content-Type", contentType));
    }

    private RequestSpecification init(String url) {
        return RestAssured.given()
                .filter(new ResponseLoggingFilter())
                .filter(new RequestLoggingFilter())
                .baseUri(url)
                .contentType(ContentType.XML);
    }

    public ResponseBodyExtractionOptions post(String payload, String url) {
        log.info(new ParameterizedMessage("POST до відповідного url: {url} з тілом повідомлення {payload}", url,
                payload));
        return waitFor(rs
                .body(payload), Method.POST, url)
                .then()
                .statusCode(200)
                .extract()
                .body();
    }
}
