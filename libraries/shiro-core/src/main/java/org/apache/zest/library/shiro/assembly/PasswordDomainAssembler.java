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
package org.apache.zest.library.shiro.assembly;

import org.apache.zest.bootstrap.Assemblers;
import org.apache.zest.bootstrap.AssemblyException;
import org.apache.zest.bootstrap.ModuleAssembly;
import org.apache.zest.bootstrap.ServiceDeclaration;
import org.apache.zest.library.shiro.domain.passwords.PasswordRealmConfiguration;
import org.apache.zest.library.shiro.domain.passwords.PasswordRealmService;
import org.apache.zest.library.shiro.domain.passwords.PasswordSecurable;

public class PasswordDomainAssembler
    extends Assemblers.VisibilityIdentityConfig<PasswordDomainAssembler>
{
    @Override
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        module.entities( PasswordSecurable.class ).visibleIn( visibility() );
        ServiceDeclaration service = module.services( PasswordRealmService.class ).
            instantiateOnStartup().
            visibleIn( visibility() );
        if( hasIdentity() )
        {
            service.identifiedBy( identity() );
        }
        if( hasConfig() )
        {
            configModule().entities( PasswordRealmConfiguration.class ).
                visibleIn( configVisibility() );
        }
    }
}
