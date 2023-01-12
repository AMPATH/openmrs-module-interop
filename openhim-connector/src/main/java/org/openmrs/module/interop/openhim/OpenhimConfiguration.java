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

import static org.openmrs.module.interop.openhim.OpenhimConstants.GP_OPENHIM_BASE_URL;
import static org.openmrs.module.interop.openhim.OpenhimConstants.GP_OPENHIM_SUFFIX;

import org.openmrs.api.AdministrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("interop.openhimConfiguration")
public class OpenhimConfiguration {
	
	@Autowired
	@Qualifier("adminService")
	private AdministrationService administrationService;
	
	public String getOpenhimServerUrl() {
		String baseUrl = getOpenhimBaseUrl();
		String suffixUrl = getOpenhimSuffixUrl();
		if (baseUrl == null || suffixUrl == null) {
			throw new IllegalArgumentException("OpenHIM URL is invalid: baseUrl or suffixUrl is null");
		}
		return baseUrl + suffixUrl;
	}
	
	private String getOpenhimBaseUrl() {
		return administrationService.getGlobalProperty(GP_OPENHIM_BASE_URL);
	}
	
	private String getOpenhimSuffixUrl() {
		return administrationService.getGlobalProperty(GP_OPENHIM_SUFFIX);
	}
}
