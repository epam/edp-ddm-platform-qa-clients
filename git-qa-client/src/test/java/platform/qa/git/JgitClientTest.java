package platform.qa.git;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JgitClientTest {
    JgitClient jgitClient = mock(JgitClient.class);
    List<String> filesFromFolderResponse = List.of(
            "file1", "file2", "file3"
    );
    Git gerritLocal;
    Git gerritTestDataLocal;

    @Test
    public void getFilesFromFolderTest() {
        when(jgitClient.getFilesFromFolder(anyString())).thenReturn(filesFromFolderResponse);

        var filesFromFolder = jgitClient.getFilesFromFolder("test");

        assertEquals(filesFromFolderResponse, filesFromFolder);
    }

    @Test
    public void submitChangeTest() {
        when(jgitClient.submitChange(gerritLocal, gerritTestDataLocal)).thenReturn("test");

        var submitChange = jgitClient.submitChange(gerritLocal, gerritTestDataLocal);

        assertEquals("test",submitChange);
    }
}
