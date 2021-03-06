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
package org.apache.zest.index.sql.support.skeletons;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import org.apache.zest.api.ZestAPI;
import org.apache.zest.api.common.QualifiedName;
import org.apache.zest.api.composite.Composite;
import org.apache.zest.api.entity.EntityComposite;
import org.apache.zest.api.entity.Identity;
import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.injection.scope.This;
import org.apache.zest.api.injection.scope.Uses;
import org.apache.zest.api.query.grammar.AndPredicate;
import org.apache.zest.api.query.grammar.AssociationFunction;
import org.apache.zest.api.query.grammar.AssociationNotNullPredicate;
import org.apache.zest.api.query.grammar.AssociationNullPredicate;
import org.apache.zest.api.query.grammar.ComparisonPredicate;
import org.apache.zest.api.query.grammar.ContainsAllPredicate;
import org.apache.zest.api.query.grammar.ContainsPredicate;
import org.apache.zest.api.query.grammar.EqPredicate;
import org.apache.zest.api.query.grammar.GePredicate;
import org.apache.zest.api.query.grammar.GtPredicate;
import org.apache.zest.api.query.grammar.LePredicate;
import org.apache.zest.api.query.grammar.LtPredicate;
import org.apache.zest.api.query.grammar.ManyAssociationContainsPredicate;
import org.apache.zest.api.query.grammar.ManyAssociationFunction;
import org.apache.zest.api.query.grammar.MatchesPredicate;
import org.apache.zest.api.query.grammar.NePredicate;
import org.apache.zest.api.query.grammar.Notpredicate;
import org.apache.zest.api.query.grammar.OrPredicate;
import org.apache.zest.api.query.grammar.OrderBy;
import org.apache.zest.api.query.grammar.OrderBy.Order;
import org.apache.zest.api.query.grammar.PropertyFunction;
import org.apache.zest.api.query.grammar.PropertyNotNullPredicate;
import org.apache.zest.api.query.grammar.PropertyNullPredicate;
import org.apache.zest.api.query.grammar.Variable;
import org.apache.zest.api.service.ServiceDescriptor;
import org.apache.zest.api.unitofwork.UnitOfWorkFactory;
import org.apache.zest.api.value.ValueComposite;
import org.apache.zest.functional.Iterables;
import org.apache.zest.index.sql.support.api.SQLQuerying;
import org.apache.zest.index.sql.support.common.DBNames;
import org.apache.zest.index.sql.support.common.QNameInfo;
import org.apache.zest.index.sql.support.postgresql.PostgreSQLTypeHelper;
import org.apache.zest.spi.ZestSPI;
import org.apache.zest.spi.query.EntityFinderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql.generation.api.grammar.booleans.BooleanExpression;
import org.sql.generation.api.grammar.builders.booleans.BooleanBuilder;
import org.sql.generation.api.grammar.builders.booleans.InBuilder;
import org.sql.generation.api.grammar.builders.query.GroupByBuilder;
import org.sql.generation.api.grammar.builders.query.QueryBuilder;
import org.sql.generation.api.grammar.builders.query.QuerySpecificationBuilder;
import org.sql.generation.api.grammar.builders.query.TableReferenceBuilder;
import org.sql.generation.api.grammar.common.NonBooleanExpression;
import org.sql.generation.api.grammar.common.SQLFunctions;
import org.sql.generation.api.grammar.common.SetQuantifier;
import org.sql.generation.api.grammar.factories.BooleanFactory;
import org.sql.generation.api.grammar.factories.ColumnsFactory;
import org.sql.generation.api.grammar.factories.LiteralFactory;
import org.sql.generation.api.grammar.factories.QueryFactory;
import org.sql.generation.api.grammar.factories.TableReferenceFactory;
import org.sql.generation.api.grammar.query.ColumnReference;
import org.sql.generation.api.grammar.query.ColumnReferenceByName;
import org.sql.generation.api.grammar.query.Ordering;
import org.sql.generation.api.grammar.query.QueryExpression;
import org.sql.generation.api.grammar.query.QuerySpecification;
import org.sql.generation.api.grammar.query.TableReferenceByName;
import org.sql.generation.api.grammar.query.joins.JoinType;
import org.sql.generation.api.vendor.SQLVendor;

