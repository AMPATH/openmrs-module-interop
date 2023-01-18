/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.api.observers.patient;

import javax.jms.Message;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.Patient;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.Event;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.interop.api.Subscribable;
import org.openmrs.module.interop.api.observers.BaseObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component("interop.patientCreationListener")
public class PatientCreationObserver extends BaseObserver implements Subscribable<Patient> {
	
	@Autowired
	private FhirPatientService fhirPatientService;
	
	@Autowired
	@Qualifier("fhirR4")
	private FhirContext fhirContext;
	
	@Override
	public void onMessage(Message message) {
		Daemon.runInDaemonThread(() -> processMessage(fhirContext, fhirPatientService, message), getDaemonToken());
	}
	
	@Override
	public Class<?> clazz() {
		return Patient.class;
	}
	
	@Override
	public Event.Action action() {
		return Event.Action.CREATED;
	}
}
