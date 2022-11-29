/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.dhpevents.api.publisher;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import ca.uhn.fhir.parser.IParser;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.openmrs.module.dhpevents.api.Publisher;

@Slf4j
public class FilePublisher implements Publisher {
	
	@Override
	public void publish(@NotNull IAnyResource resource, @Nullable IParser parser) {
		assert parser != null;
		log.error("Nimefikiwa hehe {}", parser.setPrettyPrint(true).encodeResourceToString(resource));
	}
}
