/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
public class FhirClientUtil {
	
	private final String fhirUrl;
	
	private final String sourceUser;
	
	private final String sourcePw;
	
	private final FhirContext fhirContext;
	
	public FhirClientUtil(String sourceFhirUrl, String sourceUser, String sourcePw, FhirContext fhirContext) {
		this.fhirUrl = sourceFhirUrl;
		this.sourceUser = sourceUser;
		this.sourcePw = sourcePw;
		this.fhirContext = fhirContext;
	}
	
	public Bundle fetchPatientResource(String identifier) {
		try {
			IGenericClient client = getSourceClient();
			Bundle resource = client.search().forResource("Patient").where(Patient.IDENTIFIER.exactly().code(identifier))
			        .returnBundle(Bundle.class).execute();
			return resource;
		}
		catch (Exception e) {
			log.error(String.format("Failed fetching FHIR resource %s", e));
			return null;
		}
	}
	
	public Bundle fetchLocationResource(String identifier) {
		try {
			IGenericClient client = getSourceClient();
			Bundle resource = client.search().forResource("Location").where(Location.IDENTIFIER.exactly().code(identifier))
			        .returnBundle(Bundle.class).execute();
			return resource;
		}
		catch (Exception e) {
			log.error(String.format("Failed fetching FHIR resource %s", e));
			return null;
		}
	}
	
	public Resource fetchFhirResource(String resourceType, String resourceId) {
		try {
			IGenericClient client = getSourceClient();
			IBaseResource resource = client.read().resource(resourceType).withId(resourceId).execute();
			return (Resource) resource;
		}
		catch (Exception e) {
			log.error(String.format("Failed fetching FHIR %s resource with Id %s: %s", resourceType, resourceId, e));
			return null;
		}
	}
	
	public IGenericClient getSourceClient() {
		IClientInterceptor authInterceptor = new BasicAuthInterceptor(this.sourceUser, this.sourcePw);
		fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
		
		IGenericClient client = fhirContext.getRestfulClientFactory().newGenericClient(this.fhirUrl);
		client.registerInterceptor(authInterceptor);
		
		return client;
	}
	
}
