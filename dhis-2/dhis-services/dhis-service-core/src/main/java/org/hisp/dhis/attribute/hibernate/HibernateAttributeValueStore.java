package org.hisp.dhis.attribute.hibernate;

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

import org.hibernate.SessionFactory;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.attribute.AttributeValueStore;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Repository( "org.hisp.dhis.attribute.AttributeValueStore" )
public class HibernateAttributeValueStore
    extends HibernateGenericStore<AttributeValue>
    implements AttributeValueStore
{
    public HibernateAttributeValueStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate )
    {
        super( sessionFactory, jdbcTemplate, AttributeValue.class, true );
    }

    @Override
    public List<AttributeValue> getAllByAttributes( List<Attribute> attributes )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> root.get( "attribtue" ).in( attributes ) ) );
    }

    @Override
    public List<AttributeValue> getAllByAttribute( Attribute attribute )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "attribute" ), attribute ) ) );
    }

    @Override
    public List<AttributeValue> getAllByAttributeAndValue( Attribute attribute, String value )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "attribute" ), attribute ) )
            .addPredicate( root -> builder.equal( root.get( "value" ), value ) ) );
    }

    @Override
    public <T extends IdentifiableObject> boolean isAttributeValueUnique( T object, AttributeValue attributeValue )
    {
        List<AttributeValue> values = getAllByAttributeAndValue( attributeValue.getAttribute(), attributeValue.getValue() );
        return values.isEmpty() || (values.size() == 1 && object != null && object.getAttributeValues().contains( values.get( 0 ) ));
    }
}
