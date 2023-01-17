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

import platform.qa.entities.Db;
import platform.qa.entities.User;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import com.jcabi.aspects.Cacheable;


/**
 * DataSource to get database connection
 */
public final class DataSourceInitializer {
    private DataSourceInitializer() {
        throw new IllegalStateException("This is utility class!");
    }

    @Cacheable(forever = true)
    public static DataSource getSource(Db db, String schema) {
        PGSimpleDataSource src = new PGSimpleDataSource();

        src.setURL(db.getUrl().concat(schema));
        src.setUser(db.getUser());
        src.setPassword(db.getPassword());
        return src;
    }

    @Cacheable(forever = true)
    public static DataSource getSource(Db db, User user, String schema) {
        PGSimpleDataSource src = new PGSimpleDataSource();

        src.setURL(db.getUrl().concat(schema));
        src.setUser(user.getLogin());
        src.setPassword(user.getPassword());
        return src;
    }
}
