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
package org.apache.zest.library.shiro.domain.passwords;

import java.util.Set;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.zest.api.configuration.Configuration;
import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.injection.scope.This;
import org.apache.zest.api.query.QueryBuilder;
import org.apache.zest.api.query.QueryBuilderFactory;
import org.apache.zest.api.service.ServiceActivation;
import org.apache.zest.api.unitofwork.UnitOfWork;
import org.apache.zest.api.unitofwork.UnitOfWorkFactory;
import org.apache.zest.library.shiro.Shiro;
import org.apache.zest.library.shiro.domain.permissions.RoleAssignee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.zest.api.query.QueryExpressions.eq;
import static org.apache.zest.api.query.QueryExpressions.templateFor;

public class PasswordRealmMixin
    extends AuthorizingRealm
    implements Realm, Authorizer, PasswordService, ServiceActivation
{

    private static final Logger LOG = LoggerFactory.getLogger( Shiro.LOGGER_NAME );

    @Structure
    private UnitOfWorkFactory uowf;

    @Structure
    private QueryBuilderFactory qbf;

    @This
    private Configuration<PasswordRealmConfiguration> configuration;

    private final DefaultPasswordService passwordService;

    public PasswordRealmMixin()
    {
        super();
        passwordService = new DefaultPasswordService();
        PasswordMatcher matcher = new PasswordMatcher();
        matcher.setPasswordService( passwordService );
        setCredentialsMatcher( matcher );
    }

    @Override
    public void activateService()
        throws Exception
    {
        configuration.refresh();
        PasswordRealmConfiguration config = configuration.get();
        String algorithm = config.hashAlgorithmName().get();
        Integer iterations = config.hashIterationsCount().get();
        if( algorithm != null || iterations != null )
        {
            DefaultHashService hashService = (DefaultHashService) passwordService.getHashService();
            if( algorithm != null )
            {
                hashService.setHashAlgorithmName( algorithm );
            }
            if( iterations != null )
            {
                hashService.setHashIterations( iterations );
            }
        }
    }

    @Override
    public void passivateService()
        throws Exception
    {
    }

    @Override
    public String encryptPassword( Object plaintextPassword )
        throws IllegalArgumentException
    {
        return passwordService.encryptPassword( plaintextPassword );
    }

    @Override
    public boolean passwordsMatch( Object submittedPlaintext, String encrypted )
    {
        return passwordService.passwordsMatch( submittedPlaintext, encrypted );
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo( AuthenticationToken token )
        throws AuthenticationException
    {
        UnitOfWork uow = uowf.newUnitOfWork();
        try
        {

            String username = ( (UsernamePasswordToken) token ).getUsername();
            PasswordSecurable account = findPasswordSecurable( uow, username );
            if( account == null )
            {
                LOG.debug( "Unknown subject identifier: {}" + username );
                return null;
            }
            LOG.debug( "Found account for {}: {}", username, account );
            return new SimpleAuthenticationInfo( account.subjectIdentifier().get(), account.password()
                .get(), getName() );
        }
        finally
        {
            uow.discard();
        }
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo( PrincipalCollection principals )
    {
        UnitOfWork uow = uowf.newUnitOfWork();
        try
        {

            String username = getAvailablePrincipal( principals ).toString();
            RoleAssignee roleAssignee = findRoleAssignee( uow, username );
            if( roleAssignee == null )
            {
                LOG.debug( "No authorization info for {}", username );
                return null;
            }
            LOG.debug( "Found role assignee for {}: {}", username, roleAssignee );
            Set<String> roleNames = roleAssignee.roleNames();
            Set<String> permissionStrings = roleAssignee.permissionStrings();
            LOG.debug( "Found role assignee has the following roles: {}", roleNames );
            LOG.debug( "Found role assignee has the following permissions: {}", permissionStrings );
            SimpleAuthorizationInfo atzInfo = new SimpleAuthorizationInfo( roleNames );
            atzInfo.setStringPermissions( permissionStrings );
            return atzInfo;
        }
        finally
        {
            uow.discard();
        }
    }

    private PasswordSecurable findPasswordSecurable( UnitOfWork uow, String username )
    {
        QueryBuilder<PasswordSecurable> builder = qbf.newQueryBuilder( PasswordSecurable.class );
        builder = builder.where( eq( templateFor( PasswordSecurable.class ).subjectIdentifier(), username ) );
        return uow.newQuery( builder ).find();
    }

    private RoleAssignee findRoleAssignee( UnitOfWork uow, String username )
    {
        QueryBuilder<RoleAssignee> builder = qbf.newQueryBuilder( RoleAssignee.class );
        builder = builder.where( eq( templateFor( RoleAssignee.class ).subjectIdentifier(), username ) );
        return uow.newQuery( builder ).find();
    }
}
