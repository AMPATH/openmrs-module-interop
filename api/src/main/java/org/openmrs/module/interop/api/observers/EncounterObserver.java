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
import javax.validation.constraints.NotNull;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.Event;
import org.openmrs.module.interop.api.Subscribable;
import org.openmrs.module.interop.api.metadata.EventMetadata;
import org.openmrs.module.interop.utils.ObserverUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component("interop.encounterCreationObserver")
public class EncounterObserver extends BaseObserver implements Subscribable<Encounter> {
	
	@Override
	public Class<?> clazz() {
		return Encounter.class;
	}
	
	@Override
	public List<Event.Action> actions() {
		return ObserverUtils.voidableEntityActions();
	}
	
	@Override
	public void onMessage(Message message) {
		processMessage(message).ifPresent(metadata -> {
			Daemon.runInDaemonThread(() -> prepareEncounterMessage(metadata), getDaemonToken());
		});
	}
	
	protected void prepareEncounterMessage(@NotNull EventMetadata metadata) {
		//Create bundle
		Encounter encounter = Context.getEncounterService().getEncounterByUuid(metadata.getString("uuid"));
		// Get observations & other referenced resources from encounter convert to FHIR then add to the bundle.
		
		// now publish the bundle
		// publish(bundle, context.newJsonParser());
	}
}
