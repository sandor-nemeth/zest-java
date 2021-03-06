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
package org.apache.zest.index.rdf.assembly;

import org.apache.zest.api.common.Visibility;
import org.apache.zest.api.value.ValueSerialization;
import org.apache.zest.bootstrap.Assembler;
import org.apache.zest.bootstrap.AssemblyException;
import org.apache.zest.bootstrap.ModuleAssembly;
import org.apache.zest.index.rdf.RdfIndexingEngineService;
import org.apache.zest.index.rdf.query.RdfQueryParserFactory;
import org.apache.zest.library.rdf.entity.EntityStateSerializer;
import org.apache.zest.library.rdf.entity.EntityTypeSerializer;
import org.apache.zest.library.rdf.repository.RdbmsRepositoryService;
import org.apache.zest.valueserialization.orgjson.OrgJsonValueSerializationService;

public class RdfRdbmsSesameStoreAssembler
    implements Assembler
{
    private Visibility indexingVisibility;
    private Visibility repositoryVisibility;

    public RdfRdbmsSesameStoreAssembler()
    {
        this( Visibility.application, Visibility.module );
    }

    public RdfRdbmsSesameStoreAssembler(
                                         Visibility indexingVisibility,
                                         Visibility repositoryVisibility
    )
    {
        this.indexingVisibility = indexingVisibility;
        this.repositoryVisibility = repositoryVisibility;
    }

    @Override
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        module.services( RdbmsRepositoryService.class )
            .visibleIn( repositoryVisibility )
            .instantiateOnStartup()
            .identifiedBy( "rdf-indexing" );
        module.services( RdfIndexingEngineService.class )
            .visibleIn( indexingVisibility )
            .instantiateOnStartup();
        module.services( RdfQueryParserFactory.class ).visibleIn( indexingVisibility );
        module.services( OrgJsonValueSerializationService.class ).taggedWith( ValueSerialization.Formats.JSON );
        module.objects( EntityStateSerializer.class, EntityTypeSerializer.class );
    }
}