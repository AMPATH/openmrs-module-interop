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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.InteropProcessor;
import org.openmrs.module.interop.api.processors.translators.ConditionObsTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component("interop.conditionBroker")
public class ConditionProcessor implements InteropProcessor<Encounter> {
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	@Qualifier("interop.conditions")
	private ConditionObsTranslator conditionObsTranslator;
	
	@Override
	public List<String> encounterTypes() {
		
		return Arrays.asList(Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.CONDITION_BROKER_ENCOUNTER_TYPE_UUIDS, "").split(","));
	}
	
	@Override
	public List<String> questions() {
		String conditionString = Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.CONDITIONS_CONCEPT_UUID, "");
		
		return Arrays.asList(conditionString.split(","));
	}
	
	@Override
	public List<String> forms() {
		return null;
	}
	
	@Override
	public List<Condition> process(Encounter encounter) {
		List<Obs> allObs = new ArrayList<>(encounter.getAllObs());
		
		List<Obs> conditionsObs = new ArrayList<>();
		List<Obs> treatmentPlanObs = new ArrayList<>();
		if (validateEncounterType(encounter)) {
			allObs.forEach(obs -> {
				if (validateConceptQuestions(obs)) {
					conditionsObs.add(obs);
				}
				if (validateDiagnosisTreatmentPlan(obs)) {
					treatmentPlanObs.add(obs);
				}
			});
		}
		
		List<Condition> conditions = new ArrayList<>();
		if (!conditionsObs.isEmpty()) {
			conditionsObs.forEach(obs -> {
				Condition condition = conditionObsTranslator.toFhirResource(obs);
				if (obs.getObsGroup() != null) {
					treatmentPlanObs.forEach(value -> {
						if (value.getObsGroup().equals(obs.getObsGroup())) {
							CodeableConcept treatmentPlan = conceptTranslator.toFhirResource(value.getConcept());
							treatmentPlan.getCodingFirstRep().setDisplay(value.getValueText());
							treatmentPlan.setText(value.getValueText());
							
							Extension extension = new Extension();
							extension.setValue(treatmentPlan);
							extension.setUrl(InteropConstant.SYSTEM_URL);
							
							condition.addExtension(extension);
						}
					});
				}
				conditions.add(condition);
			});
		}
		
		return conditions;
	}
	
	public boolean validateDiagnosisTreatmentPlan(Obs conceptObs) {
		String conceptString = Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.DIAGNOSIS_TREATMENT_PLAN_CONCEPT_UUID, "");
		
		List<String> conceptUuids = Arrays.asList(conceptString.split(","));
		
		return conceptUuids.contains(conceptObs.getConcept().getUuid());
	}
	
	private boolean validateEncounterType(Encounter encounter) {
		return encounterTypes().contains(encounter.getEncounterType().getUuid());
	}
	
	private boolean validateConceptQuestions(Obs conceptObs) {
		return questions().contains(conceptObs.getConcept().getUuid());
	}
	
}
