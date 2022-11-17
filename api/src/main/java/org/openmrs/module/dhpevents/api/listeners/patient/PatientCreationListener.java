/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.dhpevents.api.listeners.patient;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

import ca.uhn.fhir.context.FhirContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.Event;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.dhpevents.api.Subscribable;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component("dhp.patientCreationListener")
public class PatientCreationListener implements Subscribable<Patient> {
	
	@Setter
	@Getter
	public DaemonToken daemonToken;
	
	@Autowired
	private PatientTranslator patientTranslator;
	
	@Autowired
	@Qualifier("fhirR4")
	private FhirContext fhirContext;
	
	@Override
	public void onMessage(Message message) {
		log.trace("Patient created message received {}", message);
		Daemon.runInDaemonThread(() -> processMessage(message), getDaemonToken());
	}
	
	private void processMessage(Message message) {
		if (message instanceof MapMessage) {
			MapMessage mapMessage = (MapMessage) message;
			String uuid;
			try {
				uuid = mapMessage.getString("uuid");
				log.debug("Handling patient {}", uuid);
			}
			catch (JMSException e) {
				log.error("Exception caught while trying to get patient uuid for event", e);
				return;
			}
			
			if (uuid == null || StringUtils.isBlank(uuid))
				return;
			
			Patient patient = Context.getPatientService().getPatientByUuid(uuid);
			if (patient == null) {
				log.debug("could not find patient with uuid {}", uuid);
			} else {
				//Convert to fhir then publish to kafka
				log.debug("Patient created: {}", patient.getUuid());
				org.hl7.fhir.r4.model.Patient patientResource = patientTranslator.toFhirResource(patient);
				
				log.error("Created patient resource {}",
				    fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(patientResource));
			}
		}
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
