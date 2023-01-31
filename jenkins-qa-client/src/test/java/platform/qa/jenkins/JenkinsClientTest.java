package platform.qa.jenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

public class JenkinsClientTest {
    JenkinsClient jenkinsClient = mock(JenkinsClient.class);
    long startJobResponse = 123456789;

    @Test
    public void startJobTest() {
        when(jenkinsClient.startJob(anyString())).thenReturn(startJobResponse);

        var startJob = jenkinsClient.startJob("test");

        assertEquals(startJobResponse, startJob);
    }
}
