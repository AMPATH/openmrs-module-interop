/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.openhim.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

public class OpenhimClient {
	
	public static void postFhirResource(String fhirResource, String openHimUrl) throws Exception {
		HttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(openHimUrl);
		
		StringEntity fhirResourceEntity = new StringEntity(fhirResource);
		httpPost.setEntity(fhirResourceEntity);
		httpPost.setHeader("Content-type", "application/fhir+json");
		
		HttpResponse response = httpClient.execute(httpPost);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode >= 200 && statusCode < 300) {
			System.out.println("FHIR resource was successfully posted to the OpenHIM channel");
		} else {
			String responseBody = response.getEntity().toString();
			System.out.println("An error occurred while posting the FHIR resource to the OpenHIM channel. Status code: "
			        + statusCode + " Response body: " + responseBody);
		}
	}
}
