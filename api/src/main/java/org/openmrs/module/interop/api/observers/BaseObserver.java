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

import javax.annotation.Nullable;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.validation.constraints.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.fhir2.api.FhirService;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.Publisher;
import org.openmrs.module.interop.utils.ClassUtils;

@Slf4j
public abstract class BaseObserver {
	
	@Setter
	@Getter
	public DaemonToken daemonToken;
	
	protected void processMessage(FhirContext context, FhirService<? extends IAnyResource> fhirService, Message message) {
		if (message instanceof MapMessage) {
			MapMessage mapMessage = (MapMessage) message;
			String uuid;
			try {
				uuid = mapMessage.getString("uuid");
				log.debug("Handling patient {}", uuid);
			}
			catch (JMSException e) {
				log.error("Exception caught while trying to get patient uuid for event", e);
				return;
			}
			
			if (uuid == null || StringUtils.isBlank(uuid))
				return;
			
			IAnyResource resource = fhirService.get(uuid);
			if (resource == null) {
				log.debug("could not find patient with uuid {}", uuid);
			} else {
				log.debug("Fhir Patient resource with UUID {} created", resource.getId());
				//publish resource
				publish(resource, context.newJsonParser());
			}
		}
	}
	
	public void publish(@NotNull IAnyResource resource, @Nullable IParser parser) {
		this.getPublishers().forEach(publisher -> {
			log.debug("Publishing resource with ID {} to {}", resource.getId(), publisher.getSimpleName());
			try {
				publisher.getDeclaredConstructor().newInstance().publish(resource, parser);
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				log.error("Unable to publish resource using {} Implementation", publisher.getSimpleName());
				// log to db
				throw new RuntimeException(e);
			}
		});
	}
	
	public Set<Class<? extends Publisher>> getPublishers() {
		return ClassUtils.getPublishers();
	}
	
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
		return Context.getAdministrationService().getGlobalProperty(InteropConstant.PUBLISHER_CLASS_NAME, "");
	}
}
