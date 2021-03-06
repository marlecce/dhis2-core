package org.hisp.dhis.program;

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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;

import java.util.*;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.parser.expression.*;
import org.hisp.dhis.parser.expression.item.ItemConstant;
import org.hisp.dhis.parser.expression.literal.SqlLiteral;
import org.hisp.dhis.program.function.*;
import org.hisp.dhis.program.item.ProgramItemAttribute;
import org.hisp.dhis.program.item.ProgramItemStageElement;
import org.hisp.dhis.program.variable.*;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Service( "org.hisp.dhis.program.ProgramIndicatorService" )
public class DefaultProgramIndicatorService
    implements ProgramIndicatorService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramIndicatorStore programIndicatorStore;

    private ProgramStageService programStageService;

    private DataElementService dataElementService;

    private TrackedEntityAttributeService attributeService;

    private ConstantService constantService;

    private StatementBuilder statementBuilder;

    private IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore;

    private I18nManager i18nManager;

    private RelationshipTypeService relationshipTypeService;

    public DefaultProgramIndicatorService( ProgramIndicatorStore programIndicatorStore,
        ProgramStageService programStageService, DataElementService dataElementService,
        TrackedEntityAttributeService attributeService, ConstantService constantService, StatementBuilder statementBuilder,
        @Qualifier("org.hisp.dhis.program.ProgramIndicatorGroupStore") IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore,
        I18nManager i18nManager, RelationshipTypeService relationshipTypeService )
    {
        checkNotNull( programIndicatorStore );
        checkNotNull( programStageService );
        checkNotNull( dataElementService );
        checkNotNull( attributeService );
        checkNotNull( constantService );
        checkNotNull( statementBuilder );
        checkNotNull( programIndicatorGroupStore );
        checkNotNull( i18nManager );
        checkNotNull( relationshipTypeService );

        this.programIndicatorStore = programIndicatorStore;
        this.programStageService = programStageService;
        this.dataElementService = dataElementService;
        this.attributeService = attributeService;
        this.constantService = constantService;
        this.statementBuilder = statementBuilder;
        this.programIndicatorGroupStore = programIndicatorGroupStore;
        this.i18nManager = i18nManager;
        this.relationshipTypeService = relationshipTypeService;
    }

    public final static ImmutableMap<Integer, ExprFunction> PROGRAM_INDICATOR_FUNCTIONS = ImmutableMap.<Integer, ExprFunction>builder()

        // Common functions

        .putAll( COMMON_EXPRESSION_FUNCTIONS )

        // Program variables

        .put( V_ANALYTICS_PERIOD_END, new vAnalyticsPeriodEnd() )
        .put( V_ANALYTICS_PERIOD_START, new vAnalyticsPeriodStart() )
        .put( V_CREATION_DATE, new vCreationDate() )
        .put( V_CURRENT_DATE, new vCurrentDate() )
        .put( V_DUE_DATE, new vDueDate() )
        .put( V_ENROLLMENT_COUNT, new vEnrollmentCount() )
        .put( V_ENROLLMENT_DATE, new vEnrolmentDate() )
        .put( V_ENROLLMENT_STATUS, new vEnrollmentStatus() )
        .put( V_EVENT_COUNT, new vEventCount() )
        .put( V_EXECUTION_DATE, new vEventDate() ) // Same as event date
        .put( V_EVENT_DATE, new vEventDate() )
        .put( V_INCIDENT_DATE, new vIncidentDate() )
        .put( V_ORG_UNIT_COUNT, new vOrgUnitCount() )
        .put( V_PROGRAM_STAGE_ID, new vProgramStageId() )
        .put( V_PROGRAM_STAGE_NAME, new vProgramStageName() )
        .put( V_SYNC_DATE, new vSyncDate() )
        .put( V_TEI_COUNT, new vTeiCount() )
        .put( V_VALUE_COUNT, new vValueCount() )
        .put( V_ZERO_POS_VALUE_COUNT, new vZeroPosValueCount() )

        // Program functions

        .put( D2_CONDITION, new d2Condition() )
        .put( D2_COUNT, new d2Count() )
        .put( D2_COUNT_IF_CONDITION, new d2CountIfCondition() )
        .put( D2_COUNT_IF_VALUE, new d2CountIfValue() )
        .put( D2_DAYS_BETWEEN, new d2DaysBetween() )
        .put( D2_HAS_VALUE, new d2HasValue() )
        .put( D2_MINUTES_BETWEEN, new d2MinutesBetween() )
        .put( D2_MONTHS_BETWEEN, new d2MonthsBetween() )
        .put( D2_OIZP, new d2Oizp() )
        .put( D2_RELATIONSHIP_COUNT, new d2RelationshipCount() )
        .put( D2_WEEKS_BETWEEN, new d2WeeksBetween() )
        .put( D2_YEARS_BETWEEN, new d2YearsBetween() )
        .put( D2_ZING, new d2Zing() )
        .put( D2_ZPVC, new d2Zpvc() )

        // Program functions for custom aggregation

        .put( AVG, new aggAvg() )
        .put( COUNT, new aggCount() )
        .put( MAX, new aggMax() )
        .put( MIN, new aggMin() )
        .put( STDDEV, new aggStddev() )
        .put( SUM, new aggSum() )
        .put( VARIANCE, new aggVariance() )

        .build();

    public final static ImmutableMap<Integer, ExprItem> PROGRAM_INDICATOR_ITEMS = ImmutableMap.<Integer, ExprItem>builder()

        .put( C_BRACE, new ItemConstant() )
        .put( HASH_BRACE, new ProgramItemStageElement() )
        .put( A_BRACE, new ProgramItemAttribute() )

        .build();
    // -------------------------------------------------------------------------
    // ProgramIndicator CRUD
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.save( programIndicator );
        return programIndicator.getId();
    }

    @Override
    @Transactional
    public void updateProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.update( programIndicator );
    }

    @Override
    @Transactional
    public void deleteProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.delete( programIndicator );
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramIndicator getProgramIndicator( long id )
    {
        return programIndicatorStore.get( id );
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramIndicator getProgramIndicator( String name )
    {
        return programIndicatorStore.getByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramIndicator getProgramIndicatorByUid( String uid )
    {
        return programIndicatorStore.getByUid( uid );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramIndicator> getAllProgramIndicators()
    {
        return programIndicatorStore.getAll();
    }

    // -------------------------------------------------------------------------
    // ProgramIndicator logic
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Deprecated public String getUntypedDescription( String expression )
    {
        return getDescription( expression, null );
    }

    @Override
    @Transactional(readOnly = true)
    public String getExpressionDescription( String expression )
    {
        return getDescription( expression, Double.class );
    }

    @Override
    @Transactional(readOnly = true)
    public String getFilterDescription( String expression )
    {
        return getDescription( expression, Boolean.class );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean expressionIsValid( String expression )
    {
        return isValid( expression, Double.class );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean filterIsValid( String filter )
    {
        return isValid( filter, Boolean.class );
    }

    @Override
    @Transactional(readOnly = true)
    public void validate( String expression, Class<?> clazz, Map<String, String> itemDescriptions )
    {
        CommonExpressionVisitor visitor = newVisitor( FUNCTION_EVALUATE, ITEM_GET_DESCRIPTIONS );

        castClass( clazz, Parser.visit( expression, visitor ) );

        itemDescriptions.putAll( visitor.getItemDescriptions() );
    }

    @Override
    @Transactional(readOnly = true)
    public String getAnalyticsSql( String expression, ProgramIndicator programIndicator, Date startDate, Date endDate )
    {
        if ( expression == null )
        {
            return null;
        }

        Set<String> uids = getDataElementAndAttributeIdentifiers( expression, programIndicator.getAnalyticsType() );

        CommonExpressionVisitor visitor = newVisitor( FUNCTION_GET_SQL, ITEM_GET_SQL );

        visitor.setExpressionLiteral( new SqlLiteral() );
        visitor.setProgramIndicator( programIndicator );
        visitor.setReportingStartDate( startDate );
        visitor.setReportingEndDate( endDate );
        visitor.setDataElementAndAttributeIdentifiers( uids );

        return castString( Parser.visit( expression, visitor ) );
    }

    @Override
    @Transactional(readOnly = true)
    public String getAnyValueExistsClauseAnalyticsSql( String expression, AnalyticsType analyticsType )
    {
        if ( expression == null )
        {
            return null;
        }

        try
        {
            Set<String> uids = getDataElementAndAttributeIdentifiers( expression, analyticsType );

            if ( uids.isEmpty() )
            {
                return null;
            }

            String sql = StringUtils.EMPTY;

            for ( String uid : uids )
            {
                sql += statementBuilder.columnQuote( uid ) + " is not null or ";
            }

            return TextUtils.removeLastOr( sql ).trim();
        }
        catch ( ParserException e )
        {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // ProgramIndicatorGroup
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        programIndicatorGroupStore.save( programIndicatorGroup );
        return programIndicatorGroup.getId();
    }

    @Override
    @Transactional
    public void updateProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        programIndicatorGroupStore.update( programIndicatorGroup );
    }

    @Override
    @Transactional
    public void deleteProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        programIndicatorGroupStore.delete( programIndicatorGroup );
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramIndicatorGroup getProgramIndicatorGroup( long id )
    {
        return programIndicatorGroupStore.get( id );
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramIndicatorGroup getProgramIndicatorGroup( String uid )
    {
        return programIndicatorGroupStore.getByUid( uid );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramIndicatorGroup> getAllProgramIndicatorGroups()
    {
        return programIndicatorGroupStore.getAll();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private CommonExpressionVisitor newVisitor( ExprFunctionMethod functionMethod, ExprItemMethod itemMethod )
    {
        return CommonExpressionVisitor.newBuilder()
            .withFunctionMap( PROGRAM_INDICATOR_FUNCTIONS )
            .withItemMap( PROGRAM_INDICATOR_ITEMS )
            .withFunctionMethod( functionMethod )
            .withItemMethod( itemMethod )
            .withConstantService( constantService )
            .withProgramIndicatorService( this )
            .withProgramStageService( programStageService )
            .withDataElementService( dataElementService )
            .withAttributeService( attributeService )
            .withRelationshipTypeService( relationshipTypeService )
            .withStatementBuilder( statementBuilder )
            .withI18n( i18nManager.getI18n() )
            .buildForProgramIndicatorExpressions();
    }

    private String getDescription( String expression, Class<?> clazz )
    {
        Map<String, String> itemDescriptions = new HashMap<>();

        validate( expression, clazz, itemDescriptions );

        String description = expression;

        for ( Map.Entry<String, String> entry : itemDescriptions.entrySet() )
        {
            description = description.replace( entry.getKey(), entry.getValue() );
        }

        return description;
    }

    private boolean isValid( String expression, Class<?> clazz )
    {
        if ( expression != null )
        {
            try
            {
                validate( expression, clazz, new HashMap<>() );
            }
            catch ( ParserException e )
            {
                return false;
            }
        }

        return true;
    }

    private Set<String> getDataElementAndAttributeIdentifiers( String expression, AnalyticsType analyticsType )
    {
        Set<String> items = new HashSet<>();

        ProgramElementsAndAttributesCollecter listener = new ProgramElementsAndAttributesCollecter( items, analyticsType );

        Parser.listen( expression, listener );

        return items;
    }
}
