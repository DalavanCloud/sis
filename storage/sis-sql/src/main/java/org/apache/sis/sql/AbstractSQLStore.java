/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.sql;

import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.sql.reverse.RelationMetaModel;
import org.apache.sis.sql.dialect.SQLDialect;
import org.apache.sis.storage.DataStore;
import org.opengis.feature.AttributeType;

/**
 * Parent store class for DataStore implementation using java JDBC backend.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public abstract class AbstractSQLStore extends DataStore {

    /**
     * Returns the dialect configuration used by this store.
     *
     * @return dialect used by the store, never null
     */
    public abstract SQLDialect getDialect();

    public abstract Logger getLogger();

    public abstract DataSource getDataSource();

    public abstract String getDatabaseSchema();

    public abstract String getDatabaseTable();

}
