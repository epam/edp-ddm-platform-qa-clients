package platform.qa.ceph;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CephClientTest {
    CephClient cephClient = mock(CephClient.class);
    List<String> listOfFilesFromBucketResponse = List.of(
            "file1", "file2", "file3"
    );

    @Test
    public void getListOfFilesFromBucketTest() {
        when(cephClient.getListOfFilesFromBucket(anyString())).thenReturn(listOfFilesFromBucketResponse);

        var listOfFilesFromBucket = cephClient.getListOfFilesFromBucket("test");

        assertTrue(listOfFilesFromBucket.size() == 3);
        Assertions.assertEquals(listOfFilesFromBucketResponse, listOfFilesFromBucket);
    }

    @Test
    public void isBucketExistsTest() {
        when(cephClient.isBucketExists(anyString())).thenReturn(true);

        var isBucketExist = cephClient.isBucketExists("test");

        assertTrue(isBucketExist);
    }
}
