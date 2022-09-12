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

package platform.qa.oc;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.CatalogSource;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import jodd.util.Base64;
import lombok.Getter;
import lombok.SneakyThrows;
import platform.qa.entities.Service;
import platform.qa.entities.User;
import platform.qa.extension.SocketAnalyzer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Client to work with Openshift
 */
public class OkdClient {

    @Getter
    private OpenShiftClient osClient;

    @Deprecated
    public OkdClient(String namespace) {
        if (StringUtils.isEmpty(namespace))
            throw new RuntimeException("Openshift namespace is NOT defined in your property file:");

        osClient = new DefaultOpenShiftClient()
                .inNamespace(namespace);

        List<String> nsList = osClient.projects().list().getItems().stream()
                .map(a -> a.getMetadata().getName()).collect(Collectors.toList());
        if (!nsList.contains(namespace)) {
            throw new RuntimeException("Not found OpenShift namespace with name: \n" + namespace + "\n from the list:"
                    + " " + nsList);
        }

    }

    public OkdClient(Service service, String namespace) {
        if (StringUtils.isEmpty(namespace))
            throw new RuntimeException("Openshift namespace is NOT set!");

        var configBuilder = new ConfigBuilder()
                .withUsername(service.getUser().getLogin())
                .withTrustCerts(true)
                .withMasterUrl(service.getUrl());
        var config = getOkdClientWithPassword(service, configBuilder);

        osClient = new DefaultOpenShiftClient(config)
                .inNamespace(namespace);
    }

    public void setOsClientNamespace(String namespace) {
        osClient = ((DefaultOpenShiftClient) osClient).inNamespace(namespace);
    }

    public OkdClient(Service service) {
        var configBuilder = new ConfigBuilder()
                .withUsername(service.getUser().getLogin())
                .withTrustCerts(true)
                .withMasterUrl(service.getUrl());
        var config = getOkdClientWithPassword(service, configBuilder);

        osClient = new DefaultOpenShiftClient(config).inAnyNamespace();
    }

    public PodList getPodList(String namespace) {
        return ((DefaultOpenShiftClient) osClient)
                .inNamespace(namespace)
                .pods().list();
    }

    public List<Pod> getPodsWithNoJobOwner(String namespace) {
        return getPodList(namespace).getItems()
                .stream()
                .filter(pod -> !pod.getMetadata().getOwnerReferences()
                        .stream().map(OwnerReference::getKind).collect(Collectors.toList())
                        .contains("Job"))
                .collect(Collectors.toList());
    }

    public List<PersistentVolumeClaim> getPersistentVolumeClaims(String namespace) {
        return ((DefaultOpenShiftClient) osClient)
                .inNamespace(namespace)
                .persistentVolumeClaims().list()
                .getItems();
    }

    @SneakyThrows
    public List<Pod> getCephObjectStores(String namespace) {
        String path = "apis/ceph.rook.io/v1/namespaces/" + namespace + "/cephobjectstores";
        String httpResponse = getHttpResponse(namespace, path);
        return new ObjectMapper().readValue(httpResponse, PodList.class).getItems();
    }

    @SneakyThrows
    public List<Pod> getBackupStorageLocations(String namespace) {
        String path = "apis/velero.io/v1/namespaces/" + namespace + "/backupstoragelocations";
        String httpResponse = getHttpResponse(namespace, path);
        return new ObjectMapper().readValue(httpResponse, PodList.class).getItems();
    }

    public Map<String, String> getConfigurationMap(String configMapName) {
        var list = osClient.configMaps().list().getItems();
        var metadata = list.parallelStream()
                .filter(configuration -> configuration.getMetadata().getName().equals(configMapName))
                .findFirst();

        return metadata.isPresent() ? metadata.get().getData() : new HashMap<>();
    }

    public HashMap<String, String> getOkdRoutes() {

        var list = osClient.routes().list().getItems();

        HashMap<String, String> mapRoutes = new HashMap<>();

        for (var route : list) {
            var key = route.getMetadata().getName();
            var value = (route.getSpec().getPath() != null) ?
                    (route.getSpec().getHost() + route.getSpec().getPath()) :
                    route.getSpec().getHost();
            mapRoutes.put(key, "https://" + value);
        }

        return mapRoutes;
    }

    public User getCredentials(String secretName) {
        var secret = osClient.secrets().withName(secretName).get().getData();
        var userKey = secret.keySet().stream().filter(a -> a.contains("user")).findFirst().orElse(null);
        var user = Base64.decodeToString(secret.get(userKey));
        var pwd = Base64.decodeToString(secret.get("password"));
        return new User(user, pwd);
    }

    public String getTokenVault(String secretName) {
        var secret = osClient.secrets().withName(secretName).get();
        String errorMessage = "token not found for secret " + secretName;
        if (secret == null) {
            throw new IllegalStateException(errorMessage);
        }
        String token = secret.getData()
                .entrySet().stream()
                .filter(el -> el.getKey().contains("TOKEN") && el.getValue() != null)
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(errorMessage));

        return Base64.decodeToString(token);
    }

    public List<Boolean> getPodStatusByLabel(String podLabels) {
        return osClient.pods().withLabel(podLabels)
                .list().getItems().get(0).getStatus().getContainerStatuses().stream()
                .map(ContainerStatus::getReady)
                .collect(Collectors.toList());
    }

    public Pod getPodByName(String name) {
        return osClient.pods().withName(name).get();
    }

    public List<CatalogSource> getClusterSources() {
        return osClient.operatorHub().catalogSources().list().getItems();
    }

    @SneakyThrows
    public List<Pod> getCustomResourceDefinitions(String namespace, String definitionName) {
        String path = "apis/v1.edp.epam.com/v1alpha1/namespaces/" + namespace + "/" + definitionName + "/";
        String httpResponse = getHttpResponse(namespace, path);
        return new ObjectMapper().readValue(httpResponse, PodList.class).getItems();
    }

    public Namespace getNamespaceByName(String name) {
        return osClient.namespaces().withName(name).get();
    }

    @SneakyThrows
    public int performPortForwarding(String podLabel, String route, int defaultPort) {
        String podName = osClient
                .pods()
                //label can be found in Networking -> Services -> Pod Selector column
                .withLabel(podLabel)
                .list()
                .getItems()
                .stream()
                //route it's part of pod name. Ex "bpms"
                .filter(x -> x.getMetadata().getName().contains(route))
                .collect(Collectors.toList())
                .get(0)
                .getMetadata()
                .getName();

        int port = new SocketAnalyzer().getAvailablePort();

        osClient
                .pods()
                .withName(podName)
                //default port can be found in Networking -> Services -> "Service name" -> "Service port mapping"
                .portForward(defaultPort, port);
        return port;
    }

    public Map<String, String> getSecretsByName(String secretName) {
        return osClient.secrets().withName(secretName).get().getData();
    }

    private String getHttpResponse(String namespace, String path) throws IOException {
        NamespacedOpenShiftClient openShiftClient = ((DefaultOpenShiftClient) osClient)
                .inNamespace(namespace);
        HttpClient httpClient = openShiftClient
                .getHttpClient();
        HttpRequest httpRequest = httpClient.newHttpRequestBuilder()
                .uri(openShiftClient.getMasterUrl() + path)
                .build();

        return httpClient.send(httpRequest, String.class).body();
    }

    private Config getOkdClientWithPassword(Service service, ConfigBuilder configBuilder) {
        if (service.getUser().getLogin().contains("service")) {
            return configBuilder.withOauthToken(service.getUser().getPassword()).build();
        } else {
            return configBuilder.withPassword(service.getUser().getPassword()).build();
        }
    }
}
