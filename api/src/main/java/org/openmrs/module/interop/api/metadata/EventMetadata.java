/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.api.metadata;

import java.util.HashMap;

public class EventMetadata {
	
	private HashMap<String, Object> properties;
	
	public EventMetadata() {
		properties = new HashMap<>();
	}
	
	public void addProperty(String key, Object value) {
		if (properties == null) {
			properties = new HashMap<>();
		}
		properties.put(key, value);
	}
	
	public Object getProperty(String key) {
		return properties.get(key);
	}
	
	public String getString(String key) {
		return String.valueOf(getProperty(key));
	}
}
