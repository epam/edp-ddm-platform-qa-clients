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

package platform.qa.keycloak;

import java.io.IOException;
import java.util.logging.Logger;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

/**
 * Keycloak logger
 */
public class KeycloakLoggingFilter implements ClientResponseFilter {
    private static final Logger LOG = Logger.getLogger(KeycloakLoggingFilter.class.getName());

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        LOG.info(String.format("\nRequest URI: %s\n" +
                        "Request method: %s\n" +
                        "Request headers: %s\n" +
                        "Request cookies: %s\n" +
                        "Request entity: %s\n",
                requestContext.getUri(),
                requestContext.getMethod(),
                requestContext.getHeaders(),
                requestContext.getCookies(),
                requestContext.getEntity()));

        LOG.info(String.format("\nResponse code: %s\n"
                        + "Response message: %s\n"
                        + "Response date: %s\n",
                responseContext.getStatus(),
                responseContext.getStatusInfo() != null ? responseContext.getStatusInfo().getReasonPhrase() :
                        responseContext.getStatusInfo(),
                responseContext.getDate()
        ));
    }
}
