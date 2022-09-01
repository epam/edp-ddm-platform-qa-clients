package platform.qa.vault;

import org.junit.jupiter.api.Test;
import platform.qa.VaultClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class VaultClientTest {
    VaultClient vaultClient = mock(VaultClient.class);
    Map<String, String> dataSecretResponse = Map.of(
            "secret1", "data1",
            "secret2", "data2",
            "secret3", "data3"
    );
    Map<String, Object> secrets = Map.of(
            "secret1", "data1",
            "secret2", "data2",
            "secret3", "data3"
    );

    @Test
    public void getDataSecretTest() {
        when(vaultClient.getDataSecrete(anyString())).thenReturn(dataSecretResponse);

        var dataSecret = vaultClient.getDataSecrete("test");

        assertEquals(dataSecretResponse, dataSecret);
    }

    @Test
    public void createSecreteDataTest() {
        doNothing().when(vaultClient).createSecreteData(anyString(), anyObject());

        vaultClient.createSecreteData("test", secrets);

        verify(vaultClient, times(1)).createSecreteData("test", secrets);
    }
}
