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

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Encounter;
import org.openmrs.User;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.interop.api.processors.translators.DiagnosticReportTranslator;
import org.openmrs.module.interop.utils.ReferencesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import static org.apache.commons.lang3.Validate.notNull;

@Component("interop.diagnosticReportTranslator")
public class DiagnosticReportTranslatorImpl implements DiagnosticReportTranslator {
	
	@Autowired
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Autowired
	private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Override
	public DiagnosticReport toFhirResource(@Nonnull Encounter encounter) {
		notNull(encounter, "The encounter object should not be null");
		
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		
		diagnosticReport.setId(encounter.getUuid());
		diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
		diagnosticReport.addCategory(new CodeableConcept()
		        .addCoding(new Coding("http://terminology.hl7.org/3.1.0/CodeSystem-v2-0074.html", "LAB", "Laboratory")));
		
		diagnosticReport.setEncounter(encounterReferenceTranslator.toFhirResource(encounter));
		diagnosticReport.setIssued(encounter.getEncounterDatetime());
		diagnosticReport.setSubject(patientReferenceTranslator.toFhirResource(encounter.getPatient()));
		diagnosticReport.getSubject().setIdentifier(ReferencesUtil.buildPatientUpiIdentifier(encounter.getPatient()));
		
		diagnosticReport.addResultsInterpreter(practitionerReferenceTranslator.toFhirResource(encounter.getCreator()));
		diagnosticReport.getResultsInterpreterFirstRep()
		        .setIdentifier(ReferencesUtil.buildProviderIdentifierByUser(encounter.getCreator()));
		
		return diagnosticReport;
	}
}
