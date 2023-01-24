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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.Patient;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.Event;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.interop.api.Subscribable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component("interop.patientObserver")
public class PatientObserver extends BaseObserver implements Subscribable<Patient> {
	
	@Autowired
	private FhirPatientService fhirPatientService;
	
	@Autowired
	@Qualifier("fhirR4")
	private FhirContext fhirContext;
	
	@Override
	public void onMessage(Message message) {
		Daemon.runInDaemonThread(() -> processPatientMessage(processMessage(message)), getDaemonToken());
	}
	
	private void processPatientMessage(Optional<String> patientUuid) {
		patientUuid.ifPresent((uuid) -> publish(fhirPatientService.get(uuid), fhirContext.newJsonParser()));
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
