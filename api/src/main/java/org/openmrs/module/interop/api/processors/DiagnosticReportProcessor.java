/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.api.processors;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.InteropProcessor;
import org.openmrs.module.interop.api.processors.translators.DiagnosticReportTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component("interop.diagnosticReportProcessor")
public class DiagnosticReportProcessor implements InteropProcessor<Encounter> {
	
	@Autowired
	@Qualifier("interop.diagnosticReportTranslator")
	private DiagnosticReportTranslator diagnosticReportTranslator;
	
	@Autowired
	private ObservationReferenceTranslator observationReferenceTranslator;
	
	@Override
	public List<String> encounterTypes() {
		return Arrays.asList(Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.LAB_RESULT_PROCESSOR_ENCOUNTER_TYPE_UUIDS, "").split(","));
	}
	
	@Override
	public List<String> questions() {
		String labResultString = Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.LAB_RESULT_CONCEPT_UUID, "");
		
		return Arrays.asList(labResultString.split(","));
	}
	
	@Override
	public List<String> forms() {
		return null;
	}
	
	@Override
	public List<DiagnosticReport> process(Encounter encounter) {
		List<Obs> encounterObs = new ArrayList<>(encounter.getAllObs());
		
		List<Obs> resultObs = new ArrayList<>();
		if (validateEncounterType(encounter)) {
			encounterObs.forEach(obs -> {
				if (validateConceptQuestions(obs)) {
					resultObs.add(obs);
				}
			});
		}
		
		DiagnosticReport diagnosticReport = diagnosticReportTranslator.toFhirResource(encounter);
		
		if (!resultObs.isEmpty()) {
			for (Obs obs : resultObs) {
				Reference observation = observationReferenceTranslator.toFhirResource(obs);
				observation.setIdentifier(new Identifier().setSystem(InteropConstant.SYSTEM_URL).setValue(obs.getUuid())
				        .setUse(Identifier.IdentifierUse.OFFICIAL));
				diagnosticReport.addResult(observation);
			}
		} else {
			return new ArrayList<>();
		}
		
		return Collections.singletonList(diagnosticReport);
	}
	
	private boolean validateEncounterType(Encounter encounter) {
		return encounterTypes().contains(encounter.getEncounterType().getUuid());
	}
	
	private boolean validateConceptQuestions(Obs conceptObs) {
		return questions().contains(conceptObs.getConcept().getUuid());
	}
	
}
