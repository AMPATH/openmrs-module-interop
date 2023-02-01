/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.api;

import javax.validation.constraints.NotNull;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IAnyResource;

public interface Publisher {
	
	/**
	 * Publishes FHIR resource to a configured publisher e.g. kafka connect
	 *
	 * @param resource Resource to be published.
	 */
	void publish(@NotNull FhirContext context, @NotNull IAnyResource resource);
	
	/**
	 * Publishes FHIR resource (Not JSON encoded)
	 *
	 * @param resource Resource to be published.
	 */
	void publish(@NotNull IAnyResource resource);
	
	default boolean isEnabled() {
		return false;
	}
	
	default boolean verifyConnection() {
		return true;
	}
	
}
