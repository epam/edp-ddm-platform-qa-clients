package platform.qa.keycloak;

import org.junit.jupiter.api.Test;
import platform.qa.entities.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KeycloakClientTest {
    KeycloakClient keycloakClient = mock(KeycloakClient.class);
    String response = "response";
    User userData = User
            .builder()
            .login("test")
            .password("test")
            .build();

    @Test
    public void getClientSecretTest() {
        when(keycloakClient.getClientSecret(anyString(), anyString())).thenReturn(response);

        var clientSecret = keycloakClient.getClientSecret("test", "test");

        assertEquals(response, clientSecret);
    }

    @Test
    public void getAccessTokenClientTest() {
        when(keycloakClient.getAccessTokenClient(userData)).thenReturn(response);

        var accessTokenClient = keycloakClient.getAccessTokenClient(userData);

        assertEquals(response, accessTokenClient);
    }
}
