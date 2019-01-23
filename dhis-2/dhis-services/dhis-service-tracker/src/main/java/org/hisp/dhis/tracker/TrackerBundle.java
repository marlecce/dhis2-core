package org.hisp.dhis.tracker;

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

import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerBundle
{
    /**
     * User to use for import job.
     */
    private User user;

    /**
     * Should import be imported or just validated.
     */
    private TrackerBundleMode importMode = TrackerBundleMode.COMMIT;

    /**
     * What identifiers to match on.
     */
    private TrackerPreheatIdentifier identifier = TrackerPreheatIdentifier.UID;

    /**
     * Sets import strategy (create, update, etc).
     */
    private TrackerImportStrategy importStrategy = TrackerImportStrategy.CREATE;

    /**
     * Should import be treated as a atomic import (all or nothing).
     */
    private AtomicMode atomicMode = AtomicMode.ALL;

    /**
     * Flush for every object or per type.
     */
    private FlushMode flushMode = FlushMode.AUTO;

    /**
     * Skip validation of objects (not recommended).
     */
    private boolean skipValidation;

    /**
     * Tracked entities to import.
     */
    private List<TrackedEntityInstance> trackedEntities = new ArrayList<>();

    /**
     * Enrollments to import.
     */
    private List<Enrollment> enrollments = new ArrayList<>();

    /**
     * Events to import.
     */
    private List<Event> events = new ArrayList<>();

    public TrackerBundle()
    {
    }

    public User getUser()
    {
        return user;
    }

    public TrackerBundle setUser( User user )
    {
        this.user = user;
        return this;
    }

    public TrackerBundleMode getImportMode()
    {
        return importMode;
    }

    public TrackerBundle setImportMode( TrackerBundleMode importMode )
    {
        this.importMode = importMode;
        return this;
    }

    public TrackerPreheatIdentifier getIdentifier()
    {
        return identifier;
    }

    public TrackerBundle setIdentifier( TrackerPreheatIdentifier identifier )
    {
        this.identifier = identifier;
        return this;
    }

    public TrackerImportStrategy getImportStrategy()
    {
        return importStrategy;
    }

    public TrackerBundle setImportStrategy( TrackerImportStrategy importStrategy )
    {
        this.importStrategy = importStrategy;
        return this;
    }

    public AtomicMode getAtomicMode()
    {
        return atomicMode;
    }

    public TrackerBundle setAtomicMode( AtomicMode atomicMode )
    {
        this.atomicMode = atomicMode;
        return this;
    }

    public FlushMode getFlushMode()
    {
        return flushMode;
    }

    public TrackerBundle setFlushMode( FlushMode flushMode )
    {
        this.flushMode = flushMode;
        return this;
    }

    public boolean isSkipValidation()
    {
        return skipValidation;
    }

    public TrackerBundle setSkipValidation( boolean skipValidation )
    {
        this.skipValidation = skipValidation;
        return this;
    }

    public List<TrackedEntityInstance> getTrackedEntities()
    {
        return trackedEntities;
    }

    public TrackerBundle setTrackedEntities( List<TrackedEntityInstance> trackedEntities )
    {
        this.trackedEntities = trackedEntities;
        return this;
    }

    public TrackerBundle addTrackedEntity( TrackedEntityInstance... trackedEntity )
    {
        trackedEntities.addAll( Arrays.asList( trackedEntity ) );
        return this;
    }

    public List<Enrollment> getEnrollments()
    {
        return enrollments;
    }

    public TrackerBundle setEnrollments( List<Enrollment> enrollments )
    {
        this.enrollments = enrollments;
        return this;
    }

    public TrackerBundle addEnrollment( Enrollment... enrollment )
    {
        enrollments.addAll( Arrays.asList( enrollment ) );
        return this;
    }

    public List<Event> getEvents()
    {
        return events;
    }

    public TrackerBundle setEvents( List<Event> events )
    {
        this.events = events;
        return this;
    }

    public TrackerBundle addEvent( Event... event )
    {
        events.addAll( Arrays.asList( event ) );
        return this;
    }
}