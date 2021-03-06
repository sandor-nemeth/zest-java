/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package org.apache.zest.manual.recipes.createEntity;

import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.query.Query;
import org.apache.zest.api.query.QueryBuilder;
import org.apache.zest.api.structure.Module;
import org.apache.zest.api.unitofwork.UnitOfWork;
import org.apache.zest.api.unitofwork.UnitOfWorkFactory;

import static org.apache.zest.api.query.QueryExpressions.eq;
import static org.apache.zest.api.query.QueryExpressions.templateFor;

// START SNIPPET: repo
public class ManufacturerRepositoryMixin
        implements ManufacturerRepository
{
    @Structure
    private UnitOfWorkFactory uowf;

    @Structure
    private Module module;

    public Manufacturer findByIdentity( String identity )
    {
        UnitOfWork uow = uowf.currentUnitOfWork();
        return uow.get(Manufacturer.class, identity);
    }

    public Manufacturer findByName( String name )
    {
        UnitOfWork uow = uowf.currentUnitOfWork();
        QueryBuilder<Manufacturer> builder =
                module.newQueryBuilder( Manufacturer.class );

        Manufacturer template = templateFor( Manufacturer.class );
        builder.where( eq( template.name(), name ) );

        Query<Manufacturer> query = uow.newQuery( builder);
        return query.find();
    }
}

// END SNIPPET: repo