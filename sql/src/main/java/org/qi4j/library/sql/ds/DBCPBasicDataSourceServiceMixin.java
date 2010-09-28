/*
 * Copyright (c) 2010, Paul Merlin. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.qi4j.library.sql.ds;

import org.apache.commons.dbcp.BasicDataSource;
import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.injection.scope.This;

/**
 * @author Stanislav Muhametsin
 * @author Paul Merlin
 */
public abstract class DBCPBasicDataSourceServiceMixin
    implements DataSourceServiceComposite
{

    @This
    private Configuration<DataSourceConfiguration> configuration;

    private BasicDataSource dataSource;

    public BasicDataSource getDataSource()
    {
        return dataSource;
    }

    public void activate()
        throws Exception
    {
        System.out.println( "ACTIVATE" );
        dataSource = new BasicDataSource();
        dataSource.setUrl( configuration.configuration().additionalInfo().get() );
    }

    public void passivate()
        throws Exception
    {
        System.out.println( "PASSIVATE" );
        dataSource.close();
    }

}
