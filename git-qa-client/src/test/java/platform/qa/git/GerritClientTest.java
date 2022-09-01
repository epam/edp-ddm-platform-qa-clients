package platform.qa.git;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class GerritClientTest {
    GerritClient gerritClient = mock(GerritClient.class);
    Map<String, Object> repositoryResponse = Map.of(
            "repository1", "value1",
            "repository2", "value2",
            "repository3", "value3"
    );

    @Test
    public void getRepositoryTest() {
        when(gerritClient.getRepository(anyString())).thenReturn(repositoryResponse);

        var repository = gerritClient.getRepository("test");

        assertEquals(repositoryResponse, repository);
    }

    @Test
    public void deployChangeTest() {
        doNothing().when(gerritClient).deployChange(anyString());

        gerritClient.deployChange("test");

        verify(gerritClient, times(1)).deployChange("test");
    }
}
