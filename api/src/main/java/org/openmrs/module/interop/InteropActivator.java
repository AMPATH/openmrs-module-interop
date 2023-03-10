/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop;

import java.lang.reflect.InvocationTargetException;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.module.interop.api.InteropEventManager;
import org.openmrs.module.interop.api.Publisher;
import org.openmrs.module.interop.utils.ClassUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InteropActivator extends BaseModuleActivator implements ApplicationContextAware, DaemonTokenAware {
	
	@Autowired
	private InteropEventManager eventManager;
	
	private static ApplicationContext applicationContext;
	
	private static DaemonToken daemonToken;
	
	/**
	 * @see #started()
	 */
	public void started() {
		applicationContext.getAutowireCapableBeanFactory().autowireBean(this);
		this.eventManager.setDaemonToken(daemonToken);
		this.eventManager.enableEvents();
		
		//Verify only enabled publishers configured connections
		for (Class<? extends Publisher> publisher : ClassUtils.getPublishers()) {
			try {
				Publisher newInstancePublisher = publisher.getDeclaredConstructor().newInstance();
				if (newInstancePublisher.isEnabled()) {
					if (newInstancePublisher.verifyConnection()) {
						log.debug("{} verification was successful", publisher.getSimpleName());
					}
				}
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
		log.info("Started Interoperability Module");
	}
	
	/**
	 * @see #shutdown()
	 */
	public void shutdown() {
		this.eventManager.setDaemonToken(daemonToken);
		this.eventManager.disableEvents();
		log.info("Shutdown Interoperability Module");
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		InteropActivator.applicationContext = applicationContext;
	}
	
	@Override
	public void setDaemonToken(DaemonToken token) {
		InteropActivator.daemonToken = token;
	}
}
