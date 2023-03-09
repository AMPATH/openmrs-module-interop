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

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.openmrs.Auditable;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.event.Event;
import org.openmrs.module.fhir2.api.translators.AllergyIntoleranceTranslator;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.impl.AllergyIntoleranceTranslatorImpl;
import org.openmrs.module.interop.api.processors.translators.AllergyIntoleranceObsTranslator;
import org.openmrs.module.interop.api.processors.translators.ConditionObsTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Component("interop.allergyIntolerance")
public class AllergyIntoleranceObsTranslatorImpl implements AllergyIntoleranceObsTranslator {
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	AllergyIntoleranceTranslatorImpl allergyIntoleranceTranslator;
	
	@Override
	public AllergyIntolerance toFhirResource(@Nonnull Obs obs) {
		notNull(obs, "The Openmrs Allergy Intolerance object should not be null");
		
		AllergyIntolerance fhirAllergyIntolerance = new AllergyIntolerance();
		fhirAllergyIntolerance.setId(obs.getUuid());
		
		Person obsPerson = obs.getPerson();
		fhirAllergyIntolerance.setPatient(patientReferenceTranslator.toFhirResource((Patient) obsPerson));
		//Category, code and recorded date
		if (obs.getConcept().getUuid().equalsIgnoreCase("160643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
			if (obs.getValueCoded() != null) {
				fhirAllergyIntolerance.addCategory(AllergyIntolerance.AllergyIntoleranceCategory.FOOD);
				
			}
		}
		//Onset Date
		if (obs.getConcept().getUuid().equalsIgnoreCase("160753AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
			if (obs.getValueDatetime() != null) {
				fhirAllergyIntolerance.setOnset(new DateTimeType().setValue(obs.getValueDatetime()));
			}
		}
		//Reaction
		if (obs.getConcept().getUuid().equalsIgnoreCase("159935AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
			if (obs.getValueCoded() != null) {
				AllergyIntolerance.AllergyIntoleranceReactionComponent rc = new AllergyIntolerance.AllergyIntoleranceReactionComponent();
				rc.addManifestation(conceptTranslator.toFhirResource(obs.getValueCoded()));
				fhirAllergyIntolerance.setReaction(Arrays.asList(rc));
			}
		}
		
		//Severity
		if (obs.getConcept().getUuid().equalsIgnoreCase("162760AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
			if (obs.getValueCoded() != null) {
				AllergyIntolerance.AllergyIntoleranceReactionComponent cc = new AllergyIntolerance.AllergyIntoleranceReactionComponent();
				if (obs.getValueCoded().getConceptId().equals("160754AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
					cc.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.MILD);
					fhirAllergyIntolerance.setReaction(Arrays.asList(cc));
				} else if (obs.getValueCoded().getConceptId().equals("160755AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
					cc.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.MODERATE);
					fhirAllergyIntolerance.setReaction(Arrays.asList(cc));
				} else if (obs.getValueCoded().getConceptId().equals("160756AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
					cc.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.SEVERE);
					fhirAllergyIntolerance.setReaction(Arrays.asList(cc));
				}
				
			}
		}
		
		fhirAllergyIntolerance.setRecorder(practitionerReferenceTranslator.toFhirResource(obs.getCreator()));
		fhirAllergyIntolerance.getMeta().setLastUpdated(this.getLastUpdated(obs));
		
		return fhirAllergyIntolerance;
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
