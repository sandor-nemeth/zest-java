/*  Copyright 2008 Edward Yakop.
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
package org.qi4j.library.swing.envisage.school.infrastructure.mail.mixins;

import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.injection.scope.This;
import org.qi4j.library.swing.envisage.school.infrastructure.mail.Mail;
import org.qi4j.library.swing.envisage.school.infrastructure.mail.MailConfiguration;
import org.qi4j.library.swing.envisage.school.infrastructure.mail.MailService;

import java.util.Arrays;

/**
 * @author edward.yakop@gmail.com
 * @since 0.5
 */
public final class MailServiceMixin
    implements MailService
{
    @This Configuration<MailConfiguration> config;

    public final void send( Mail... mails )
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