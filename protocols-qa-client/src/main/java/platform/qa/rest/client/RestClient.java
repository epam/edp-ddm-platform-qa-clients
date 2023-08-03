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

package platform.qa.rest.client;


import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Rest client for REST requests that supports methods:
 *
 * GET {@link RestClient#get(String, Map, Type, int)}
 * PUT {@link RestClient#put(String, Map, Object, Type, int)}
 * POST {@link RestClient#post(String, Map, Object, Type, int)}
 * DELETE {@link RestClient#delete(String, int)}
 */
public interface RestClient {

    /**
     * GET HTTP request
     * @param path path to REST API
     * @param pathParams params for path of REST API
     * @param type type of returned value
     * @param statusCode expected request status code
     * @return response of specified type
     * @param <Response> response type
     */
    <Response> Response get(String path,
                            @Nullable Map<String, String> pathParams,
                            Type type,
                            int statusCode);

    /**
     * POST HTTP request
     * @param path path to REST API
     * @param pathParams params for path of REST API
     * @param body request body
     * @param type type of returned value
     * @param statusCode expected request status code
     * @return response of specified type
     * @param <Request> request type
     * @param <Response> response type
     */
    <Request, Response> Response post(String path,
                                      @Nullable Map<String, String> pathParams,
                                      Request body,
                                      Type type,
                                      int statusCode);

    /**
     * PUT HTTP request
     * @param path path to REST API
     * @param pathParams params for path of REST API
     * @param body request body
     * @param type type of returned value
     * @param statusCode expected request status code
     * @return response of specified type
     * @param <Request> request type
     * @param <Response> response type
     */
    <Request, Response> Response put(String path,
                                     @Nullable Map<String, String> pathParams,
                                     Request body,
                                     Type type,
                                     int statusCode,
                                     @Nullable Map<String, String> headers);

    /**
     * DELETE HTTP request
     * @param path path to REST API
     * @param statusCode expected request status code
     */
    void delete(String path, int statusCode, @Nullable Map<String, String> headers);
}
