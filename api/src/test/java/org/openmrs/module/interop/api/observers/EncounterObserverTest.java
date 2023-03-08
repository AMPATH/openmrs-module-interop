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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.event.Event;
import org.openmrs.module.interop.SpringTestConfiguration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class EncounterObserverTest {
	
	private EncounterObserver encounterObserver;
	
	@Before
	public void setUp() {
		encounterObserver = new EncounterObserver();
	}
	
	@Test
	public void shouldReturnOpenMRSPatientObject() {
		assertThat(encounterObserver.clazz(), notNullValue());
		assertThat(encounterObserver.clazz(), equalTo(Encounter.class));
	}
	
	@Test
	public void verifySpecifiedActions() {
		List<Event.Action> actions = encounterObserver.actions();
		assertThat(actions, hasSize(2));
		assertThat(actions, hasItems(Event.Action.CREATED, Event.Action.UPDATED));
	}
}
