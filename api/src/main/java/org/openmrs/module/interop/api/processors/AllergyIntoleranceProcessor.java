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
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.DateTimeType;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.InteropProcessor;
import org.openmrs.module.interop.api.processors.translators.AllergyIntoleranceObsTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component("interop.allergyIntoleranceBroker")
public class AllergyIntoleranceProcessor implements InteropProcessor<Encounter> {
	
	@Autowired
	private AllergyIntoleranceObsTranslator allergyIntoleranceObsTranslator;
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Override
	public List<String> encounterTypes() {
		
		return Arrays.asList(Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.ALLERGY_PROCESSOR_ENCOUNTER_TYPE_UUIDS, "").split(","));
	}
	
	@Override
	public List<String> questions() {
		String appointmentString = Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.ALLERGY_CAUSATIVE_AGENT_CONCEPT_UUID, "");
		
		return Arrays.asList(appointmentString.split(","));
	}
	
	@Override
	public List<String> forms() {
		return null;
	}
	
	@Override
	public List<AllergyIntolerance> process(Encounter encounter) {
		List<Obs> allObs = new ArrayList<>(encounter.getAllObs());
		
		List<Obs> allergyObs = new ArrayList<>();
		List<Obs> reactionsObs = new ArrayList<>();
		List<Obs> severityObs = new ArrayList<>();
		List<Obs> onsetDateObs = new ArrayList<>();
		List<Obs> actionObs = new ArrayList<>();
		if (validateEncounterType(encounter)) {
			allObs.forEach(obs -> {
				if (validateConceptQuestions(obs)) {
					allergyObs.add(obs);
				}
				if (validateAllergyReactionObs(obs)) {
					reactionsObs.add(obs);
				}
				if (validateAllergySeverityObs(obs)) {
					severityObs.add(obs);
				}
				if (validateAllergyOnsetDateObs(obs)) {
					onsetDateObs.add(obs);
				}
				if (validateAllergyActionTakenObs(obs)) {
					actionObs.add(obs);
				}
			});
		}
		
		List<AllergyIntolerance> allergyIntolerances = new ArrayList<>();
		if (!allergyObs.isEmpty()) {
			allergyObs.forEach(obs -> {
				AllergyIntolerance allergy = allergyIntoleranceObsTranslator.toFhirResource(obs);
				String obsGroupId = allergy.getIdentifierFirstRep().getValue();
				
				AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent = new AllergyIntolerance.AllergyIntoleranceReactionComponent();
				reactionComponent.setSubstance(allergy.getCode());
				reactionsObs.forEach(r -> {
					if (r.getObsGroup().getUuid().equals(obsGroupId)) {
						reactionComponent.addManifestation(conceptTranslator.toFhirResource(r.getValueCoded()));
					}
				});
				severityObs.forEach(s -> {
					if (s.getObsGroup().getUuid().equals(obsGroupId)) {
						reactionComponent.setSeverity(getAllergySeverity(obs));
						if (getAllergySeverity(obs).equals(AllergyIntolerance.AllergyIntoleranceSeverity.SEVERE)) {
							allergy.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.HIGH);
						} else {
							allergy.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.LOW);
						}
					}
				});
				onsetDateObs.forEach(od -> {
					if (od.getObsGroup().getUuid().equals(obsGroupId)) {
						if (od.getObsDatetime() != null) {
							reactionComponent.setOnset(obs.getValueDatetime());
							allergy.setOnset(new DateTimeType().setValue(obs.getValueDatetime()));
						}
					}
				});
				actionObs.forEach(a -> {
					reactionComponent.addNote(new Annotation().setText(a.getValueCoded().getDisplayString()));
				});
				
				allergy.addReaction(reactionComponent);
				allergyIntolerances.add(allergy);
			});
		}
		
		return allergyIntolerances;
	}
	
	private static AllergyIntolerance.AllergyIntoleranceSeverity getAllergySeverity(Obs obs) {
		String concept = obs.getValueCoded().getUuid();
		List<String> severeConcepts = Arrays.asList(Context.getAdministrationService()
		        .getGlobalProperty(InteropConstant.ALLERGY_SEVERITY_SEVERE_CONCEPT_UUID, "").split(","));
		List<String> moderateConcepts = Arrays.asList(Context.getAdministrationService()
		        .getGlobalProperty(InteropConstant.ALLERGY_SEVERITY_MODERATE_CONCEPT_UUID, "").split(","));
		if (moderateConcepts.contains(concept)) {
			return AllergyIntolerance.AllergyIntoleranceSeverity.MODERATE;
		} else if (severeConcepts.contains(concept)) {
			return AllergyIntolerance.AllergyIntoleranceSeverity.SEVERE;
		} else {
			return AllergyIntolerance.AllergyIntoleranceSeverity.MILD;
		}
	}
	
	public boolean validateAllergyReactionObs(Obs conceptObs) {
		String conceptString = Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.ALLERGY_REACTION_CONCEPT_UUID, "");
		
		List<String> conceptUuids = Arrays.asList(conceptString.split(","));
		
		return conceptUuids.contains(conceptObs.getConcept().getUuid());
	}
	
	public boolean validateAllergySeverityObs(Obs conceptObs) {
		String conceptString = Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.ALLERGY_SEVERITY_CONCEPT_UUID, "");
		
		List<String> conceptUuids = Arrays.asList(conceptString.split(","));
		
		return conceptUuids.contains(conceptObs.getConcept().getUuid());
	}
	
	public boolean validateAllergyOnsetDateObs(Obs conceptObs) {
		String conceptString = Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.ALLERGY_ONSET_DATE_CONCEPT_UUID, "");
		
		List<String> conceptUuids = Arrays.asList(conceptString.split(","));
		
		return conceptUuids.contains(conceptObs.getConcept().getUuid());
	}
	
	public boolean validateAllergyActionTakenObs(Obs conceptObs) {
		String conceptString = Context.getAdministrationService()
		        .getGlobalPropertyValue(InteropConstant.ALLERGY_ACTION_TAKEN_CONCEPT_UUID, "");
		
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
