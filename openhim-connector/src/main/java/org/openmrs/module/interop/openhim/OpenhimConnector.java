/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.openhim;

import javax.validation.constraints.NotNull;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.openmrs.api.context.Context;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.Publisher;
import org.openmrs.module.interop.openhim.api.OpenhimClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenhimConnector implements Publisher {
	
	@Override
	public void publish(@NotNull FhirContext context, @NotNull IAnyResource resource) {
		log.debug("publish resource with ID {}", resource.getId());
		String encodeResourceString = "";
		if (context != null) {
			encodeResourceString = context.newJsonParser().encodeResourceToString(resource);
		}
		if (encodeResourceString == null || encodeResourceString.isEmpty()) {
			encodeResourceString = resource.getId();
			log.error("Resource with UUID {} isn't encoded", encodeResourceString);
		}
		try {
			OpenhimConfiguration config = Context.getRegisteredComponent("interop.openhimConfiguration",
			    OpenhimConfiguration.class);
			OpenhimClient.postFhirResource(encodeResourceString,
			    config.getOpenhimServerUrl() + "/" + resource.fhirType().toLowerCase());
		}
		catch (Exception e) {
			log.error("Unable to post fhir resource", e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void publish(IAnyResource resource) {
	}
	
	@Override
	public boolean isEnabled() {
		return Boolean.parseBoolean(
		    Context.getAdministrationService().getGlobalProperty(InteropConstant.GP_ENABLE_OPENHIM, "false"));
	}
	
	@Override
	public boolean verifyConnection() {
		return Publisher.super.verifyConnection();
	}
}
