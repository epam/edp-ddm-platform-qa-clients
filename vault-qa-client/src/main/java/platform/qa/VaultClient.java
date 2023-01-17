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

package platform.qa;

import lombok.extern.log4j.Log4j2;
import platform.qa.entities.Service;
import platform.qa.exception.RestApiVaultException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.RestResponse;


/**
 * Client to work with Vault service
 */
@Log4j2
public class VaultClient {
    private VaultConfig config;
    private Vault vault;
    private MountOptions mountOptions;
    private final static int GLOBAL_ENGINE_VERSION = 2;

    public VaultClient(Service service, String pathSecretEngine) {
        try {
            config = new VaultConfig()
                    .address(service.getUrl())
                    .token(service.getToken())
                    .openTimeout(5)
                    .engineVersion(GLOBAL_ENGINE_VERSION)
                    .readTimeout(30)
                    .sslConfig(new SslConfig().build())
                    .build();

            this.vault = new Vault(config);
            this.mountOptions = MountOptions
                    .builder()
                    .pathSecretEngine(pathSecretEngine)
                    .build();

        } catch (VaultException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Create secret or rewrite secret
     *
     * @param pathSecrets - path to secret
     * @param secrets     - map with some data for pathSecrets:
     *                    Info: v2 or v1, data - additional elements will be automatically added to the context path
     *                    (vault-java-driver)
     *                    Example secret "platform-integration/mdtu-ddm-edp-cicd-lowcode-dev-dev/mdtu-ddm-edp-cicd
     *                    -lowcode-dev-dev"
     */
    public void createSecreteData(String pathSecrets, Map<String, Object> secrets) {
        LogicalResponse writeResponse;

        try {
            writeResponse = this.vault.logical()
                    .write(mountOptions.getPathSecretEngine() + "/" + pathSecrets, secrets);

            checkVaultResponse(writeResponse.getRestResponse());

        } catch (VaultException ex) {
            throw new RestApiVaultException(ex);
        }
    }

    /**
     * Gets map with data secrete or empty map
     *
     * @param pathSecret - path to secret. Example, the part that needs to be sent to get the secret data:
     *                   {pathSecrets} "baseUrl/ui/vault/secrets/{pathSecrets}"
     * @return Map<String, String> from data
     */
    public Map<String, String> getDataSecrete(String pathSecret) {
        LogicalResponse writeResponse;
        try {
            writeResponse = this.vault.logical()
                    .read(mountOptions.getPathSecretEngine() + "/" + pathSecret);
            return writeResponse.getData();
        } catch (VaultException ex) {
            throw new RestApiVaultException(ex);
        }
    }

    /**
     * Enable a Secrets Engine
     */
    public void enableSecretsEngineIfNotExists() {
        try {
            vault.mounts()
                    .list()
                    .getMounts().entrySet().stream()
                    .filter(el -> el.getKey().equals(mountOptions.getPathSecretEngine() + "/"))
                    .findFirst()
                    .ifPresentOrElse(
                            el -> log.info("path for mount is already exists " + mountOptions.getPathSecretEngine()),
                            this::enableSecretsEngine);
        } catch (VaultException ex) {
            throw new RestApiVaultException(ex);
        }
    }

    public void enableSecretsEngine() {
        try {
            this.vault.mounts()
                    .enable(mountOptions.getPathSecretEngine(),
                            mountOptions.getMountType(),
                            mountOptions.getMountPayload()
                    );
            log.info(mountOptions.getPathSecretEngine() + " mount was created");
        } catch (VaultException ex) {
            throw new RestApiVaultException(ex);
        }
    }

    public VaultClient setVault(Vault vault) {
        this.vault = vault;
        return this;
    }

    public VaultClient setMountOptions(MountOptions mountOptions) {
        this.mountOptions = mountOptions;
        return this;
    }

    public MountOptions getMountOptions() {
        return mountOptions;
    }

    private void checkVaultResponse(RestResponse responseVault) {
        if (responseVault.getStatus() != 200) {
            throw new RestApiVaultException(String.format("statusCode: %s, body: %s ",
                    responseVault.getStatus(), new String(responseVault.getBody(), StandardCharsets.UTF_8)));
        }
    }

}
