/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.api.processors.translators.impl;

import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.Auditable;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.interop.api.processors.translators.AppointmentObsTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Date;

import static org.apache.commons.lang3.Validate.notNull;

@Component("interop.appointments")
public class AppointmentObsTranslatorImpl implements AppointmentObsTranslator {
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Override
	public Appointment toFhirResource(@Nonnull Obs obs) {
		notNull(obs, "The Openmrs Appointment object should not be null");
		
		Appointment fhirAppointment = new Appointment();
		fhirAppointment.setId(obs.getUuid());
		
		Person obsPerson = obs.getPerson();
		Appointment.AppointmentParticipantComponent t = new Appointment.AppointmentParticipantComponent();
		t.setActor(patientReferenceTranslator.toFhirResource((Patient) obsPerson));
		fhirAppointment.addParticipant(t);
		
		fhirAppointment.setStart(obs.getValueDatetime());
		fhirAppointment.setCreated(obs.getObsDatetime());
		if (obs.getConcept().getUuid().equalsIgnoreCase("160288AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
			if (obs.getValueCoded() != null) {
				fhirAppointment.setAppointmentType(conceptTranslator.toFhirResource(obs.getValueCoded()));
			}
		}
		
		fhirAppointment.getMeta().setLastUpdated(this.getLastUpdated(obs));
		
		return fhirAppointment;
	}
	
	public Date getLastUpdated(OpenmrsObject object) {
		if (object instanceof Auditable) {
			Auditable auditable = (Auditable) object;
			return auditable.getDateChanged() != null ? auditable.getDateChanged() : auditable.getDateCreated();
		} else {
			return null;
		}
	}
}
