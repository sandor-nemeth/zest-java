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

package org.apache.zest.library.eventsourcing.application.source;

import java.io.IOException;
import org.apache.zest.io.Input;
import org.apache.zest.library.eventsourcing.application.api.TransactionApplicationEvents;

/**
 * An ApplicationEventSource is a source of application events that can be pulled in chunks. Events are grouped in the transactions in which they were created.
 */
public interface ApplicationEventSource
{
    /**
     * Get list of event transactions after the given timestamp.
     * <p>
     * If they are on the exact same timestamp, they will not be included.
     * <p>
     * The method uses the visitor pattern, so a visitor is sent in which is given each transaction, one at a time.
     *
     * @param afterTimestamp timestamp of transactions
     * @param maxTransactions maximum transactions
     * @return list of event transactions
     */
    Input<TransactionApplicationEvents, IOException> transactionsAfter( long afterTimestamp, long maxTransactions );

    /**
     * Get list of event transactions before the given timestamp.
     * <p>
     * If they are on the exact same timestamp, they will not be included.
     * <p>
     * The method uses the visitor pattern, so a visitor is sent in which is given each transaction, one at a time.
     * <p>
     * The transactions are sent to the visitor with the latest transaction first, i.e. walking backwards in the stream.
     *
     * @param beforeTimestamp timestamp of transactions
     * @param maxTransactions maximum transactions
     * @return list of event transactions
     */
    Input<TransactionApplicationEvents, IOException> transactionsBefore( long beforeTimestamp, long maxTransactions );
}
