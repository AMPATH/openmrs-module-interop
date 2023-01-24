/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.api.observers;

import javax.jms.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.Event;
import org.openmrs.module.interop.api.Subscribable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component("interop.encounterCreationObserver")
public class EncounterObserver extends BaseObserver implements Subscribable<Encounter> {
	
	@Autowired
	@Qualifier("fhirR4")
	private FhirContext fhirContext;
	
	@Override
	public Class<?> clazz() {
		return Encounter.class;
	}
	
	@Override
	public List<Event.Action> actions() {
		return Arrays.asList(Event.Action.UPDATED, Event.Action.CREATED, Event.Action.VOIDED);
	}
	
	@Override
	public void onMessage(Message message) {
		log.debug("Encounter message received {}", message);
		Daemon.runInDaemonThread(() -> preProcessEncounterMessage(processMessage(message)), getDaemonToken());
	}
	
	protected void preProcessEncounterMessage(Optional<String> encounterUuid) {
		//Create bundle
		encounterUuid.ifPresent(uuid -> {
			Encounter encounter = Context.getEncounterService().getEncounterByUuid(uuid);
			
			// Get observations & other referenced resources from encounter convert to FHIR then add to the bundle.
			
		});
		
		// now publish the bundle
		// publish(bundle, context.newJsonParser());
	}
}
