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

import java.util.Arrays;
import java.util.List;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.Patient;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.Event;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.interop.api.Subscribable;
import org.openmrs.module.interop.api.metadata.EventMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component("interop.patientObserver")
public class PatientObserver extends BaseObserver implements Subscribable<Patient> {
	
	@Autowired
	private FhirPatientService fhirPatientService;
	
	@Override
	public void onMessage(Message message) {
		processMessage(message)
		        .ifPresent(metadata -> Daemon.runInDaemonThread(() -> preparePatientMessage(metadata), getDaemonToken()));
	}
	
	private void preparePatientMessage(@NotNull EventMetadata metadata) {
		org.hl7.fhir.r4.model.Patient patientResource = fhirPatientService.get(metadata.getString("uuid"));
		if (patientResource != null) {
			this.publish(patientResource);
		} else {
			log.error("Couldn't find patient with UUID {} ", metadata.getString("uuid"));
			// todo persist to db unresolved patient UUIDs
		}
	}
	
	@Override
	public Class<?> clazz() {
		return Patient.class;
	}
	
	@Override
	public List<Event.Action> actions() {
		// make this configurable using GPs
		return Arrays.asList(Event.Action.UPDATED, Event.Action.CREATED, Event.Action.VOIDED);
	}
}
