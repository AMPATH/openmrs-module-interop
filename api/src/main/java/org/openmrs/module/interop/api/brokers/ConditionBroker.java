/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.api.brokers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Condition;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.InteropBroker;
import org.openmrs.module.interop.api.brokers.translators.ConditionObsTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component("interop.conditionBroker")
public class ConditionBroker implements InteropBroker {
	
	@Autowired
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
	public List<Condition> processEncounter(Encounter encounter) {
		List<Obs> allObs = new ArrayList<>(encounter.getAllObs());
		
		List<Obs> conditionsObs = new ArrayList<>();
		log.error("size:: {}", allObs.size());
		if (!allObs.isEmpty()) {
			allObs.forEach((obs -> {
				if (questions().contains(obs.getConcept().getUuid())) {
					conditionsObs.add(obs);
				}
			}));
		}
		
		//		if (validateEncounterType(encounter)) {
		//
		//		}
		
		List<Condition> conditions = new ArrayList<>();
		log.error("Questions:: {}", questions());
		log.error("conditions Obs:: {}", conditionsObs);
		if (!conditionsObs.isEmpty()) {
			conditionsObs.forEach(obs -> conditions.add(conditionObsTranslator.toFhirResource(obs)));
		}
		
		return conditions;
	}
	
	private boolean validateEncounterType(Encounter encounter) {
		return encounterTypes().contains(encounter.getEncounterType().getUuid());
	}
	
}
