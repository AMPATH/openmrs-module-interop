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

import java.util.Set;

import org.openmrs.module.interop.api.InteropBroker;
import org.openmrs.module.interop.api.Publisher;
import org.reflections.Reflections;

public class ClassUtils {
	
	public static Set<Class<? extends Publisher>> getPublishers() {
		Reflections reflections = new Reflections("org.openmrs.module.interop");
		//using query functions
		//reflections.get(SubTypes.of(Publisher.class).asClass());
		return reflections.getSubTypesOf(Publisher.class);
	}
	
	public static Set<Class<? extends InteropBroker>> getInteropBrokers() {
		Reflections reflections = new Reflections("org.openmrs.module.interop");
		return reflections.getSubTypesOf(InteropBroker.class);
	}
}
