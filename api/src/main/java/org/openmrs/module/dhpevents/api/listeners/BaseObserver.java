/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.dhpevents.api.listeners;

import java.lang.reflect.InvocationTargetException;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.dhpevents.DhpEventsConstant;
import org.openmrs.module.dhpevents.api.Publisher;

@Slf4j
public abstract class BaseObserver {
	
	public Publisher getPublisher() {
		Class<?> publisherClass;
		try {
			publisherClass = Context.loadClass(getPublisherClassName());
		}
		catch (ClassNotFoundException e) {
			log.error("Failed to load class {}", getPublisherClassName(), e);
			throw new APIException("Failed to load publisher", new Object[] { getPublisherClassName() }, e);
		}
		
		try {
			return (Publisher) publisherClass.getDeclaredConstructor().newInstance();
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
			throw new APIException("Failed to load publisher", new Object[] { getPublisherClassName() }, e);
		}
	}
	
	private String getPublisherClassName() {
		return Context.getAdministrationService().getGlobalProperty(DhpEventsConstant.PUBLISHER_CLASS_NAME,
		    "org.openmrs.module.dhpevents.producer.api.impl.kafkaConnectPublisher");
	}
}
