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

package org.apache.zest.envisage.school.infrastructure.mail.assembly;

import org.apache.zest.api.configuration.Configuration;
import org.apache.zest.api.injection.scope.This;
import org.apache.zest.api.service.ServiceComposite;
import org.apache.zest.envisage.school.config.mail.MailConfiguration;
import org.apache.zest.envisage.school.infrastructure.mail.Mail;
import org.apache.zest.envisage.school.infrastructure.mail.MailService;

import java.util.Arrays;

public interface MailServiceComposite
    extends MailService, ServiceComposite
{
    public static class MailServiceMixin
        implements MailService
    {
        @This
        Configuration<MailConfiguration> config;

        @Override
        public void send( Mail... mails )
        {
            for( Mail mail : mails )
            {
                String[] recipients = mail.to().get();
                String mailSubject = mail.subject().toString();
                System.out.println(
                    "Sent email to [" + Arrays.toString( recipients ) + "] with subject [" + mailSubject + "]"
                );
            }
        }
    }

}