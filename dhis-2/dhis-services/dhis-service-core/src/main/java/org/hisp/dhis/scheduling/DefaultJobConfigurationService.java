package org.hisp.dhis.scheduling;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.schema.NodePropertyIntrospectorService;
import org.hisp.dhis.schema.Property;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.scheduling.JobType.values;

/**
 * @author Henning Håkonsen
 */
@Service( "jobConfigurationService" )
public class DefaultJobConfigurationService
    implements JobConfigurationService
{
    private Log log = LogFactory.getLog( DefaultJobConfigurationService.class );

    private final IdentifiableObjectStore<JobConfiguration> jobConfigurationStore;

    public DefaultJobConfigurationService(
        @Qualifier( "org.hisp.dhis.scheduling.JobConfigurationStore" ) IdentifiableObjectStore<JobConfiguration> jobConfigurationStore )
    {
        checkNotNull( jobConfigurationStore );

        this.jobConfigurationStore = jobConfigurationStore;
    }

    @Override
    @Transactional
    public long addJobConfiguration( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            jobConfigurationStore.save( jobConfiguration );
        }
        return jobConfiguration.getId();
    }

    @Override
    @Transactional
    public void addJobConfigurations( List<JobConfiguration> jobConfigurations )
    {
        jobConfigurations.forEach( jobConfiguration -> jobConfigurationStore.save( jobConfiguration ) );
    }

    @Override
    @Transactional
    public long updateJobConfiguration( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            jobConfigurationStore.update( jobConfiguration );
        }

        return jobConfiguration.getId();
    }

    @Override
    @Transactional
    public void deleteJobConfiguration( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            jobConfigurationStore.delete( jobConfigurationStore.getByUid( jobConfiguration.getUid() ) );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public JobConfiguration getJobConfigurationByUid( String uid )
    {
        return jobConfigurationStore.getByUid( uid );
    }

    @Override
    @Transactional(readOnly = true)
    public JobConfiguration getJobConfiguration( long jobId )
    {
        return jobConfigurationStore.get( jobId );
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobConfiguration> getAllJobConfigurations()
    {
        return jobConfigurationStore.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Property>> getJobParametersSchema()
    {
        Map<String, Map<String, Property>> propertyMap = Maps.newHashMap();

        for ( JobType jobType : values() )
        {
            Map<String, Property> jobParameters = new LinkedHashMap<>();

            if ( !jobType.isConfigurable() )
            {
                continue;
            }

            Class<?> clazz = jobType.getJobParameters();
            if ( clazz == null )
            {
                propertyMap.put( jobType.name(), new LinkedHashMap<>() );
                continue;
            }

            final Set<String> propertyNames = Stream.of( PropertyUtils.getPropertyDescriptors( clazz ) )
                .filter( pd -> pd.getReadMethod() != null && pd.getWriteMethod() != null && pd.getReadMethod().getAnnotation( JsonProperty.class ) != null )
                .map( PropertyDescriptor::getName )
                .collect( Collectors.toSet() );

            for ( Field field : Stream.of( clazz.getDeclaredFields() ).filter( f -> propertyNames.contains( f.getName() ) ).collect( Collectors.toList() ) )
            {
                Property property = new Property( Primitives.wrap( field.getType() ), null, null );
                property.setName( field.getName() );
                property.setFieldName( prettyPrint( field.getName() ) );

                try
                {
                    field.setAccessible( true );
                    property.setDefaultValue( field.get( jobType.getJobParameters().newInstance() ) );
                }
                catch ( IllegalAccessException | InstantiationException e )
                {
                    log.error( "Fetching default value for JobParameters properties failed for property: " + field.getName(), e );
                }

                String relativeApiElements = jobType.getRelativeApiElements() != null ?
                    jobType.getRelativeApiElements().get( field.getName() ) : "";

                if ( relativeApiElements != null && !relativeApiElements.equals( "" ) )
                {
                    property.setRelativeApiEndpoint( relativeApiElements );
                }

                if ( Collection.class.isAssignableFrom( field.getType() ) )
                {
                    property = new NodePropertyIntrospectorService()
                        .setPropertyIfCollection( property, field, clazz );
                }

                jobParameters.put( property.getName(), property );
            }
            propertyMap.put( jobType.name(), jobParameters );
        }

        return propertyMap;
    }

    private String prettyPrint( String field )
    {
        List<String> fieldStrings = Arrays.stream( field.split( "(?=[A-Z])" ) ).map( String::toLowerCase )
            .collect( Collectors.toList() );

        fieldStrings.set( 0, fieldStrings.get( 0 ).substring( 0, 1 ).toUpperCase() + fieldStrings.get( 0 ).substring( 1 ) );

        return String.join( " ", fieldStrings );
    }
}
