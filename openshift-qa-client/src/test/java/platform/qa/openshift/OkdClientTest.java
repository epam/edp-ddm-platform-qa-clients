package platform.qa.openshift;

import org.junit.jupiter.api.Test;
import platform.qa.oc.OkdClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OkdClientTest {
    OkdClient okdClientTest = mock(OkdClient.class);
    List<Boolean> podStatusByLabelResponse = List.of(
            true, false
    );
    Map<String, String> secretsByNameResponse = Map.of(
            "secret1", "value1",
            "secret2", "value2",
            "secret3", "value3"
    );

    @Test
    public void getPodStatusByLabelTest() {
        when(okdClientTest.getPodStatusByLabel(anyString())).thenReturn(podStatusByLabelResponse);

        var podStatusByLabel = okdClientTest.getPodStatusByLabel("test");

        assertEquals(podStatusByLabelResponse, podStatusByLabel);
    }

    @Test
    public void getSecretsByNameTest(){
        when(okdClientTest.getSecretsByName(anyString())).thenReturn(secretsByNameResponse);

        var secretsByName = okdClientTest.getSecretsByName("test");

        assertEquals(secretsByNameResponse, secretsByName);
    }
}
