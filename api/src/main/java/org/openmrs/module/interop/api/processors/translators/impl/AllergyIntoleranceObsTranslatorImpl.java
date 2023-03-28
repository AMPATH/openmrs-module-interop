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
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.openmrs.Auditable;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.processors.translators.AllergyIntoleranceObsTranslator;
import org.openmrs.module.interop.utils.ObserverUtils;
import org.openmrs.module.interop.utils.ReferencesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Component("interop.allergyIntolerance")
public class AllergyIntoleranceObsTranslatorImpl implements AllergyIntoleranceObsTranslator {
	
	private static String CLINICAL_STATUS_SYSTEM = "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical";
	
	private static String ALLERGY_VERIFICATION_STATUS_SYSTEM = "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification";
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Override
	public AllergyIntolerance toFhirResource(@Nonnull Obs obs) {
		notNull(obs, "The OpenMrs Allergy Obs object should not be null");
		
		AllergyIntolerance fhirAllergyIntolerance = new AllergyIntolerance();
		fhirAllergyIntolerance.setId(obs.getUuid());
		
		fhirAllergyIntolerance.addIdentifier(new Identifier().setSystem(ObserverUtils.getSystemUrlConfiguration())
		        .setUse(Identifier.IdentifierUse.OFFICIAL).setValue(obs.getObsGroup().getUuid()));
		
		Person obsPerson = obs.getPerson();
		fhirAllergyIntolerance.setPatient(patientReferenceTranslator.toFhirResource((Patient) obsPerson));
		
		if (drugAllergies().contains(obs.getConcept().getUuid())) {
			fhirAllergyIntolerance.addCategory(AllergyIntolerance.AllergyIntoleranceCategory.MEDICATION);
		}
		fhirAllergyIntolerance.setType(AllergyIntolerance.AllergyIntoleranceType.ALLERGY);
		
		fhirAllergyIntolerance.setCode(conceptTranslator.toFhirResource(obs.getValueCoded()));
		fhirAllergyIntolerance.setRecordedDate(obs.getObsDatetime());
		fhirAllergyIntolerance
		        .setClinicalStatus(new CodeableConcept().addCoding(new Coding(CLINICAL_STATUS_SYSTEM, "active", "Active")));
		fhirAllergyIntolerance.setVerificationStatus(
		    new CodeableConcept().addCoding(new Coding(ALLERGY_VERIFICATION_STATUS_SYSTEM, "unconfirmed", "Unconfirmed")));
		
		fhirAllergyIntolerance.setRecorder(practitionerReferenceTranslator.toFhirResource(obs.getCreator()));
		fhirAllergyIntolerance.getRecorder().setIdentifier(ReferencesUtil.buildProviderIdentifierByUser(obs.getCreator()));
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
	
	public List<String> drugAllergies() {
		String conceptString = Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.ALLERGY_SEVERITY_CONCEPT_UUID, "");
		
		return Arrays.asList(conceptString.split(","));
	}
}
