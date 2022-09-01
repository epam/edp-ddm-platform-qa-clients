package platform.qa.database;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TableInfoDbTest {
    TableInfoDb tableInfoDb = mock(TableInfoDb.class);
    List<String> allTablesFromRegistrySchemeResponse = List.of(
            "table1", "table2", "table3"
    );

    @Test
    public void getAllTablesFromRegistrySchemeTest() throws SQLException {
        when(tableInfoDb.getAllTablesFromRegistryScheme()).thenReturn(allTablesFromRegistrySchemeResponse);

        var allTablesFromRegistryScheme = tableInfoDb.getAllTablesFromRegistryScheme();

        assertEquals(allTablesFromRegistrySchemeResponse, allTablesFromRegistryScheme);
    }

    @Test
    public void getAllTablesFromRegistrySchemeExceptionTest() throws SQLException {
        when(tableInfoDb.getAllTablesFromRegistryScheme()).thenThrow(SQLException.class);

        assertThatThrownBy(() -> {
            tableInfoDb.getAllTablesFromRegistryScheme();
        }).isInstanceOf(SQLException.class)
                ;
    }
}
