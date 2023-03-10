/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.api.observers;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Patient;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.Event;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.Subscribable;
import org.openmrs.module.interop.api.metadata.EventMetadata;
import org.openmrs.module.interop.utils.ObserverUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Setter
@Component("interop.patientObserver")
public class PatientObserver extends BaseObserver implements Subscribable<Patient> {
	
	@Autowired
	private FhirPatientService fhirPatientService;
	
	@Override
	public void onMessage(Message message) {
		processMessage(message)
		        .ifPresent(metadata -> Daemon.runInDaemonThread(() -> preparePatientMessage(metadata), getDaemonToken()));
	}
	
	private void preparePatientMessage(@NotNull EventMetadata metadata) {
		org.hl7.fhir.r4.model.Patient patientResource = fhirPatientService.get(metadata.getString("uuid"));
		if (patientResource != null) {
			//patientResource.getContained().clear();
			patientResource.setContained(resolvePatientProvence(patientResource.getContained()));
			this.publish(patientResource);
		} else {
			log.error("Couldn't find patient with UUID {} ", metadata.getString("uuid"));
		}
	}
	
	@Override
	public Class<?> clazz() {
		return Patient.class;
	}
	
	@Override
	public List<Event.Action> actions() {
		return ObserverUtils.defaultActions();
	}
	
	private List<Resource> resolvePatientProvence(List<Resource> resources) {
		List<Resource> result = resources.stream().filter(resource -> resource.fhirType().equals("Provenance"))
		        .collect(Collectors.toList());
		log.error(result + "Contained resources");
		List<Resource> provenceReferences = new ArrayList<>();
		for (Resource resource : result) {
			Provenance provenance = (Provenance) resource;
			Identifier identifier = new Identifier();
			//identifier.setValue();
			identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
			identifier.setSystem(ObserverUtils.getSystemUrlConfiguration());
			provenance.getAgentFirstRep().getWho().setIdentifier(identifier);
			provenceReferences.add(provenance);
		}
		
		return provenceReferences;
	}
}
