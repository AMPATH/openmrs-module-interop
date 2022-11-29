/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.dhpevents.api;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import java.util.Set;

import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.event.Event;
import org.openmrs.event.EventListener;
import org.openmrs.module.DaemonToken;

/**
 * A wrapper interface for all DHP events
 */
public interface Subscribable<E extends OpenmrsObject & Auditable> extends EventListener {
	
	Class<?> clazz();
	
	Event.Action action();
	
	void setDaemonToken(@NotNull DaemonToken daemonToken);
	
	DaemonToken getDaemonToken();
	
	Publisher getPublisher();
	
	Set<Class<? extends Publisher>> getPublishers();
	
	void publish(@NotNull IAnyResource resource, @Nullable IParser parser);
}
