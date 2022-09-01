package platform.qa.openshift;

import org.junit.jupiter.api.Test;
import platform.qa.extension.SocketAnalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SocketAnalyzerTest {
    SocketAnalyzer socketAnalyzer = mock(SocketAnalyzer.class);
    int availablePortResponse = 8080;

    @Test
    public void getAvailablePortTest() {
        when(socketAnalyzer.getAvailablePort()).thenReturn(availablePortResponse);

        var availablePort = socketAnalyzer.getAvailablePort();

        assertEquals(availablePortResponse, availablePort);
    }
}
