/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.dhpevents.producer.api;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import java.util.concurrent.ExecutionException;

import ca.uhn.fhir.parser.IParser;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.openmrs.module.dhpevents.api.Publisher;
import org.openmrs.module.dhpevents.producer.api.impl.ProduceServiceImpl;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConnectPublisher implements Publisher {
	
	@Override
	public void publish(@NotNull IAnyResource resource, @Nullable IParser parser) {
		ProducerService<String, String> producerService = new ProduceServiceImpl("PATIENT_CREATION");
		try {
			log.debug("publish resource with ID {}", resource.getId());
			String encodeResourceString = "";
			if (parser != null) {
				encodeResourceString = parser.encodeResourceToString(resource);
			}
			if (encodeResourceString == null || encodeResourceString.isEmpty()) {
				encodeResourceString = resource.getId();
				log.error("Resource with UUID {} isn't encoded", encodeResourceString);
			}
			producerService.produce(resource.getId(), encodeResourceString);
		}
		catch (ExecutionException | InterruptedException e) {
			log.error("Error publishing resource with id {}", resource.getIdElement().getIdPart(), e);
			throw new RuntimeException(e);
		}
	}
}
