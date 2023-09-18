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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import platform.qa.entities.Service;
import platform.qa.entities.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class KeycloakClient {
    private static final String MASTER_REALM_NAME = "master";
    private final Keycloak keycloak;
    private final Service keycloakService;

    public KeycloakClient(Service keycloakService) {
        this.keycloakService = keycloakService;
        var user = keycloakService.getUser();
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakService.getUrl())
                .realm(MASTER_REALM_NAME)
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("admin-cli")
                .username(user.getLogin())
                .password(user.getPassword())
                .resteasyClient(createResteasyClient())
                .build();
    }

    public IdentityProviderResource getIdentityProvider(String realm, String providerAlias) {
        return keycloak.realm(realm).identityProviders().get(providerAlias);
    }

    @Deprecated
    public void setKeycloakUserAttributes(String realmName, String userName, Map<String, List<String>> attributes) {
        UserRepresentation keyCloakUser = getKeyCloakUserByName(realmName, userName);
        keyCloakUser.setAttributes(attributes);
        keycloak.realm(realmName).users().get(keyCloakUser.getId()).update(keyCloakUser);
    }

    public UserRepresentation getKeyCloakUserByName(String realmName, String userName) {
        List<UserRepresentation> userList = keycloak.realm(realmName).users().search(userName, true);
        return (userList.isEmpty()) ? null : userList.get(0);
    }

    /**
     * Returns user Keycloak token depends on realm, client and user.
     * Use when it is impossible to get user token for client which specified to have Credentials:
     * Client Authenticator -> Client Secret
     * <p>
     *
     * @param realm realm name
     * @param user  user data {@link User}
     * @return keycloak user token
     */
    public String getAccessToken(String realm, User user) {
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(this.keycloakService.getUrl())
                .realm(realm)
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(user.getClientId())
                .username(user.getLogin())
                .password(user.getPassword())
                .clientSecret(getClientSecret(user.getRealm(), user.getClientId()))
                .resteasyClient(createResteasyClient())
                .build()) {

            return keycloak.tokenManager().getAccessTokenString();
        }
    }

    /**
     * Returns token for client
     * GrantType = OAuth2Constants.CLIENT_CREDENTIALS
     *
     * @param client (!Set User realm and clientId)
     * @return keycloak user token
     */
    public String getAccessTokenClient(User client) {
        if (client == null || client.getRealm() == null || client.getClientId() == null) {
            throw new IllegalStateException("realm and clientId - required params " + client);
        }

        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(this.keycloakService.getUrl())
                .realm(client.getRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(client.getClientId())
                .clientSecret(getClientSecret(client.getRealm(), client.getClientId()))
                .resteasyClient(createResteasyClient())
                .build()) {

            return keycloak.tokenManager().getAccessTokenString();
        }
    }

    public String getClientSecret(String realmName, String clientId) {
        List<ClientRepresentation> clients = getClientRepresentation(realmName, clientId);
        if (clients == null || clients.isEmpty() || clients.size() > 1) return null;

        String id = clients.get(0).getId();
        return keycloak.realm(realmName).clients().get(id).getSecret().getValue();
    }

    public void createRealmRole(String realmName, String roleName) {
        var count = keycloak.realm(realmName).roles().list().stream()
                .filter(role -> roleName.equals(role.getName())).count();
        if (count == 0) {
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setName(roleName);
            try {
                keycloak.realm(realmName).roles().create(roleRepresentation);
            } catch (ClientErrorException ignored) {
                //TODO add concurrent access
            }
        }
    }

    public void createUser(User user) {
        if (!user.isInitiated()) {
            enableClientDirectAccessGrants(user.getRealm(), user.getClientId());

            keycloak.realm(user.getRealm()).users().create(initUserData(user));
            createRealmRoles(user.getRealm(), user.getRealmRoles());
            setUserRealmRoles(user);
            user.setInitiated(true);
        }
    }

    public void createUser(User user, String namespace) {
        if (!user.isInitiated()) {
            enableClientDirectAccessGrants(user.getRealm(), user.getClientId());

            keycloak.realm(user.getRealm()).users().create(initUserData(user));
            List<String> realmRoles = user.getRealmRoles();

            if (user.getClientId().equals("admin-portal")) {
                IntStream.range(0, realmRoles.size())
                        .filter(i -> realmRoles.get(i).equals("cp-registry-admin"))
                        .forEach(i -> realmRoles.set(i, "cp-registry-admin-".concat(namespace)));
            }

            createRealmRoles(user.getRealm(), realmRoles);
            setUserRealmRoles(user);
            user.setInitiated(true);
        }
    }

    public void deleteUsersByPrefix(String realmName, String userPrefix) {
        var users = keycloak
                .realm(realmName)
                .users()
                .list()
                .stream()
                .filter(user -> user.getUsername().startsWith(userPrefix))
                .collect(Collectors.toList());

        users.forEach(user -> keycloak.realm(realmName).users().delete(user.getId()));
    }

    public User getUserByAttributes(String realmName, Map<String, List<String>> attributes) {
        var user = searchUserByAttributes(realmName, attributes);
        if (user == null) return null;
        return User.builder()
                .login(user.getId())
                .mail(user.getEmail())
                .attributes(user.getAttributes())
                .build();
    }

    public void deleteUserByAttributes(String realmName, Map<String, List<String>> attributes) {
        var user = searchUserByAttributes(realmName, attributes);
        if (user == null) return;
        keycloak.realm(realmName).users().delete(user.getId());

    }

    public List<User> getAllUsersFromRealm(String realmName) {
        List<UserRepresentation> userRepresentationList = this.keycloak.realm(realmName).users().list();
        List<User> userList = new ArrayList<>();
        User user;

        for (UserRepresentation userRepresentation : userRepresentationList) {
            user = User.builder()
                    .login(userRepresentation.getId())
                    .mail(userRepresentation.getEmail())
                    .attributes(userRepresentation.getAttributes())
                    .build();
            userList.add(user);
        }

        return userList;
    }

    public void deleteAllUsersInRealm(String realmName) {
        var users = new ArrayList<>(keycloak
                .realm(realmName)
                .users()
                .list());

        users.forEach(user -> keycloak.realm(realmName).users().delete(user.getId()));
    }

    public List<String> getRealmRolesForUser(String realm, String keycloakUserId) {
        var realmRoles = keycloak.realm(realm).users().get(keycloakUserId).roles().realmLevel().listAll();
        return realmRoles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }

    public void removeUserRealmRole(User user, String role) {
        var userRoles = user.getRealmRoles();
        userRoles.removeIf(x->x.equals(role));
        user.setRealmRoles(userRoles);

        UserRepresentation keyCloakUser = getKeyCloakUserByName(user.getRealm(), user.getLogin());
        List<RoleRepresentation> realmRolesRep = getRealmRoleRepresentations(user.getRealm(), user.getRealmRoles());
        assignRealmRoleToUser(user, keyCloakUser, realmRolesRep);
    }

    public void removeAssignedUserRealmRoles(User user, List<String> roles) {
        UserRepresentation keyCloakUser = getKeyCloakUserByName(user.getRealm(), user.getLogin());

        List<RoleRepresentation> realmRolesRep = getRealmRoleRepresentations(user.getRealm(), roles);
        removeRealmRoleFromUser(user, keyCloakUser, realmRolesRep);
    }

    private UserRepresentation searchUserByAttributes(String realmName, Map<String, List<String>> attributes) {
        List<UserRepresentation> userResource = this.keycloak.realm(realmName).users().list();
        return userResource.stream()
                .filter(usr -> usr.getAttributes() != null)
                .filter(usr -> usr.getAttributes().keySet().containsAll(attributes.keySet())
                        && usr.getAttributes().entrySet().containsAll(attributes.entrySet()))
                .findFirst().orElse(null);
    }

    private void setUserRealmRoles(User user) {
        UserRepresentation keyCloakUser = getKeyCloakUserByName(user.getRealm(), user.getLogin());
        List<RoleRepresentation> realmRolesRep = getRealmRoleRepresentations(user.getRealm(), user.getRealmRoles());
        assignRealmRoleToUser(user, keyCloakUser, realmRolesRep);
    }

    private void assignRealmRoleToUser(User user, UserRepresentation keyCloakUser,
                                       List<RoleRepresentation> realmRolesRep) {
        await("Assign role for user")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .ignoreExceptionsInstanceOf(WebApplicationException.class)
                .untilAsserted(() -> {
                    keycloak.realm(user.getRealm()).users().get(keyCloakUser.getId()).roles().realmLevel().add(realmRolesRep);

                    assertThat(isRoleAssignedForUser(user, keyCloakUser, realmRolesRep))
                            .as("Roles wasn't assigned for user: " + realmRolesRep)
                            .isTrue();
                });
    }

    private void removeRealmRoleFromUser(User user, UserRepresentation keyCloakUser,
                                       List<RoleRepresentation> realmRolesRep) {
        await("Remove role for user")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .ignoreExceptionsInstanceOf(WebApplicationException.class)
                .untilAsserted(() -> {
                    keycloak.realm(user.getRealm()).users().get(keyCloakUser.getId()).roles().realmLevel().remove(realmRolesRep);

                    assertThat(isRoleAssignedForUser(user, keyCloakUser, realmRolesRep))
                            .as("Roles wasn't removed for user: " + realmRolesRep)
                            .isFalse();
                });
    }

    private boolean isRoleAssignedForUser(User user, UserRepresentation keyCloakUser,
                                          List<RoleRepresentation> realmRoles) {
        var assignedRoles = getRealmRolesForUser(user.getRealm(), keyCloakUser.getId());
        var expectedRoles = realmRoles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());

        return assignedRoles.containsAll(expectedRoles);
    }

    private void createRealmRoles(String realm, List<String> roles) {
        if (roles == null || roles.isEmpty()) return;
        roles.forEach(realmRole -> createRealmRole(realm, realmRole));
    }

    private void enableClientDirectAccessGrants(String realmName, String clientId) {
        if (clientId == null) return;
        var clientRepresentations = getClientRepresentation(realmName, clientId);
        for (ClientRepresentation clientRepresentation : clientRepresentations) {
            if (!clientRepresentation.isDirectAccessGrantsEnabled()) {
                clientRepresentation.setDirectAccessGrantsEnabled(true);
                try {
                    keycloak.realm(realmName).clients().get(clientRepresentation.getId()).update(clientRepresentation);
                } catch (ClientErrorException | ServerErrorException ignored) {
                    ignored.printStackTrace();
                }
            }
        }
    }

    private List<ClientRepresentation> getClientRepresentation(String realmName, String clientId) {
        return keycloak.realm(realmName).clients().findByClientId(clientId);
    }

    private List<RoleRepresentation> getRealmRoleRepresentations(String realmName, List<String> rolesToAdd) {
        if (realmName == null || realmName.isEmpty()) return new ArrayList<>();
        if (rolesToAdd == null || rolesToAdd.isEmpty()) return new ArrayList<>();

        List<RoleRepresentation> list = new ArrayList<>();
        rolesToAdd.forEach(role -> list.add(keycloak.realm(realmName).roles().get(role).toRepresentation()));

        return list;
    }

    private UserRepresentation initUserData(User user) {
        CredentialRepresentation credentialRepresentation = initCredentialData(user);
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(user.getLogin());
        userRepresentation.setFirstName(user.getLogin());
        userRepresentation.setLastName(user.getLogin());
        userRepresentation.setEmail(user.getMail() != null ? user.getMail() : user.getLogin() + "@epam.com");
        userRepresentation.setEnabled(true);
        userRepresentation.setCredentials(
                singletonList(credentialRepresentation));
        userRepresentation.setGroups(user.getGroups());
        userRepresentation.setAttributes(user.getAttributes());
        return userRepresentation;
    }

    private CredentialRepresentation initCredentialData(User user) {
        CredentialRepresentation credentialRepresentation = new
                CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(user.getPassword());
        credentialRepresentation.setTemporary(false);
        return credentialRepresentation;
    }

    private ResteasyClient createResteasyClient() {
        return new ResteasyClientBuilderImpl().register(KeycloakLoggingFilter.class).connectionPoolSize(10).build();
    }
}
