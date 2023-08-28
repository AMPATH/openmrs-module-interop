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

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.validation.constraints.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;

import ca.uhn.fhir.context.FhirContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.openmrs.event.Event;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.interop.api.Publisher;
import org.openmrs.module.interop.api.metadata.EventMetadata;
import org.openmrs.module.interop.utils.ClassUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public abstract class BaseObserver {
	
	@Autowired
	@Getter
	@Qualifier("fhirR4")
	private FhirContext fhirContext;
	
	@Setter
	@Getter
	public DaemonToken daemonToken;
	
	/**
	 * Process the message and gets the UUID of the OpenMRS Object modified or created, or deleted
	 *
	 * @param message the emitted event message to be processed.
	 * @return UUID of the OpenMRS Object
	 */
	protected Optional<EventMetadata> processMessage(Message message) {
		EventMetadata metadata = new EventMetadata();
		if (message instanceof MapMessage) {
			MapMessage mapMessage = (MapMessage) message;
			try {
				String uuid = mapMessage.getString("uuid");
				metadata.addProperty("uuid", uuid);
				
				//retrieve destination message then determine the action
				String destinationMessage = mapMessage.getString("destination");
				determineAction(Optional.ofNullable(destinationMessage)).ifPresent(action -> {
					metadata.addProperty("action", action);
				});
				
				log.error("metadata {}", metadata);
			}
			catch (JMSException e) {
				log.error("Exception caught while trying to get patient uuid or Destination message for event", e);
			}
		}
		return Optional.of(metadata);
	}
	
	/**
	 * Determines the database operation/activity performed
	 *
	 * @param destinationMessage The destination message
	 * @return {@link Event.Action} performed
	 */
	protected Optional<Event.Action> determineAction(@NotNull Optional<String> destinationMessage) {
		// destination = topic://UPDATED:org.openmrs.Patient
		String action = "";
		if (destinationMessage.isPresent()) {
			action = destinationMessage.get().split(":(?://)?")[1];
		}
		switch (action) {
			case "CREATED":
				return Optional.of(Event.Action.CREATED);
			case "UPDATED":
				return Optional.of(Event.Action.UPDATED);
			case "VOIDED":
				return Optional.of(Event.Action.VOIDED);
			case "UNVOIDED":
				return Optional.of(Event.Action.UNVOIDED);
			case "RETIRED":
				return Optional.of(Event.Action.RETIRED);
			case "UNRETIRED":
				return Optional.of(Event.Action.UNRETIRED);
			case "PURGED":
				return Optional.of(Event.Action.PURGED);
			default:
				return Optional.empty();
		}
	}
	
	public void publish(@NotNull IAnyResource resource) {
		log.error("Bundled resources :: {}",
		    fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource));
		
		this.getPublishers().forEach(publisher -> {
			Publisher newInstancePublisher;
			try {
				newInstancePublisher = publisher.getDeclaredConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				log.error("Unable to instantiate {} publisher class", publisher.getSimpleName());
				throw new RuntimeException(e);
			}
			// Publish to enabled connectors
			if (newInstancePublisher.isEnabled()) {
				log.info("Publishing resource with ID {} to {}", resource.getId(), publisher.getSimpleName());
				newInstancePublisher.publish(fhirContext, resource);
			}
		});
	}
	
	public Set<Class<? extends Publisher>> getPublishers() {
		return ClassUtils.getPublishers();
	}
}
