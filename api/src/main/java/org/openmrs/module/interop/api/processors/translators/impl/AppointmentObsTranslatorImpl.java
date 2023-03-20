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
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Auditable;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.User;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.interop.api.processors.translators.AppointmentObsTranslator;
import org.openmrs.module.interop.api.processors.translators.AppointmentRequestTranslator;
import org.openmrs.module.interop.utils.ReferencesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Date;

import static org.apache.commons.lang3.Validate.notNull;

@Component("interop.appointments")
public class AppointmentObsTranslatorImpl implements AppointmentObsTranslator {
	
	@Autowired
	private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;
	
	@Autowired
	private AppointmentRequestTranslator appointmentRequestTranslator;
	
	@Override
	public Appointment toFhirResource(@Nonnull Obs obs) {
		notNull(obs, "The Openmrs Appointment object should not be null");
		
		Appointment fhirAppointment = new Appointment();
		fhirAppointment.setId(obs.getUuid());
		
		Appointment.AppointmentParticipantComponent participant = new Appointment.AppointmentParticipantComponent();
		participant.addType(new CodeableConcept()
		        .addCoding(new Coding("http://terminology.hl7.org/CodeSystem/v3-ParticipationType", "ATND", "Attender")));
		participant.setActor(practitionerReferenceTranslator.toFhirResource(obs.getCreator()));
		participant.getActor().setIdentifier(ReferencesUtil.buildProviderIdentifierByUser(obs.getCreator()));
		participant.setRequired(Appointment.ParticipantRequired.OPTIONAL);
		participant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
		fhirAppointment.addParticipant(participant);
		
		fhirAppointment.setStart(obs.getValueDatetime());
		fhirAppointment.setStatus(Appointment.AppointmentStatus.BOOKED);
		
		fhirAppointment.addBasedOn(getServiceRequest(obs.getEncounter()));
		fhirAppointment.setCreated(obs.getObsDatetime());
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
	
	private Reference getServiceRequest(Encounter encounter) {
		ServiceRequest request = appointmentRequestTranslator.toFhirResource(encounter);
		Reference reference = new Reference().setReference(FhirConstants.SERVICE_REQUEST + "/" + request.getId())
		        .setType(FhirConstants.PRACTITIONER);
		return reference;
	}
}
