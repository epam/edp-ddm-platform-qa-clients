package platform.qa.jenkins;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

public class JenkinsRestClientTest {
    JenkinsRestClient jenkinsRestClient = mock(JenkinsRestClient.class);
    Map<String, String> params = Map.of(
            "param1", "value1",
            "param2", "value2"
    );
    Map<String, String> inputRequest = Map.of(
            "inputRequest1", "value1",
            "inputRequest2", "value2"
    );

    @Test
    public void startJobWithInputRequestTest() {
        doNothing().when(jenkinsRestClient).startJobWithInputRequest("test", "test", params, inputRequest);

        jenkinsRestClient.startJobWithInputRequest("test", "test", params, inputRequest);

        verify(jenkinsRestClient, times(1)).startJobWithInputRequest("test",
                "test", params, inputRequest);
    }
}
