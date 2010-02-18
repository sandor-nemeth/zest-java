/*  Copyright 2009 Alex Shneyderman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.entitystore.qrm;

import org.qi4j.api.entity.EntityReference;
import org.qi4j.spi.entity.EntityDescriptor;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entitystore.DefaultEntityStoreUnitOfWork;
import org.qi4j.spi.entitystore.helpers.DefaultEntityState;

public interface QrmMapper
{

    void bootstrap( QrmEntityStoreDescriptor cfg );

    Class findMappedMixin( EntityDescriptor eDesc );

    String fetchNextId( Class mappedClassName );

    EntityDescriptor fetchDescriptor( Class mappedClazz );

    EntityState get( DefaultEntityStoreUnitOfWork unitOfWork, Class mappedClazz, EntityReference identity );

    boolean newEntity( Class mappedClazz, DefaultEntityState state, String version );

    boolean delEntity( Class mappedClazz, DefaultEntityState state, String version );

    boolean updEntity( Class mappedClazz, DefaultEntityState state, String version );
}
