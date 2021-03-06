/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.analytics.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.ColumnDataType;

/**
 * @author Luciano Fiandesio
 */
public class AnalyticsTableAsserter
{
    // the table to check //

    private AnalyticsTable table;

    private int columnsSize;

    private List<AnalyticsTableColumn> defaultColumns;

    private List<AnalyticsTableColumn> columns;

    private AnalyticsTableType tableType;

    private AnalyticsTableAsserter()
    {
    }

    public void verify()
    {
        // verify column size
        assertThat( table.getDimensionColumns(), hasSize( columnsSize ) );
        assertThat( table.getTableType(), is( tableType ) );

        // verify default columns

        Map<String, AnalyticsTableColumn> tableColumnMap = Stream
            .concat( table.getDimensionColumns().stream(), table.getValueColumns().stream() )
            .collect( Collectors.toMap( AnalyticsTableColumn::getName, c -> c ) );

        for ( AnalyticsTableColumn col : defaultColumns )
        {
            if ( !tableColumnMap.containsKey( col.getName() ) )
            {
                fail( "Default column [" + col.getName() + "] is missing" );
            }
            else
            {
                new AnalyticsColumnAsserter.Builder( col ).build().verify( tableColumnMap.get( col.getName() ) );
            }
        }

        // verify additional columns
        for ( AnalyticsTableColumn col : columns )
        {
            if ( !tableColumnMap.containsKey( col.getName() ) )
            {
                fail( "Column [" + col.getName() + "] is missing" );
            }
            else
            {
                new AnalyticsColumnAsserter.Builder( col ).build().verify( tableColumnMap.get( col.getName() ) );
            }
        }
    }

    public static class Builder
    {
        private AnalyticsTable _table;

        private int _columnSize;

        private List<AnalyticsTableColumn> _defaultColumns;

        private List<AnalyticsTableColumn> _columns = new ArrayList<>();

        private AnalyticsTableType _tableType;

        public Builder( AnalyticsTable analyticsTable )
        {
            _table = analyticsTable;
        }

        public Builder withColumnSize( int columnSize )
        {
            _columnSize = columnSize;
            return this;
        }

        public Builder withDefaultColumns( List<AnalyticsTableColumn> defaultColumns )
        {

            _defaultColumns = defaultColumns;
            return this;
        }

        public Builder withTableType( AnalyticsTableType analyticsTableType )
        {
            _tableType = analyticsTableType;
            return this;
        }

        public Builder addColumn( String name, ColumnDataType dataType, String alias, Date created )
        {
            AnalyticsTableColumn col = new AnalyticsTableColumn( quote( name ), dataType, alias + quote( name ) );
            col.withCreated( created );
            this._columns.add( col );

            return this;
        }

        public Builder addColumn( String name, ColumnDataType dataType, String alias )
        {
            AnalyticsTableColumn col = new AnalyticsTableColumn( quote( name ), dataType, alias);
            this._columns.add( col );

            return this;
        }

        public Builder addColumns( List<AnalyticsTableColumn> columns )
        {
            this._columns.addAll( columns );

            return this;
        }

        public AnalyticsTableAsserter build()
        {
            // verify

            if ( _tableType == null )
            {
                fail( "Missing table type" );
            }

            AnalyticsTableAsserter asserter = new AnalyticsTableAsserter();
            asserter.table = _table;
            asserter.columnsSize = _columnSize;
            asserter.defaultColumns = _defaultColumns;
            asserter.tableType = _tableType;
            asserter.columns = _columns;

            return asserter;
        }
    }
}
