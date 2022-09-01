package platform.qa.protocols;

import org.junit.jupiter.api.Test;
import platform.qa.rest.RestApiClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class RestApiClientTest {
    RestApiClient restApiClient = mock(RestApiClient.class);

    @Test
    public void addBusinessProcessIdTest() {
        when(restApiClient.addBusinessProcessId(anyString())).thenReturn(restApiClient);

        var businessProcessId = restApiClient.addBusinessProcessId("test");

        assertEquals(restApiClient, businessProcessId);
    }
}
