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

package org.apache.zest.library.rest.server.api;

import java.time.Instant;
import org.apache.zest.api.entity.EntityComposite;
import org.apache.zest.api.unitofwork.NoSuchEntityException;
import org.apache.zest.api.unitofwork.UnitOfWork;
import org.apache.zest.spi.ZestSPI;
import org.apache.zest.spi.entity.EntityState;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.resource.ResourceException;

/**
 * JAVADOC
 */
class ResourceValidity
{
    private EntityComposite entity;
    private final ZestSPI spi;
    private Request request;

    ResourceValidity( EntityComposite entity, ZestSPI spi, Request request )
    {
        this.entity = entity;
        this.spi = spi;
        this.request = request;
    }

    void updateEntity( UnitOfWork current )
    {
        try
        {
            entity = current.get( entity );
        }
        catch( NoSuchEntityException e )
        {
            // Entity was deleted
            entity = null;
        }
    }

    void updateResponse( Response response )
    {
        if( entity != null )
        {
            EntityState state = spi.entityStateOf( entity );
            Tag tag = new Tag( state.identity().identity() + "/" + state.version() );
            response.getEntity().setModificationDate( new java.util.Date( state.lastModified() ) );
            response.getEntity().setTag( tag );
        }
    }

    void checkRequest()
        throws ResourceException
    {
        // Check command rules
        Instant unmodifiedSince = request.getConditions().getUnmodifiedSince().toInstant();
        EntityState state = spi.entityStateOf( entity );
        Instant lastModified = cutoffMillis( state.lastModified() );
        if( unmodifiedSince != null )
        {
            if( lastModified.isAfter( unmodifiedSince ) )
            {
                throw new ResourceException( Status.CLIENT_ERROR_CONFLICT );
            }
        }

        // Check query rules
        Instant modifiedSince = request.getConditions().getModifiedSince().toInstant();
        if( modifiedSince != null )
        {
            if( !lastModified.isAfter( modifiedSince ) )
            {
                throw new ResourceException( Status.REDIRECTION_NOT_MODIFIED );
            }
        }
    }

    private Instant cutoffMillis( long time )
    {
        return Instant.ofEpochMilli( time / 1000 * 1000 );
    }
}
