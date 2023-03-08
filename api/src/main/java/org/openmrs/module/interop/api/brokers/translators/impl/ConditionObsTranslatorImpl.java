/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.api.brokers.translators.impl;

import static org.apache.commons.lang3.Validate.notNull;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getLastUpdated;

import javax.annotation.Nonnull;

import org.hibernate.proxy.HibernateProxy;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.api.db.hibernate.HibernateUtil;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.interop.api.brokers.translators.ConditionObsTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConditionObsTranslatorImpl implements ConditionObsTranslator {
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Override
	public Condition toFhirResource(@Nonnull Obs obs) {
		notNull(obs, "The Openmrs Condition object should not be null");
		
		Condition fhirCondition = new Condition();
		fhirCondition.setId(obs.getUuid());
		
		Person obsPerson = obs.getPerson();
		if (obsPerson != null) {
			if (obsPerson instanceof HibernateProxy) {
				obsPerson = HibernateUtil.getRealObjectFromProxy(obsPerson);
			}
			
			if (obsPerson instanceof Patient) {
				fhirCondition.setSubject(patientReferenceTranslator.toFhirResource((Patient) obsPerson));
			}
		}
		
		if (obs.getValueCoded() != null) {
			fhirCondition.setCode(conceptTranslator.toFhirResource(obs.getValueCoded()));
		}
		
		fhirCondition.setOnset(new DateTimeType().setValue(obs.getObsDatetime()));
		fhirCondition.setRecorder(practitionerReferenceTranslator.toFhirResource(obs.getCreator()));
		fhirCondition.setRecordedDate(obs.getDateCreated());
		
		fhirCondition.getMeta().setLastUpdated(getLastUpdated(obs));
		
		return fhirCondition;
	}
}
