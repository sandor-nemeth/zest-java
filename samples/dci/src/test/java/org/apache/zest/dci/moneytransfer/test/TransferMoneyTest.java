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

package org.apache.zest.dci.moneytransfer.test;

import org.apache.zest.api.unitofwork.UnitOfWorkFactory;
import org.apache.zest.bootstrap.unitofwork.DefaultUnitOfWorkAssembler;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.zest.api.unitofwork.UnitOfWork;
import org.apache.zest.api.usecase.UsecaseBuilder;
import org.apache.zest.bootstrap.AssemblyException;
import org.apache.zest.bootstrap.ModuleAssembly;
import org.apache.zest.bootstrap.SingletonAssembler;
import org.apache.zest.dci.moneytransfer.context.PayBillsContext;
import org.apache.zest.dci.moneytransfer.context.TransferMoneyContext;
import org.apache.zest.dci.moneytransfer.domain.data.BalanceData;
import org.apache.zest.dci.moneytransfer.domain.entity.CheckingAccountEntity;
import org.apache.zest.dci.moneytransfer.domain.entity.SavingsAccountEntity;
import org.apache.zest.dci.moneytransfer.rolemap.CheckingAccountRolemap;
import org.apache.zest.dci.moneytransfer.rolemap.CreditorRolemap;
import org.apache.zest.dci.moneytransfer.rolemap.SavingsAccountRolemap;
import org.apache.zest.test.EntityTestAssembler;

import static org.apache.zest.api.usecase.UsecaseBuilder.newUsecase;

/**
 * Test of TransferMoneyContext
 */
public class TransferMoneyTest
{
    private static SingletonAssembler assembler;
    public static final String SAVINGS_ACCOUNT_ID = "SavingsAccountId";
    public static final String CHECKING_ACCOUNT_ID = "CheckingAccountId";
    public static final String CREDITOR_ID1 = "BakerAccount";
    public static final String CREDITOR_ID2 = "ButcherAccount";
    private static UnitOfWorkFactory uowf;

    @BeforeClass
    public static void setup()
        throws Exception
    {
        assembler = new SingletonAssembler()
        {
            public void assemble( ModuleAssembly module )
                throws AssemblyException
            {
                module.withDefaultUnitOfWorkFactory();
                module.entities(
                    CheckingAccountRolemap.class,
                    SavingsAccountRolemap.class,
                    CreditorRolemap.class );

                new EntityTestAssembler().assemble( module );
            }
        };
        uowf = assembler.module().unitOfWorkFactory();
        bootstrapData( assembler );
    }

    @Before
    public void beforeBalances()
    {
        System.out.println( "Before enactment:" );
        printBalances();
        System.out.println( "" );
    }

    @After
    public void afterBalances()
    {
        System.out.println( "After enactment:" );
        printBalances();
        System.out.println( "-----------------" );
    }

    public void printBalances()
    {
        UnitOfWork uow = uowf.newUnitOfWork( UsecaseBuilder.newUsecase( "Print balances" ) );

        try
        {
            System.out
                .println( SAVINGS_ACCOUNT_ID + ":" + uow.get( BalanceData.class, SAVINGS_ACCOUNT_ID ).getBalance() );
            System.out
                .println( CHECKING_ACCOUNT_ID + ":" + uow.get( BalanceData.class, CHECKING_ACCOUNT_ID ).getBalance() );
            System.out.println( CREDITOR_ID1 + ":" + uow.get( BalanceData.class, CREDITOR_ID1 ).getBalance() );
            System.out.println( CREDITOR_ID2 + ":" + uow.get( BalanceData.class, CREDITOR_ID2 ).getBalance() );
        }
        finally
        {
            uow.discard();
        }
    }

    private static void bootstrapData( SingletonAssembler assembler )
        throws Exception
    {
        UnitOfWork uow = uowf.newUnitOfWork( newUsecase( "Bootstrap data" ) );
        try
        {
            SavingsAccountEntity savingsAccount = uow.newEntity( SavingsAccountEntity.class, SAVINGS_ACCOUNT_ID );
            savingsAccount.increasedBalance(1000);

            CheckingAccountEntity checkingAccount = uow.newEntity(CheckingAccountEntity.class, CHECKING_ACCOUNT_ID);
            checkingAccount.increasedBalance(200);

            // Create some creditor debt
            BalanceData bakerAccount = uow.newEntity( CreditorRolemap.class, CREDITOR_ID1 );
            bakerAccount.decreasedBalance( 50 );

            BalanceData butcherAccount = uow.newEntity( CreditorRolemap.class, CREDITOR_ID2 );
            butcherAccount.decreasedBalance( 90 );

            // Save
            uow.complete();
        }
        finally
        {
            uow.discard();
        }
    }

    @Test
    public void transferHalfOfMoneyFromSavingsToChecking()
        throws Exception
    {
        UnitOfWork uow = uowf.newUnitOfWork( UsecaseBuilder.newUsecase( "Transfer from savings to checking" ) );

        try
        {
            // Select source and destination
            BalanceData source = uow.get( BalanceData.class, SAVINGS_ACCOUNT_ID );
            BalanceData destination = uow.get( BalanceData.class, CHECKING_ACCOUNT_ID );

            // Instantiate context and execute enactments with that context
            TransferMoneyContext context = new TransferMoneyContext();
            context.bind( source, destination );

            // Query for half the balance
            final Integer amountToTransfer = context.availableFunds() / 2;

            // Transfer from savings to checking
            context.transfer( amountToTransfer );
        }
        finally
        {
            uow.discard();
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void transferTwiceOfMoneyFromSavingsToChecking()
        throws Exception
    {
        UnitOfWork uow = uowf.newUnitOfWork( UsecaseBuilder.newUsecase( "Transfer from savings to checking" ) );

        try
        {
            // Select source and destination
            BalanceData source = uow.get( BalanceData.class, SAVINGS_ACCOUNT_ID );
            BalanceData destination = uow.get( BalanceData.class, CHECKING_ACCOUNT_ID );

            // Instantiate context and execute enactments with that context
            TransferMoneyContext context = new TransferMoneyContext();
            context.bind( source, destination );

            // Query for double the balance
            final Integer amountToTransfer = context.availableFunds() * 2;

            // Transfer from savings to checking
            context.transfer( amountToTransfer );
        }
        finally
        {
            uow.discard();
        }
    }

    @Test
    public void payAllBills()
        throws Exception
    {
        UnitOfWork uow = uowf.newUnitOfWork( newUsecase( "Pay all bills from checking to creditors" ) );
        try
        {
            BalanceData source = uow.get( BalanceData.class, CHECKING_ACCOUNT_ID );

            PayBillsContext context = new PayBillsContext();
            context.bind( source ).payBills();
        }
        finally
        {
            uow.discard();
        }
    }
}
