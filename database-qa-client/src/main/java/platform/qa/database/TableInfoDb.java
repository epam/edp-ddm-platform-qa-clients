/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.qa.database;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import platform.qa.entities.WaitConfiguration;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.lang3.BooleanUtils;
import org.assertj.core.util.Lists;
import com.jcabi.jdbc.JdbcSession;

/**
 * Class to implement common approach to get data from database tables
 */
@Log4j2
public class TableInfoDb {
    protected final DataSource source;
    @Setter
    private WaitConfiguration waitConfiguration;

    public TableInfoDb(DataSource source) {
        waitConfiguration = WaitConfiguration
                .newBuilder()
                .setPoolIntervalTimeout(1)
                .setWaitTimeout(2)
                .build();
        this.source = source;
    }

    @SneakyThrows
    protected String waitAndGetEntity(String query, boolean toBeEmpty) {
        final List<String> list = Lists.newArrayList();

        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())
                .untilAsserted(() -> {
                    list.addAll(getValues(query));

                    assertThat(toBeEmpty).as("Waiting condition is not reached:").isEqualTo(list.isEmpty());
                });

        return list.isEmpty() ? null : list.get(0);

    }

    @SneakyThrows
    private List<String> getValues(String query) {
        List<String> list = Lists.newArrayList();
        return new JdbcSession(source)
                .sql(query).select((resultSet, statement) -> {
                    while (resultSet.next()) {
                        list.add(resultSet.getString(1));
                    }
                    return list;
                });

    }

    @SneakyThrows
    protected <T> List<T> waitAndGetEntity(String query, Class<T> clazz, boolean toBeEmpty) {
        final List<T> list = Lists.newArrayList();
        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())
                .untilAsserted(() -> {
                    list.addAll(getValues(query, clazz));

                    assertThat(toBeEmpty).as("Waiting condition is not reached:").isEqualTo(list.isEmpty());
                });
        return list;
    }

    @SneakyThrows
    protected <T> List<T> waitAndGetEntity(String query, Class<T> clazz) {
        final List<T> list = Lists.newArrayList();
        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())
                .untilAsserted(() -> {
                    boolean result = list.addAll(getValues(query, clazz));
                    assertThat(result).as("No items in database!").isNotNull();
                });
        return list;
    }

    @SneakyThrows
    private <T> List<T> getValues(String query, Class<T> clazz) {
        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        for (Field field : fields) {
            field.setAccessible(true);
        }
        List<T> list = Lists.newArrayList();
        return new JdbcSession(source)
                .sql(query).select((resultSet, statement) -> {
                    while (resultSet.next()) {
                        T dto = null;
                        try {
                            dto = clazz.getConstructor().newInstance();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        for (Field field : fields) {
                            String name = field.getName();

                            try {
                                String value = resultSet.getString(name);

                                if (field.getType().getName().equals("boolean")) {
                                    field.set(dto, resultSet.getBoolean(name));
                                } else {
                                    field.set(dto, field.getType().getConstructor(String.class).newInstance(value));
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }

                        list.add(dto);

                    }
                    return list;
                });


    }

    public List<String> getAllTablesFromRegistryScheme() throws SQLException {
        var query = "select table_name from information_schema.\"tables\" where table_schema = 'registry'";

        return new JdbcSession(source)
                .sql(query)
                .select(
                        (resultSet, statement) -> {
                            var names = new ArrayList<String>();
                            while (resultSet.next()) {
                                names.add(resultSet.getString(1));
                            }
                            return names;
                        }
                );
    }

    public List<String> getAllTablesNamesFromPublicScheme() throws SQLException {
        log.info("Перевірка відповідності створених таблиць");
        var query = "select table_name from information_schema.\"tables\" where table_schema = 'public'";

        return new JdbcSession(source)
                .sql(query)
                .select(
                        (resultSet, statement) -> {
                            var names = new ArrayList<String>();
                            while (resultSet.next()) {
                                names.add(resultSet.getString(1));
                            }
                            return names;
                        }
                );
    }


    public void createCustomTable(String tableName) throws SQLException {
        log.info("Створення таблиці " + tableName);
        var query = "CREATE TABLE " + tableName + " (\n" +
                "    PersonID int,\n" +
                "    LastName varchar(255),\n" +
                "    FirstName varchar(255),\n" +
                "    Address varchar(255),\n" +
                "    City varchar(255)\n" +
                ");";

        new JdbcSession(source)
                .sql(query)
                .execute();
    }

    public void dropCustomTable(String tableName) throws SQLException {
        log.info("Видалення таблиці " + tableName);
        var query = "DROP TABLE " + tableName;

        new JdbcSession(source)
                .sql(query)
                .execute();
    }

    public void createRoleWithPermission(String roleName) throws SQLException {
        log.info("Створення ролі");
        var query = "create user " + roleName + " with encrypted password 'qwerty'";
        var query2 = "grant all privileges on database registry to " + roleName;
        new JdbcSession(source)
                .sql(query)
                .execute();

        new JdbcSession(source)
                .sql(query2)
                .execute();
    }

    public HashMap<String, HashMap<String, String>> getAllColumnsForSpecificTables(List<String> tableNames) throws SQLException {
        log.info("Перевірка відповідності створених колонок");
        String query = "SELECT column_name , data_type FROM information_schema.columns " +
                "WHERE table_name = '%s'";

        HashMap<String, HashMap<String, String>> map = new HashMap<>();
        for (var table : tableNames) {
            var columns = new JdbcSession(source)
                    .sql(String.format(query, table))
                    .select((resultSet, statement) -> {
                                var actualMap = new HashMap<String, String>();
                                while (resultSet.next()) {
                                    actualMap.put(resultSet.getString(1), resultSet.getString(2));
                                }
                                return actualMap;
                            }
                    );
            map.put(table, columns);
        }
        return map;
    }

    public HashMap<String, List<String>> getAllConstraintsForSpecificTables(List<String> tableNames) throws SQLException {
        log.info("Перевірка відповідності створених обмежень");
        String query = "select check_clause from information_schema.table_constraints c " +
                "join information_schema.check_constraints cc on c.constraint_name = cc.constraint_name" +
                " where c.constraint_type = 'CHECK' and c.table_name = '%s'";

        HashMap<String, List<String>> map = new HashMap<>();

        for (String table : tableNames) {

            var constraints = new JdbcSession(source)
                    .sql(String.format(query, table))
                    .select((resultSet, statement) -> {
                        var actualList = new ArrayList<String>();
                        while (resultSet.next()) {
                            actualList.add(resultSet.getString(1));
                        }
                        return actualList;
                    });
            map.put(table, constraints);
        }
        return map;

    }


    public HashMap<String, String> getPrimaryKeys() throws SQLException {
        log.info("Перевірка відповідності primary key");
        var actualMap = new HashMap<String, String>();
        String query = "SELECT KU.table_name, column_name FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS AS TC" +
                " INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS KU ON TC.CONSTRAINT_NAME = KU.CONSTRAINT_NAME" +
                " where constraint_type = 'PRIMARY KEY' and KU.table_name like 'pd%'";

        return new JdbcSession(source)
                .sql(query)
                .select((resultSet, statement) -> {
                            while (resultSet.next()) {
                                actualMap.put(resultSet.getString(1), resultSet.getString(2));
                            }
                            return actualMap;
                        }
                );
    }

    public long getCountsOfRowsInTable(String tableName) {
        log.info(String.format("Отримання кількості записів в таблиці БД: %s за виключенням авто-згенерованих значень", tableName));
        String query = "SELECT COUNT(*) FROM " + tableName + " WHERE ddm_created_by ='admin'";
        return Long.parseLong(waitAndGetEntity(query, false));
    }

    public boolean isTableExistsInSchema(String tableName, String schemaName) {
        log.info("Перевiрка що таблиця iснуе у схемi");
        String query = String.format("SELECT EXISTS (\n" +
                        "   SELECT FROM pg_tables\n" +
                        "   WHERE  schemaname = $$%s$$\n" +
                        "   AND    tablename  = $$%s$$\n" +
                        ");",
                schemaName,
                tableName
        );

        return BooleanUtils.toBoolean(waitAndGetEntity(query, false));
    }
}