public abstract class AbstractSQLQuerying
    implements SQLQuerying
{

    @This
    private SQLDBState _state;

    @This
    private PostgreSQLTypeHelper _typeHelper;

    @Structure
    private UnitOfWorkFactory uowf;

    @Structure
    private ZestSPI spi;

    private static class TraversedAssoOrManyAssoRef
    {
        private final AssociationFunction<?> _traversedAsso;
        private final ManyAssociationFunction<?> _traversedManyAsso;
        private final boolean _hasRefs;

        private TraversedAssoOrManyAssoRef( AssociationFunction<?> func )
        {
            this( func.traversedAssociation(), func.traversedManyAssociation() );
        }

        private TraversedAssoOrManyAssoRef( PropertyFunction<?> func )
        {
            this( func.traversedAssociation(), func.traversedManyAssociation() );
        }

        private TraversedAssoOrManyAssoRef( ManyAssociationFunction<?> func )
        {
            this( func.traversedAssociation(), func.traversedManyAssociation() );
        }

        private TraversedAssoOrManyAssoRef( AssociationNullPredicate<?> spec )
        {
            this( spec.association(), null );
        }

        private TraversedAssoOrManyAssoRef( AssociationNotNullPredicate<?> spec )
        {
            this( spec.association(), null );
        }

        private TraversedAssoOrManyAssoRef( ManyAssociationContainsPredicate<?> spec )
        {
            this( null, spec.manyAssociation() );
        }

        private TraversedAssoOrManyAssoRef( AssociationFunction<?> traversedAsso,
                                            ManyAssociationFunction<?> traversedManyAsso
        )
        {
            this._traversedAsso = traversedAsso;
            this._traversedManyAsso = traversedManyAsso;
            this._hasRefs = this._traversedAsso != null || this._traversedManyAsso != null;
        }

        private TraversedAssoOrManyAssoRef getTraversedAssociation()
        {
            return this._traversedAsso == null
                   ? new TraversedAssoOrManyAssoRef( this._traversedManyAsso )
                   : new TraversedAssoOrManyAssoRef( this._traversedAsso );
        }

        private AccessibleObject getAccessor()
        {
            return this._traversedAsso == null
                   ? this._traversedManyAsso.accessor()
                   : this._traversedAsso.accessor();
        }

        @Override
        public String toString()
        {
            return "[hasRefs="
                   + this._hasRefs
                   + ", ref:"
                   + ( this._hasRefs ? getTraversedAssociations() : "null" )
                   + "]";
        }

        private Object getTraversedAssociations()
        {
            return this._traversedAsso == null ? this._traversedManyAsso : this._traversedAsso;
        }
    }

    public static interface SQLBooleanCreator
    {
        public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
            BooleanFactory factory,
            NonBooleanExpression left, NonBooleanExpression right
        );
    }

    private static interface BooleanExpressionProcessor
    {
        public QueryBuilder processBooleanExpression(
            AbstractSQLQuerying thisObject,
            Predicate<Composite> expression,
            Boolean negationActive,
            SQLVendor vendor,
            org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition,
            Map<String, Object> variables,
            List<Object> values,
            List<Integer> valueSQLTypes
        );
    }

    private static final Map<Class<? extends Predicate>, SQLBooleanCreator> SQL_OPERATORS;

    private static final Map<Class<? extends Predicate>, JoinType> JOIN_STYLES;

    private static final Map<Class<? extends Predicate>, JoinType> NEGATED_JOIN_STYLES;

    private static final Map<Class<?>, BooleanExpressionProcessor> EXPRESSION_PROCESSORS;

    private static final String TABLE_NAME_PREFIX = "t";

    private static final String TYPE_TABLE_SUFFIX = "_types";

    private static final Logger LOGGER = LoggerFactory.getLogger( AbstractSQLQuerying.class.getName() );

    static
    {
        SQL_OPERATORS = new HashMap<>( 9 );
        SQL_OPERATORS.put( EqPredicate.class, new SQLBooleanCreator()
        {
            @Override
            public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
                BooleanFactory factory,
                NonBooleanExpression left, NonBooleanExpression right
            )
            {
                return factory.eq( left, right );
            }
        } );
        SQL_OPERATORS.put( GePredicate.class, new SQLBooleanCreator()
        {
            @Override
            public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
                BooleanFactory factory,
                NonBooleanExpression left, NonBooleanExpression right
            )
            {
                return factory.geq( left, right );
            }
        } );
        SQL_OPERATORS.put( GtPredicate.class, new SQLBooleanCreator()
        {
            @Override
            public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
                BooleanFactory factory,
                NonBooleanExpression left, NonBooleanExpression right
            )
            {
                return factory.gt( left, right );
            }
        } );
        SQL_OPERATORS.put( LePredicate.class, new SQLBooleanCreator()
        {
            @Override
            public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
                BooleanFactory factory,
                NonBooleanExpression left, NonBooleanExpression right
            )
            {
                return factory.leq( left, right );
            }
        } );
        SQL_OPERATORS.put( LtPredicate.class, new SQLBooleanCreator()
        {
            @Override
            public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
                BooleanFactory factory,
                NonBooleanExpression left, NonBooleanExpression right
            )
            {
                return factory.lt( left, right );
            }
        } );
        SQL_OPERATORS.put( ManyAssociationContainsPredicate.class, new SQLBooleanCreator()
        {
            @Override
            public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
                BooleanFactory factory,
                NonBooleanExpression left, NonBooleanExpression right
            )
            {
                return factory.eq( left, right );
            }
        } );
        SQL_OPERATORS.put( MatchesPredicate.class, new SQLBooleanCreator()
        {
            @Override
            public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
                BooleanFactory factory,
                NonBooleanExpression left, NonBooleanExpression right
            )
            {
                return factory.regexp( left, right );
            }
        } );
        SQL_OPERATORS.put( ContainsPredicate.class, new SQLBooleanCreator()
        {
            @Override
            public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
                BooleanFactory factory,
                NonBooleanExpression left, NonBooleanExpression right
            )
            {
                return factory.eq( left, right );
            }
        } );
        SQL_OPERATORS.put( ContainsAllPredicate.class, new SQLBooleanCreator()
        {
            @Override
            public org.sql.generation.api.grammar.booleans.BooleanExpression getExpression(
                BooleanFactory factory,
                NonBooleanExpression left, NonBooleanExpression right
            )
            {
                return factory.eq( left, right );
            }
        } );

        JOIN_STYLES = new HashMap<>( 13 );
        JOIN_STYLES.put( EqPredicate.class, JoinType.INNER );
        JOIN_STYLES.put( GePredicate.class, JoinType.INNER );
        JOIN_STYLES.put( GtPredicate.class, JoinType.INNER );
        JOIN_STYLES.put( LePredicate.class, JoinType.INNER );
        JOIN_STYLES.put( LtPredicate.class, JoinType.INNER );
        JOIN_STYLES.put( PropertyNullPredicate.class, JoinType.LEFT_OUTER );
        JOIN_STYLES.put( PropertyNotNullPredicate.class, JoinType.INNER );
        JOIN_STYLES.put( AssociationNullPredicate.class, JoinType.LEFT_OUTER );
        JOIN_STYLES.put( AssociationNotNullPredicate.class, JoinType.INNER );
        JOIN_STYLES.put( ManyAssociationContainsPredicate.class, JoinType.INNER );
        JOIN_STYLES.put( MatchesPredicate.class, JoinType.INNER );
        JOIN_STYLES.put( ContainsPredicate.class, JoinType.INNER );
        JOIN_STYLES.put( ContainsAllPredicate.class, JoinType.INNER );

        NEGATED_JOIN_STYLES = new HashMap<>( 13 );
        NEGATED_JOIN_STYLES.put( EqPredicate.class, JoinType.LEFT_OUTER );
        NEGATED_JOIN_STYLES.put( GePredicate.class, JoinType.LEFT_OUTER );
        NEGATED_JOIN_STYLES.put( GtPredicate.class, JoinType.LEFT_OUTER );
        NEGATED_JOIN_STYLES.put( LePredicate.class, JoinType.LEFT_OUTER );
        NEGATED_JOIN_STYLES.put( LtPredicate.class, JoinType.LEFT_OUTER );
        NEGATED_JOIN_STYLES.put( PropertyNullPredicate.class, JoinType.INNER );
        NEGATED_JOIN_STYLES.put( PropertyNotNullPredicate.class, JoinType.LEFT_OUTER );
        NEGATED_JOIN_STYLES.put( AssociationNullPredicate.class, JoinType.INNER );
        NEGATED_JOIN_STYLES.put( AssociationNotNullPredicate.class, JoinType.LEFT_OUTER );
        NEGATED_JOIN_STYLES.put( ManyAssociationContainsPredicate.class, JoinType.INNER );
        NEGATED_JOIN_STYLES.put( MatchesPredicate.class, JoinType.LEFT_OUTER );
        NEGATED_JOIN_STYLES.put( ContainsPredicate.class, JoinType.LEFT_OUTER );
        NEGATED_JOIN_STYLES.put( ContainsAllPredicate.class, JoinType.LEFT_OUTER );

        EXPRESSION_PROCESSORS = new HashMap<>( 17 );
        EXPRESSION_PROCESSORS.put( AndPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive, SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                QueryBuilder result = null;
                AndPredicate conjunction = (AndPredicate) expression;
                for( Predicate<Composite> entitySpecification : conjunction.operands() )
                {
                    if( result == null )
                    {
                        result = thisObject.processBooleanExpression(
                            entitySpecification,
                            negationActive, vendor,
                            entityTypeCondition, variables, values, valueSQLTypes );
                    }
                    else
                    {
                        result = result.intersect( thisObject.processBooleanExpression(
                            entitySpecification, negationActive, vendor,
                            entityTypeCondition, variables, values, valueSQLTypes )
                                                       .createExpression() );
                    }
                }
                return result;
            }
        } );
        EXPRESSION_PROCESSORS.put( OrPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive, SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                QueryBuilder result = null;
                OrPredicate conjunction = (OrPredicate) expression;
                for( Predicate<Composite> entitySpecification : conjunction.operands() )
                {
                    if( result == null )
                    {
                        result = thisObject.processBooleanExpression(
                            entitySpecification,
                            negationActive, vendor,
                            entityTypeCondition, variables, values, valueSQLTypes );
                    }
                    else
                    {
                        result
                            = result.union( thisObject.processBooleanExpression(
                            entitySpecification,
                            negationActive, vendor,
                            entityTypeCondition, variables, values, valueSQLTypes )
                                                .createExpression() );
                    }
                }
                return result;
            }
        } );
        EXPRESSION_PROCESSORS.put( Notpredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive, SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                return thisObject.processBooleanExpression(
                    ( (Notpredicate) expression ).operand(), !negationActive, vendor,
                    entityTypeCondition, variables, values, valueSQLTypes );
            }
        } );
        EXPRESSION_PROCESSORS.put( MatchesPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive, SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                return thisObject.processMatchesPredicate( (MatchesPredicate) expression,
                                                           negationActive, vendor,
                                                           entityTypeCondition, variables, values, valueSQLTypes );
            }
        } );
        EXPRESSION_PROCESSORS.put( ManyAssociationContainsPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive,
                SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                return thisObject.processManyAssociationContainsPredicate(
                    (ManyAssociationContainsPredicate<?>) expression, negationActive,
                    vendor, entityTypeCondition,
                    variables, values, valueSQLTypes );
            }
        } );
        EXPRESSION_PROCESSORS.put( PropertyNullPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive,
                SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                return thisObject.processPropertyNullPredicate(
                    (PropertyNullPredicate<?>) expression, negationActive,
                    vendor, entityTypeCondition );
            }
        } );
        EXPRESSION_PROCESSORS.put( PropertyNotNullPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive,
                SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                return thisObject.processPropertyNotNullPredicate(
                    (PropertyNotNullPredicate<?>) expression, negationActive,
                    vendor, entityTypeCondition );
            }
        } );
        EXPRESSION_PROCESSORS.put( AssociationNullPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive,
                SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                return thisObject.processAssociationNullPredicate(
                    (AssociationNullPredicate<?>) expression, negationActive,
                    vendor, entityTypeCondition );
            }
        } );
        EXPRESSION_PROCESSORS.put( AssociationNotNullPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression( AbstractSQLQuerying thisObject,
                                                          Predicate<Composite> expression,
                                                          Boolean negationActive,
                                                          SQLVendor vendor,
                                                          BooleanExpression entityTypeCondition,
                                                          Map<String, Object> variables,
                                                          List<Object> values,
                                                          List<Integer> valueSQLTypes
            )
            {
                return thisObject.processAssociationNotNullPredicate(
                    (AssociationNotNullPredicate<?>) expression, negationActive,
                    vendor, entityTypeCondition );
            }
        } );
        EXPRESSION_PROCESSORS.put( ContainsPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive, SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                return thisObject.processContainsPredicate( (ContainsPredicate<?>) expression,
                                                            negationActive, vendor,
                                                            entityTypeCondition, variables, values, valueSQLTypes );
            }
        } );
        EXPRESSION_PROCESSORS.put( ContainsAllPredicate.class, new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive, SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                return thisObject.processContainsAllPredicate(
                    (ContainsAllPredicate<?>) expression, negationActive,
                    vendor, entityTypeCondition, variables, values, valueSQLTypes );
            }
        } );
        BooleanExpressionProcessor comparisonProcessor = new BooleanExpressionProcessor()
        {
            @Override
            public QueryBuilder processBooleanExpression(
                AbstractSQLQuerying thisObject,
                Predicate<Composite> expression, Boolean negationActive, SQLVendor vendor,
                BooleanExpression entityTypeCondition, Map<String, Object> variables,
                List<Object> values,
                List<Integer> valueSQLTypes
            )
            {
                return thisObject.processComparisonPredicate(
                    (ComparisonPredicate<?>) expression, negationActive, vendor,
                    entityTypeCondition, variables, values, valueSQLTypes );
            }
        };
        EXPRESSION_PROCESSORS.put( EqPredicate.class, comparisonProcessor );
        EXPRESSION_PROCESSORS.put( NePredicate.class, comparisonProcessor );
        EXPRESSION_PROCESSORS.put( GePredicate.class, comparisonProcessor );
        EXPRESSION_PROCESSORS.put( GtPredicate.class, comparisonProcessor );
        EXPRESSION_PROCESSORS.put( LePredicate.class, comparisonProcessor );
        EXPRESSION_PROCESSORS.put( LtPredicate.class, comparisonProcessor );
    }

    private interface WhereClauseProcessor
    {
        public void processWhereClause( QuerySpecificationBuilder builder,
                                        BooleanBuilder afterWhere,
                                        JoinType joinStyle, Integer firstTableIndex, Integer lastTableIndex
        );
    }

    private static class PropertyNullWhereClauseProcessor
        implements WhereClauseProcessor
    {
        private final boolean negationActive;
        private final SQLVendor vendor;
        private final SQLDBState state;
        private final PropertyFunction<?> propFunction;

        private PropertyNullWhereClauseProcessor( SQLDBState pState, SQLVendor pVendor,
                                                  PropertyFunction<?> pPropFunction,
                                                  boolean pNegationActive
        )
        {
            this.state = pState;
            this.vendor = pVendor;
            this.negationActive = pNegationActive;
            this.propFunction = pPropFunction;
        }

        @Override
        public void processWhereClause( QuerySpecificationBuilder builder,
                                        BooleanBuilder afterWhere,
                                        JoinType joinStyle, Integer firstTableIndex, Integer lastTableIndex
        )
        {
            if( !this.negationActive )
            {
                ColumnsFactory c = this.vendor.getColumnsFactory();
                BooleanFactory b = this.vendor.getBooleanFactory();

                QNameInfo info = this.state
                    .qNameInfos()
                    .get()
                    .get(
                        QualifiedName.fromAccessor( this.propFunction.accessor() ) );
                String colName;
                if( info.getCollectionDepth() > 0 )
                {
                    colName = DBNames.ALL_QNAMES_TABLE_PK_COLUMN_NAME;
                }
                else
                {
                    colName = DBNames.QNAME_TABLE_VALUE_COLUMN_NAME;
                }
                // Last table column might be null because of left joins
                builder.getWhere().reset(
                    b.isNull( c.colName( TABLE_NAME_PREFIX + lastTableIndex, colName ) ) );
            }
        }
    }

    private static class AssociationNullWhereClauseProcessor
        implements WhereClauseProcessor
    {
        private final boolean negationActive;
        private final SQLVendor vendor;

        private AssociationNullWhereClauseProcessor( SQLVendor pVendor, boolean pNegationActive )
        {
            this.vendor = pVendor;
            this.negationActive = pNegationActive;
        }

        @Override
        public void processWhereClause( QuerySpecificationBuilder builder,
                                        BooleanBuilder afterWhere,
                                        JoinType joinStyle, Integer firstTableIndex, Integer lastTableIndex
        )
        {
            if( !negationActive )
            {
                ColumnsFactory c = vendor.getColumnsFactory();
                BooleanFactory b = vendor.getBooleanFactory();

                // Last table column might be null because of left joins
                builder.getWhere().reset(
                    b.isNull( c.colName( TABLE_NAME_PREFIX + lastTableIndex,
                                         DBNames.QNAME_TABLE_VALUE_COLUMN_NAME ) ) );
            }
        }
    }

    private static class ModifiableInt
    {
        private int _int;

        private ModifiableInt( Integer integer )
        {
            this._int = integer;
        }

        private int getInt()
        {
            return this._int;
        }

        private void setInt( int integer )
        {
            this._int = integer;
        }

        @Override
        public String toString()
        {
            return Integer.toString( this._int );
        }
    }

    private static class QNameJoin
    {
        private final QualifiedName _sourceQName;

        private final QualifiedName _targetQName;

        private final Integer _sourceTableIndex;

        private final Integer _targetTableIndex;

        private QNameJoin( QualifiedName sourceQName, QualifiedName targetQName,
                           Integer sourceTableIndex,
                           Integer targetTableIndex
        )
        {
            this._sourceQName = sourceQName;
            this._targetQName = targetQName;
            this._sourceTableIndex = sourceTableIndex;
            this._targetTableIndex = targetTableIndex;
        }

        private QualifiedName getSourceQName()
        {
            return this._sourceQName;
        }

        private QualifiedName getTargetQName()
        {
            return this._targetQName;
        }

        private Integer getSourceTableIndex()
        {
            return this._sourceTableIndex;
        }

        private Integer getTargetTableIndex()
        {
            return this._targetTableIndex;
        }
    }

    @Override
    public Integer getResultSetType( Integer firstResult, Integer maxResults )
    {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public Boolean isFirstResultSettingSupported()
    {
        return true;
    }

    @Uses
    private ServiceDescriptor descriptor;

    @Override
    public String constructQuery( Class<?> resultType, //
                                  Predicate<Composite> whereClause, //
                                  OrderBy[] orderBySegments, //
                                  Integer firstResult, //
                                  Integer maxResults, //
                                  Map<String, Object> variables, //
                                  List<Object> values, //
                                  List<Integer> valueSQLTypes, //
                                  Boolean countOnly //
    )
        throws EntityFinderException
    {
        SQLVendor vendor = this.descriptor.metaInfo( SQLVendor.class );

        QueryFactory q = vendor.getQueryFactory();
        TableReferenceFactory t = vendor.getTableReferenceFactory();
        LiteralFactory l = vendor.getLiteralFactory();
        ColumnsFactory c = vendor.getColumnsFactory();

        ColumnReference mainColumn = c.colName( TABLE_NAME_PREFIX + "0", DBNames.ENTITY_TABLE_IDENTITY_COLUMN_NAME );
        if( countOnly )
        {
            mainColumn = c.colExp( l.func( SQLFunctions.COUNT, mainColumn ) );
        }

        QueryBuilder innerBuilder = this.processBooleanExpression(
            whereClause, false, vendor,
            this.createTypeCondition( resultType, vendor ), variables, values, valueSQLTypes );

        QuerySpecificationBuilder mainQuery = q.querySpecificationBuilder();
        mainQuery.getSelect().addUnnamedColumns( mainColumn );
        mainQuery.getFrom().addTableReferences(
            t.tableBuilder( t.table( q.createQuery( innerBuilder.createExpression() ),
                                     t.tableAlias( TABLE_NAME_PREFIX + "0" ) ) ) );

        this.processOrderBySegments( orderBySegments, vendor, mainQuery );

        QueryExpression finalMainQuery = this.finalizeQuery(
            vendor, mainQuery, resultType, whereClause,
            orderBySegments, firstResult, maxResults, variables, values, valueSQLTypes,
            countOnly );

        String result = vendor.toString( finalMainQuery );

        LOGGER.info( "SQL query:\n" + result );
        return result;
    }

    protected org.sql.generation.api.grammar.booleans.BooleanExpression createTypeCondition(
        Class<?> resultType,
        SQLVendor vendor
    )
    {
        BooleanFactory b = vendor.getBooleanFactory();
        LiteralFactory l = vendor.getLiteralFactory();
        ColumnsFactory c = vendor.getColumnsFactory();

        List<Integer> typeIDs = this.getEntityTypeIDs( resultType );
        InBuilder in = b.inBuilder( c.colName( TABLE_NAME_PREFIX + TYPE_TABLE_SUFFIX,
                                               DBNames.ENTITY_TYPES_TABLE_PK_COLUMN_NAME ) );
        for( Integer i : typeIDs )
        {
            in.addValues( l.n( i ) );
        }

        return in.createExpression();
    }

    protected abstract QueryExpression finalizeQuery(
        SQLVendor sqlVendor, QuerySpecificationBuilder specBuilder,
        Class<?> resultType,
        Predicate<Composite> whereClause,
        OrderBy[] orderBySegments,
        Integer firstResult,
        Integer maxResults,
        Map<String, Object> variables,
        List<Object> values,
        List<Integer> valueSQLTypes,
        Boolean countOnly
    );

    protected QueryBuilder processBooleanExpression(
        Predicate<Composite> expression,
        Boolean negationActive,
        SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition,
        Map<String, Object> variables,
        List<Object> values,
        List<Integer> valueSQLTypes
    )
    {
        QueryBuilder result = null;
        if( expression == null )
        {
            QueryFactory q = vendor.getQueryFactory();
            result = q.queryBuilder(
                this.selectAllEntitiesOfCorrectType( vendor, entityTypeCondition ).createExpression() );
        }
        else
        {
            if( EXPRESSION_PROCESSORS.containsKey( expression.getClass() ) )
            {
                result = EXPRESSION_PROCESSORS.get( expression.getClass() ).processBooleanExpression(
                    this, expression,
                    negationActive, vendor, entityTypeCondition, variables, values,
                    valueSQLTypes );
            }
            else
            {
                throw new UnsupportedOperationException( "Expression " + expression + " of type "
                                                         + expression.getClass() + " is not supported" );
            }
        }
        return result;
    }

    protected QuerySpecificationBuilder selectAllEntitiesOfCorrectType(
        SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition
    )
    {
        TableReferenceFactory t = vendor.getTableReferenceFactory();

        String tableAlias = TABLE_NAME_PREFIX + "0";
        TableReferenceBuilder from = t.tableBuilder( t.table(
            t.tableName( this._state.schemaName().get(), DBNames.ENTITY_TABLE_NAME ),
            t.tableAlias( tableAlias ) ) );

        this.addTypeJoin( vendor, from, 0 );

        QuerySpecificationBuilder query = this.getBuilderForPredicate( vendor, tableAlias );
        query.getFrom().addTableReferences( from );
        query.getWhere().reset( entityTypeCondition );

        return query;
    }

    protected QueryBuilder processMatchesPredicate(
        final MatchesPredicate predicate,
        final Boolean negationActive,
        final SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition,
        final Map<String, Object> variables, final List<Object> values,
        final List<Integer> valueSQLTypes
    )
    {
        return this.singleQuery(
            predicate,
            predicate.property(),
            null,
            null,
            negationActive,
            vendor,
            entityTypeCondition,
            new WhereClauseProcessor()
            {

                @Override
                public void processWhereClause( QuerySpecificationBuilder builder,
                                                BooleanBuilder afterWhere,
                                                JoinType joinStyle, Integer firstTableIndex, Integer lastTableIndex
                )
                {
                    LiteralFactory l = vendor.getLiteralFactory();
                    ColumnsFactory c = vendor.getColumnsFactory();

                    builder.getWhere().reset(
                        vendor.getBooleanFactory().regexp(
                            c.colName( TABLE_NAME_PREFIX + lastTableIndex,
                                       DBNames.QNAME_TABLE_VALUE_COLUMN_NAME ),
                            l.param() ) );

                    Object value = predicate.value();
                    if( value instanceof Variable )
                    {
                        value = variables.get( ( (Variable) value ).variableName() );
                    }
                    values.add( translateJavaRegexpToPGSQLRegexp( value.toString() ) );
                    valueSQLTypes.add( Types.VARCHAR );
                }
            } //
        );
    }

    protected QueryBuilder processComparisonPredicate(
        final ComparisonPredicate<?> predicate,
        final Boolean negationActive, final SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition,
        final Map<String, Object> variables,
        final List<Object> values, final List<Integer> valueSQLTypes
    )
    {
        return this.singleQuery(
            predicate,
            predicate.property(),
            null,
            null,
            negationActive,
            vendor,
            entityTypeCondition,
            new WhereClauseProcessor()
            {

                @Override
                public void processWhereClause( QuerySpecificationBuilder builder,
                                                BooleanBuilder afterWhere,
                                                JoinType joinStyle, Integer firstTableIndex, Integer lastTableIndex
                )
                {
                    QualifiedName qName
                        = QualifiedName.fromAccessor( predicate.property().accessor() );
                    String columnName;
                    if( qName.type().equals( Identity.class.getName() ) )
                    {
                        columnName = DBNames.ENTITY_TABLE_IDENTITY_COLUMN_NAME;
                    }
                    else
                    {
                        columnName = DBNames.QNAME_TABLE_VALUE_COLUMN_NAME;
                    }
                    Object value = predicate.value();
                    modifyFromClauseAndWhereClauseToGetValue(
                        qName, value, predicate,
                        negationActive, lastTableIndex,
                        new ModifiableInt( lastTableIndex ), columnName,
                        DBNames.QNAME_TABLE_COLLECTION_PATH_TOP_LEVEL_NAME, vendor,
                        builder.getWhere(), afterWhere,
                        builder.getFrom().getTableReferences().iterator().next(),
                        builder.getGroupBy(),
                        builder.getHaving(), new ArrayList<QNameJoin>(), variables, values,
                        valueSQLTypes );
                }
            } //
        );
    }

    protected QueryBuilder processManyAssociationContainsPredicate(
        final ManyAssociationContainsPredicate<?> predicate, final Boolean negationActive,
        final SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition,
        Map<String, Object> variables,
        final List<Object> values, final List<Integer> valueSQLTypes
    )
    {
        return this.singleQuery(
            predicate,
            null,
            new TraversedAssoOrManyAssoRef( predicate ), // not sure about this, was 'null' before but I think this is needed.
            true,
            negationActive,
            vendor,
            entityTypeCondition,
            new WhereClauseProcessor()
            {

                @Override
                public void processWhereClause( QuerySpecificationBuilder builder,
                                                BooleanBuilder afterWhere,
                                                JoinType joinStyle, Integer firstTableIndex, Integer lastTableIndex
                )
                {
                    LiteralFactory l = vendor.getLiteralFactory();
                    ColumnsFactory c = vendor.getColumnsFactory();
                    BooleanFactory b = vendor.getBooleanFactory();

                    builder.getWhere().reset(
                        getOperator( predicate ).getExpression(
                            b,
                            c.colName( TABLE_NAME_PREFIX + lastTableIndex,
                                       DBNames.ENTITY_TABLE_IDENTITY_COLUMN_NAME ),
                            l.param() ) );

                    Object value = predicate.value();
                    // TODO Is it really certain that this value is always instance of
                    // EntityComposite?
                    if( value instanceof EntityComposite )
                    {
                        value = uowf.currentUnitOfWork().get(
                            (EntityComposite) value ).identity().get();
                    }
                    else
                    {
                        value = value.toString();
                    }
                    values.add( value );
                    valueSQLTypes.add( Types.VARCHAR );
                }
            }
        );
    }

    protected QueryBuilder processPropertyNullPredicate(
        final PropertyNullPredicate<?> predicate,
        final Boolean negationActive, final SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition
    )
    {
        return this.singleQuery(
            predicate,
            predicate.property(),
            null,
            null,
            negationActive,
            vendor,
            entityTypeCondition,
            new PropertyNullWhereClauseProcessor( this._state, vendor, predicate.property(), negationActive )
        );
    }

    protected QueryBuilder processPropertyNotNullPredicate(
        PropertyNotNullPredicate<?> predicate,
        boolean negationActive, SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition
    )
    {
        return this.singleQuery(
            predicate,
            predicate.property(),
            null,
            null,
            negationActive,
            vendor,
            entityTypeCondition,
            new PropertyNullWhereClauseProcessor( this._state, vendor, predicate.property(), !negationActive )
        );
    }

    protected QueryBuilder processAssociationNullPredicate(
        final AssociationNullPredicate<?> predicate,
        final Boolean negationActive, final SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition
    )
    {
        return this.singleQuery(
            predicate, //
            null, //
            new TraversedAssoOrManyAssoRef( predicate ), //
            false, //
            negationActive, //
            vendor, //
            entityTypeCondition, //
            new AssociationNullWhereClauseProcessor( vendor, negationActive )
        );
    }

    protected QueryBuilder processAssociationNotNullPredicate(
        final AssociationNotNullPredicate<?> predicate,
        final Boolean negationActive, final SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition
    )
    {
        return this.singleQuery(
            predicate, //
            null, //
            new TraversedAssoOrManyAssoRef( predicate ), //
            false, //
            negationActive, //
            vendor, //
            entityTypeCondition, //
            new AssociationNullWhereClauseProcessor( vendor, !negationActive )
        );
    }

    protected QueryBuilder processContainsPredicate(
        final ContainsPredicate<?> predicate,
        final Boolean negationActive, final SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition,
        final Map<String, Object> variables,
        final List<Object> values, final List<Integer> valueSQLTypes
    )
    {
        // Path: Top.* (star without braces), value = value
        // ASSUMING value is NOT collection (ie, find all entities, which collection property has
        // value x as leaf item,
        // no matter collection depth)
        QuerySpecification contains = this.constructQueryForPredicate(
            predicate, //
            predicate.collectionProperty(), //
            null, //
            null, //
            false, //
            vendor, //
            entityTypeCondition, //
            new WhereClauseProcessor()
            {
                @Override
                public void processWhereClause( QuerySpecificationBuilder builder,
                                                BooleanBuilder afterWhere,
                                                JoinType joinStyle, Integer firstTableIndex, Integer lastTableIndex
                )
                {
                    BooleanFactory b = vendor.getBooleanFactory();
                    LiteralFactory l = vendor.getLiteralFactory();
                    ColumnsFactory c = vendor.getColumnsFactory();

                    builder.getWhere().reset(
                        b.regexp( c.colName( TABLE_NAME_PREFIX + lastTableIndex,
                                             DBNames.QNAME_TABLE_COLLECTION_PATH_COLUMN_NAME ), l
                                      .s( DBNames.QNAME_TABLE_COLLECTION_PATH_TOP_LEVEL_NAME + ".*{1,}" ) ) );

                    Object value = predicate.value();
                    if( value instanceof Collection<?> )
                    {
                        throw new IllegalArgumentException(
                            "ContainsPredicate may have only either primitive or value composite as value." );
                    }
                    BooleanBuilder condition = b.booleanBuilder();
                    modifyFromClauseAndWhereClauseToGetValue(
                        QualifiedName.fromAccessor( predicate.collectionProperty().accessor() ), value, predicate,
                        false, lastTableIndex, new ModifiableInt( lastTableIndex ),
                        DBNames.QNAME_TABLE_VALUE_COLUMN_NAME,
                        DBNames.QNAME_TABLE_COLLECTION_PATH_TOP_LEVEL_NAME,
                        vendor, condition, afterWhere, builder.getFrom().getTableReferences()
                            .iterator().next(),
                        builder.getGroupBy(), builder.getHaving(), new ArrayList<QNameJoin>(),
                        variables, values, valueSQLTypes );
                    builder.getWhere().and( condition.createExpression() );
                }
            } //
        );

        return this.finalizeContainsQuery( vendor, contains, entityTypeCondition, negationActive );
    }

    protected QueryBuilder finalizeContainsQuery(
        SQLVendor vendor, QuerySpecification contains,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition,
        Boolean negationActive
    )
    {
        QueryFactory q = vendor.getQueryFactory();
        QueryBuilder result;

        if( negationActive )
        {
            result = q.queryBuilder(
                this.selectAllEntitiesOfCorrectType( vendor, entityTypeCondition )
                    .createExpression() ).except(
                contains );
        }
        else
        {
            result = q.queryBuilder( contains );
        }

        return result;
    }

    protected QueryBuilder processContainsAllPredicate(
        final ContainsAllPredicate<?> predicate, final Boolean negationActive,
        final SQLVendor vendor,
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition,
        final Map<String, Object> variables, final List<Object> values,
        final List<Integer> valueSQLTypes
    )
    {
        // has all leaf items in specified collection

        QuerySpecification contains = this.constructQueryForPredicate(
            predicate, //
            predicate.collectionProperty(), //
            null, //
            null, //
            false, //
            vendor, //
            entityTypeCondition, //
            new WhereClauseProcessor()
            {

                @Override
                public void processWhereClause( QuerySpecificationBuilder builder,
                                                BooleanBuilder afterWhere,
                                                JoinType joinStyle, Integer firstTableIndex, Integer lastTableIndex
                )
                {
                    BooleanFactory b = vendor.getBooleanFactory();
                    LiteralFactory l = vendor.getLiteralFactory();
                    ColumnsFactory c = vendor.getColumnsFactory();

                    Iterable<?> collection = predicate.containedValues();
                    List<QNameJoin> joins = new ArrayList<>();
                    for( Object value : collection )
                    {
                        if( value instanceof Collection<?> )
                        {
                            throw new IllegalArgumentException(
                                "ContainsAllPredicate may not have nested collections as value." );
                        }

                        BooleanBuilder conditionForItem = b.booleanBuilder(
                            b.regexp( c.colName( TABLE_NAME_PREFIX + lastTableIndex,
                                                 DBNames.QNAME_TABLE_COLLECTION_PATH_COLUMN_NAME ),
                                      l.s( DBNames.QNAME_TABLE_COLLECTION_PATH_TOP_LEVEL_NAME + ".*{1,}" ) ) );
                        modifyFromClauseAndWhereClauseToGetValue(
                            QualifiedName.fromAccessor( predicate.collectionProperty().accessor() ),
                            value, predicate, false, lastTableIndex,
                            new ModifiableInt( lastTableIndex ),
                            DBNames.QNAME_TABLE_VALUE_COLUMN_NAME,
                            DBNames.QNAME_TABLE_COLLECTION_PATH_TOP_LEVEL_NAME, vendor,
                            conditionForItem, afterWhere,
                            builder.getFrom().getTableReferences().iterator().next(),
                            builder.getGroupBy(), builder.getHaving(),
                            joins, variables, values, valueSQLTypes );
                        builder.getWhere().or( conditionForItem.createExpression() );
                    }

                    builder.getHaving()
                        .and(
                            b.geq(
                                l.func( "COUNT", c.colName( TABLE_NAME_PREFIX + lastTableIndex,
                                                            DBNames.QNAME_TABLE_VALUE_COLUMN_NAME ) ),
                                l.n( Iterables.count( collection ) ) ) );
                }
            } //
        );

        return this.finalizeContainsQuery( vendor, contains, entityTypeCondition, negationActive );
    }

    protected QueryBuilder singleQuery(
        Predicate<Composite> predicate, //
        PropertyFunction<?> propRef, //
        TraversedAssoOrManyAssoRef assoRef, //
        Boolean includeLastAssoPathTable, //
        Boolean negationActive, //
        SQLVendor vendor, //
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition, //
        WhereClauseProcessor whereClauseGenerator//
    )
    {
        return vendor.getQueryFactory().queryBuilder(
            this.constructQueryForPredicate( predicate, propRef, assoRef, includeLastAssoPathTable,
                                             negationActive,
                                             vendor, entityTypeCondition, whereClauseGenerator ) );
    }

    protected QuerySpecification constructQueryForPredicate(
        Predicate<Composite> predicate, //
        PropertyFunction<?> propRef, //
        TraversedAssoOrManyAssoRef assoRef, //
        Boolean includeLastAssoPathTable, //
        Boolean negationActive, //
        SQLVendor vendor, //
        org.sql.generation.api.grammar.booleans.BooleanExpression entityTypeCondition, //
        WhereClauseProcessor whereClauseGenerator//
    )
    {
        Integer startingIndex = 0;
        TableReferenceFactory t = vendor.getTableReferenceFactory();

        QuerySpecificationBuilder builder = this.getBuilderForPredicate( vendor, TABLE_NAME_PREFIX + startingIndex );
        TableReferenceBuilder from = t.tableBuilder( t.table(
            t.tableName( this._state.schemaName().get(), DBNames.ENTITY_TABLE_NAME ),
            t.tableAlias( TABLE_NAME_PREFIX + startingIndex ) ) );

        this.addTypeJoin( vendor, from, startingIndex );

        Integer lastTableIndex = null;
        JoinType joinStyle = this.getTableJoinStyle( predicate, negationActive );
        if( propRef == null && assoRef != null && assoRef._hasRefs )
        {
            lastTableIndex = this.traverseAssociationPath( assoRef, startingIndex, startingIndex + 1, vendor, from,
                                                           joinStyle, includeLastAssoPathTable );
        }
        else if( assoRef == null || !assoRef._hasRefs )
        {
            lastTableIndex = this.traversePropertyPath( propRef, startingIndex, startingIndex + 1, vendor, from,
                                                        joinStyle );
        }
        else
        {
            throw new InternalError(
                "Can not have both property reference and association reference (non-)nulls [propRef=" + propRef
                + ", assoRef=" + assoRef + ", predicate=" + predicate + "]." );
        }

        builder.getFrom().addTableReferences( from );

        BooleanBuilder afterWhere = vendor.getBooleanFactory().booleanBuilder();
        whereClauseGenerator.processWhereClause( builder, afterWhere, joinStyle, startingIndex, lastTableIndex );

        BooleanBuilder where = builder.getWhere();
        if( negationActive )
        {
            where.not();
        }
        where.and( afterWhere.createExpression() );

        where.and( entityTypeCondition );

        builder.trimGroupBy();

        return builder.createExpression();
    }

    protected void addTypeJoin( SQLVendor vendor, TableReferenceBuilder from, int startingIndex )
    {
        TableReferenceFactory t = vendor.getTableReferenceFactory();
        BooleanFactory b = vendor.getBooleanFactory();
        ColumnsFactory c = vendor.getColumnsFactory();

        from.addQualifiedJoin(
            JoinType.INNER,
            t.table(
                t.tableName( this._state.schemaName().get(), DBNames.ENTITY_TYPES_JOIN_TABLE_NAME ),
                t.tableAlias( TABLE_NAME_PREFIX + TYPE_TABLE_SUFFIX ) ),
            t.jc( b.eq(
                c.colName( TABLE_NAME_PREFIX + startingIndex, DBNames.ENTITY_TABLE_PK_COLUMN_NAME ),
                c.colName( TABLE_NAME_PREFIX + TYPE_TABLE_SUFFIX, DBNames.ENTITY_TABLE_PK_COLUMN_NAME ) ) )
        );
    }

    protected SQLBooleanCreator getOperator( Predicate<Composite> predicate )
    {
        return this.findFromLookupTables( SQL_OPERATORS, null, predicate, false );
    }

    protected JoinType
    getTableJoinStyle( Predicate<Composite> predicate, Boolean negationActive )
    {
        return this.findFromLookupTables( JOIN_STYLES, NEGATED_JOIN_STYLES, predicate,
                                          negationActive );
    }

    protected <ReturnType> ReturnType findFromLookupTables(
        Map<Class<? extends Predicate>, ReturnType> normal,
        Map<Class<? extends Predicate>, ReturnType> negated,
        Predicate<Composite> predicate, Boolean negationActive
    )
    {
        Class<? extends Predicate> predicateClass = predicate.getClass();
        ReturnType result = null;
        Set<Map.Entry<Class<? extends Predicate>, ReturnType>> entries = negationActive
                                                                         ? negated.entrySet()
                                                                         : normal.entrySet();
        for( Map.Entry<Class<? extends Predicate>, ReturnType> entry : entries )
        {
            if( entry.getKey().isAssignableFrom( predicateClass ) )
            {
                result = entry.getValue();
                break;
            }
        }

        if( result == null )
        {
            throw new UnsupportedOperationException( "Predicate [" + predicateClass.getName() + "] is not supported" );
        }

        return result;
    }

    protected QuerySpecificationBuilder
    getBuilderForPredicate( SQLVendor vendor, String tableAlias )
    {
        QueryFactory q = vendor.getQueryFactory();
        ColumnsFactory c = vendor.getColumnsFactory();
        QuerySpecificationBuilder result = q.querySpecificationBuilder();
        result
            .getSelect()
            .setSetQuantifier( SetQuantifier.DISTINCT )
            .addUnnamedColumns( c.colName( tableAlias, DBNames.ENTITY_TABLE_PK_COLUMN_NAME ),
                                c.colName( tableAlias, DBNames.ENTITY_TABLE_IDENTITY_COLUMN_NAME ) );

        return result;
    }

    protected String translateJavaRegexpToPGSQLRegexp( String javaRegexp )
    {
        // TODO
        // Yo dawg, I heard you like regular expressions, so we made a regexp about your regexp so
        // you can match while
        // you match!
        // Meaning, probably best way to translate java regexp into pg-sql regexp is by... regexp.
        return javaRegexp;
    }

    protected void processOrderBySegments( OrderBy[] orderBy, SQLVendor vendor,
                                           QuerySpecificationBuilder builder
    )
    {
        if( orderBy != null )
        {
            QNameInfo[] qNames = new QNameInfo[ orderBy.length ];

            QueryFactory q = vendor.getQueryFactory();
            ColumnsFactory c = vendor.getColumnsFactory();

            Integer tableIndex = 0;
            for( Integer idx = 0; idx < orderBy.length; ++idx )
            {
                if( orderBy[ idx ] != null )
                {
                    PropertyFunction<?> ref = orderBy[ idx ].property();
                    QualifiedName qName = QualifiedName.fromAccessor( ref.accessor() );
                    QNameInfo info = this._state.qNameInfos().get().get( qName );
                    qNames[ idx ] = info;
                    if( info == null )
                    {
                        throw new InternalError( "No qName info found for qName [" + qName + "]." );
                    }
                    tableIndex
                        = this.traversePropertyPath( ref, 0, tableIndex + 1, vendor, builder
                        .getFrom()
                        .getTableReferences().iterator().next(), JoinType.LEFT_OUTER );
                    Class<?> declaringType = ( (Member) ref.accessor() ).getDeclaringClass();
                    String colName;
                    Integer tableIdx;
                    if( Identity.class.equals( declaringType ) )
                    {
                        colName = DBNames.ENTITY_TABLE_IDENTITY_COLUMN_NAME;
                        tableIdx = tableIndex - 1;
                    }
                    else
                    {
                        colName = DBNames.QNAME_TABLE_VALUE_COLUMN_NAME;
                        tableIdx = tableIndex;
                    }
                    Ordering ordering = Ordering.ASCENDING;
                    if( orderBy[ idx ].order() == Order.DESCENDING )
                    {
                        ordering = Ordering.DESCENDING;
                    }
                    builder.getOrderBy().addSortSpecs(
                        q.sortSpec( c.colName( TABLE_NAME_PREFIX + tableIdx, colName ), ordering ) );
                }
            }
        }
    }

    protected Integer traversePropertyPath( PropertyFunction<?> reference, Integer lastTableIndex,
                                            Integer nextAvailableIndex, SQLVendor vendor, TableReferenceBuilder builder,
                                            JoinType joinStyle
    )
    {

        Stack<QualifiedName> qNameStack = new Stack<>();
        Stack<PropertyFunction<?>> refStack = new Stack<>();

        while( reference != null )
        {
            qNameStack.add( QualifiedName.fromAccessor( reference.accessor() ) );
            refStack.add( reference );
            if( reference.traversedProperty() == null
                && ( reference.traversedAssociation() != null
                     || reference.traversedManyAssociation() != null ) )
            {
                Integer lastAssoTableIndex = this.traverseAssociationPath(
                    new TraversedAssoOrManyAssoRef( reference ),
                    lastTableIndex, nextAvailableIndex, vendor, builder, joinStyle, true );
                if( lastAssoTableIndex > lastTableIndex )
                {
                    lastTableIndex = lastAssoTableIndex;
                    nextAvailableIndex = lastTableIndex + 1;
                }
            }

            reference = reference.traversedProperty();
        }

        PropertyFunction<?> prevRef = null;
        String schemaName = this._state.schemaName().get();
        TableReferenceFactory t = vendor.getTableReferenceFactory();
        BooleanFactory b = vendor.getBooleanFactory();
        ColumnsFactory c = vendor.getColumnsFactory();

        while( !qNameStack.isEmpty() )
        {
            QualifiedName qName = qNameStack.pop();
            PropertyFunction<?> ref = refStack.pop();
            if( !qName.type().equals( Identity.class.getName() ) )
            {
                QNameInfo info = this._state.qNameInfos().get().get( qName );
                if( info == null )
                {
                    throw new InternalError( "No qName info found for qName [" + qName + "]." );
                }

                String prevTableAlias = TABLE_NAME_PREFIX + lastTableIndex;
                String nextTableAlias = TABLE_NAME_PREFIX + nextAvailableIndex;
                TableReferenceByName nextTable = t.table( t.tableName( schemaName, info.getTableName() ),
                                                          t.tableAlias( nextTableAlias ) );
                // @formatter:off
                if( prevRef == null )
                {
                    builder.addQualifiedJoin(
                        joinStyle,
                        nextTable,
                        t.jc(
                            b.booleanBuilder(
                                b.eq(
                                    c.colName( prevTableAlias, DBNames.ENTITY_TABLE_PK_COLUMN_NAME ),
                                    c.colName( nextTableAlias, DBNames.ENTITY_TABLE_PK_COLUMN_NAME )
                                )
                            )
                                .and(
                                    b.isNull( c.colName( nextTableAlias, DBNames.QNAME_TABLE_PARENT_QNAME_COLUMN_NAME ) )
                                )
                                .createExpression()
                        )
                    );
                }
                else
                {
                    builder.addQualifiedJoin(
                        joinStyle,
                        nextTable,
                        t.jc(
                            b.booleanBuilder(
                                b.eq(
                                    c.colName( prevTableAlias, DBNames.ALL_QNAMES_TABLE_PK_COLUMN_NAME ),
                                    c.colName( nextTableAlias, DBNames.QNAME_TABLE_PARENT_QNAME_COLUMN_NAME ) )
                            )
                                .and(
                                    b.eq(
                                        c.colName( prevTableAlias, DBNames.ENTITY_TABLE_PK_COLUMN_NAME ),
                                        c.colName( nextTableAlias, DBNames.ENTITY_TABLE_PK_COLUMN_NAME )
                                    )
                                )
                                .createExpression()
                        )
                    );
                }
                // @formatter:on
                lastTableIndex = nextAvailableIndex;
                ++nextAvailableIndex;
                prevRef = ref;
            }
        }

        return lastTableIndex;
    }

    protected Integer traverseAssociationPath( TraversedAssoOrManyAssoRef reference,
                                               Integer lastTableIndex,
                                               Integer nextAvailableIndex,
                                               SQLVendor vendor,
                                               TableReferenceBuilder builder,
                                               JoinType joinStyle,
                                               Boolean includeLastTable
    )
    {
        Stack<QualifiedName> qNameStack = new Stack<>();
        TableReferenceFactory t = vendor.getTableReferenceFactory();
        BooleanFactory b = vendor.getBooleanFactory();
        ColumnsFactory c = vendor.getColumnsFactory();
        String schemaName = this._state.schemaName().get();

        while( reference._hasRefs )
        {
            qNameStack.add( QualifiedName.fromAccessor( reference.getAccessor() ) );
            reference = reference.getTraversedAssociation();
        }
        while( !qNameStack.isEmpty() )
        {
            QualifiedName qName = qNameStack.pop();
            QNameInfo info = this._state.qNameInfos().get().get( qName );
            if( info == null )
            {
                throw new InternalError( "No qName info found for qName [" + qName + "]." );
            }
            // @formatter:off
            builder.addQualifiedJoin(
                joinStyle,
                t.table( t.tableName( schemaName, info.getTableName() ), t.tableAlias( TABLE_NAME_PREFIX
                                                                                       + nextAvailableIndex ) ),
                t.jc(
                    b.eq(
                        c.colName( TABLE_NAME_PREFIX + lastTableIndex, DBNames.ENTITY_TABLE_PK_COLUMN_NAME ),
                        c.colName( TABLE_NAME_PREFIX + nextAvailableIndex, DBNames.ENTITY_TABLE_PK_COLUMN_NAME ) )
                ) );
            lastTableIndex = nextAvailableIndex;
            ++nextAvailableIndex;
            if( includeLastTable || !qNameStack.isEmpty() )
            {
                builder.addQualifiedJoin(
                    joinStyle,
                    t.table( t.tableName( schemaName, DBNames.ENTITY_TABLE_NAME ), t.tableAlias( TABLE_NAME_PREFIX + nextAvailableIndex ) ),
                    t.jc(
                        b.eq(
                            c.colName( TABLE_NAME_PREFIX + lastTableIndex, DBNames.QNAME_TABLE_VALUE_COLUMN_NAME ),
                            c.colName( TABLE_NAME_PREFIX + nextAvailableIndex, DBNames.ENTITY_TABLE_PK_COLUMN_NAME )
                        )
                    )
                );
                lastTableIndex = nextAvailableIndex;
                ++nextAvailableIndex;
            }
            // @formatter:on
        }

        return lastTableIndex;
    }

    protected List<Integer> getEntityTypeIDs( Class<?> entityType )
    {
        List<Integer> result = new ArrayList<>();
        for( Map.Entry<String, Integer> entry : this._state.entityTypePKs().get().entrySet() )
        {
            Class<?> clazz = null;
            try
            {
                clazz = Class.forName( entry.getKey() );
            }
            catch( Throwable t )
            {
                // Ignore
            }
            if( clazz != null && entityType.isAssignableFrom( clazz ) )
            {
                result.add( entry.getValue() );
            }
        }

        return result;
    }

    // TODO refactor this monster of a method to something more understandable
    protected Integer modifyFromClauseAndWhereClauseToGetValue(
        final QualifiedName qName,
        Object value,
        final Predicate<Composite> predicate, final Boolean negationActive,
        final Integer currentTableIndex,
        final ModifiableInt maxTableIndex, final String columnName,
        final String collectionPath,
        final SQLVendor vendor, final BooleanBuilder whereClause,
        final BooleanBuilder afterWhere,
        final TableReferenceBuilder fromClause, final GroupByBuilder groupBy,
        final BooleanBuilder having,
        final List<QNameJoin> qNameJoins, Map<String, Object> variables,
        final List<Object> values, final List<Integer> valueSQLTypes
    )
    {
        if( value instanceof Variable )
        {
            value = variables.get( ( (Variable) value ).variableName() );
        }

        final String schemaName = this._state.schemaName().get();
        Integer result = 1;

        final BooleanFactory b = vendor.getBooleanFactory();
        final LiteralFactory l = vendor.getLiteralFactory();
        final ColumnsFactory c = vendor.getColumnsFactory();
        final QueryFactory q = vendor.getQueryFactory();
        final TableReferenceFactory t = vendor.getTableReferenceFactory();

        if( value instanceof Collection<?> )
        {
            // Collection
            Integer collectionIndex = 0;
            Boolean collectionIsSet = value instanceof Set<?>;
            Boolean topLevel = collectionPath.equals( DBNames.QNAME_TABLE_COLLECTION_PATH_TOP_LEVEL_NAME );
            String collTable = TABLE_NAME_PREFIX + currentTableIndex;
            String collCol = DBNames.QNAME_TABLE_COLLECTION_PATH_COLUMN_NAME;
            ColumnReferenceByName collColExp = c.colName( collTable, collCol );

            BooleanBuilder collectionCondition = b.booleanBuilder();

            if( topLevel && negationActive )
            {
                afterWhere
                    .and( b
                              .booleanBuilder(
                                  b.neq( collColExp,
                                         l.s( DBNames.QNAME_TABLE_COLLECTION_PATH_TOP_LEVEL_NAME ) ) )
                              .or( b.isNull( collColExp ) ).createExpression() );
            }

            Integer totalItemsProcessed = 0;
            for( Object item : (Collection<?>) value )
            {
                String path = collectionPath + DBNames.QNAME_TABLE_COLLECTION_PATH_SEPARATOR
                              + ( collectionIsSet ? "*{1,}" : collectionIndex );
                Boolean isCollection = ( item instanceof Collection<?> );
                BooleanBuilder newWhere = b.booleanBuilder();
                if( !isCollection )
                {
                    newWhere.reset( b.regexp( collColExp, l.s( path ) ) );
                }
                totalItemsProcessed
                    = totalItemsProcessed
                      + modifyFromClauseAndWhereClauseToGetValue( qName, item, predicate,
                                                                  negationActive,
                                                                  currentTableIndex, maxTableIndex, columnName, path, vendor,
                                                                  newWhere, afterWhere, fromClause,
                                                                  groupBy, having, qNameJoins, variables, values, valueSQLTypes );

                ++collectionIndex;
                collectionCondition.or( newWhere.createExpression() );
            }
            result = totalItemsProcessed;

            if( topLevel )
            {
                if( totalItemsProcessed == 0 )
                {
                    collectionCondition.and( b.isNotNull( collColExp ) )
                        .and(
                            b.eq( collColExp,
                                  l.l( DBNames.QNAME_TABLE_COLLECTION_PATH_TOP_LEVEL_NAME ) ) );
                }
                else if( !negationActive )
                {
                    groupBy.addGroupingElements( q.groupingElement( c.colName( TABLE_NAME_PREFIX
                                                                               + currentTableIndex,
                                                                               DBNames.ENTITY_TABLE_PK_COLUMN_NAME ) ) );
                    having
                        .and( b.eq(
                            l.func( SQLFunctions.COUNT,
                                    c.colName( TABLE_NAME_PREFIX + currentTableIndex,
                                               DBNames.QNAME_TABLE_VALUE_COLUMN_NAME ) ),
                            l.n( totalItemsProcessed ) ) );
                }
            }

            whereClause.and( collectionCondition.createExpression() );
        }
        else if( value instanceof ValueComposite )
        {
            // Visit all properties with recursion and make joins as necessary
            // @formatter:off
            ZestAPI.FUNCTION_COMPOSITE_INSTANCE_OF.apply( (ValueComposite) value )
                .state().properties()
                .forEach( property -> {
                    Boolean qNameJoinDone = false;
                    Integer sourceIndex = maxTableIndex.getInt();
                    Integer targetIndex = sourceIndex + 1;
                    for( QNameJoin join : qNameJoins )
                    {
                        if( join.getSourceQName().equals( qName ) )
                        {
                            sourceIndex = join.getSourceTableIndex();
                            if( join.getTargetQName().equals( spi.propertyDescriptorFor( property ).qualifiedName() ) )
                            {
                                // This join has already been done once
                                qNameJoinDone = true;
                                targetIndex = join.getTargetTableIndex();
                                break;
                            }
                        }
                    }

                    if( !qNameJoinDone )
                    {
                        // @formatter:off
                        QNameInfo info = _state.qNameInfos()
                            .get()
                            .get( spi.propertyDescriptorFor( property ).qualifiedName() );
                        String prevTableName = TABLE_NAME_PREFIX + sourceIndex;
                        String nextTableName = TABLE_NAME_PREFIX + targetIndex;
                        fromClause.addQualifiedJoin(
                            JoinType.LEFT_OUTER,
                            t.table( t.tableName( schemaName, info.getTableName() ), t.tableAlias( TABLE_NAME_PREFIX + targetIndex ) ),
                            t.jc(
                                b.booleanBuilder( b.eq(
                                    c.colName( prevTableName, DBNames.ALL_QNAMES_TABLE_PK_COLUMN_NAME ),
                                    c.colName( nextTableName, DBNames.QNAME_TABLE_PARENT_QNAME_COLUMN_NAME )
                                ) )
                                    .and( b.eq(
                                        c.colName( prevTableName, DBNames.ENTITY_TABLE_PK_COLUMN_NAME ),
                                        c.colName( nextTableName, DBNames.ENTITY_TABLE_PK_COLUMN_NAME )
                                    ) ).createExpression()
                            )
                        );
                        // @formatter:on

                        qNameJoins.add( new QNameJoin( qName, spi.propertyDescriptorFor( property )
                            .qualifiedName(), sourceIndex, targetIndex ) );
                        maxTableIndex.setInt( maxTableIndex.getInt() + 1 );
                    }
                    modifyFromClauseAndWhereClauseToGetValue( spi.propertyDescriptorFor( property )
                                                                  .qualifiedName(), property.get(), predicate, negationActive,
                                                              targetIndex, maxTableIndex, columnName, collectionPath, vendor, whereClause,
                                                              afterWhere,
                                                              fromClause, groupBy, having, qNameJoins, variables, values, valueSQLTypes );
                } );

            // @formatter:on
        }
        else
        {
            // Primitive
            ColumnReferenceByName valueCol = c.colName( TABLE_NAME_PREFIX + currentTableIndex, columnName );
            if( value == null )
            {
                whereClause.and( b.isNull( valueCol ) );
            }
            else
            {
                Object dbValue = value;
                if( Enum.class.isAssignableFrom( value.getClass() ) )
                {
                    dbValue = this._state.enumPKs().get().get( value.getClass().getName() );
                }
                whereClause.and( b.and( b.isNotNull( valueCol ),
                                        this.getOperator( predicate ).getExpression( b, valueCol, l.param() ) ) );
                values.add( dbValue );
                valueSQLTypes.add( _typeHelper.getSQLType( value ) );
                LOGGER.info( TABLE_NAME_PREFIX + currentTableIndex + "." + columnName + " is " + dbValue );
            }
        }

        return result;
    }
}
