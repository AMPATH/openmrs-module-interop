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

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.OpenmrsObject;
import org.openmrs.event.Event;
import org.openmrs.module.DaemonToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component("interop.eventManager")
public class InteropEventManager {
	
	@Setter
	@Getter
	public DaemonToken daemonToken;
	
	@Autowired
	private List<? extends Subscribable<? extends OpenmrsObject>> observers;
	
	public void enableEvents() {
		log.info("Enabling OpenMRS Interoperability Layer Events");
		observers.forEach(observer -> {
			observer.setDaemonToken(getDaemonToken());
			observer.actions().forEach(action -> {
				log.debug("Registering observer on {} with event {}", observer.clazz(), action.name());
				Event.subscribe(observer.clazz(), action.name(), observer);
			});
		});
	}
	
	public void disableEvents() {
		log.info("Disabling OpenMRS Interoperability Layer Events");
		observers.forEach(observer -> {
			observer.setDaemonToken(getDaemonToken());
			observer.actions().forEach(action -> {
				log.debug("Unsubscribing {} observer with event {}", observer.clazz(), action.name());
				Event.unsubscribe(observer.clazz(), action, observer);
			});
		});
	}
}
