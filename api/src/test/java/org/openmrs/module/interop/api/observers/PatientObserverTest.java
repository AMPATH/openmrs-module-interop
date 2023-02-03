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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.event.Event;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.interop.SpringTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class PatientObserverTest {
	
	@Autowired
	private FhirPatientService fhirPatientService;
	
	private PatientObserver patientObserver;
	
	@Before
	public void setUp() {
		patientObserver = new PatientObserver();
		patientObserver.setFhirPatientService(fhirPatientService);
		Event.subscribe(Patient.class, "CREATED", patientObserver);
	}
	
	@Test
	public void shouldReturnOpenMRSPatientObject() {
		assertThat(patientObserver.clazz(), notNullValue());
		assertThat(patientObserver.clazz(), equalTo(Patient.class));
	}
	
	@Test
	public void verifySpecifiedActions() {
		List<Event.Action> actions = patientObserver.actions();
		assertThat(actions, hasSize(5));
		assertThat(actions, hasItems(Event.Action.CREATED, Event.Action.VOIDED, Event.Action.UPDATED));
	}
	
	@Test
	public void shouldDetermineCreatedAction() {
		Optional<String> destinationMessage = Optional.of("topic://CREATED:org.openmrs.Patient");
		Optional<Event.Action> action = patientObserver.determineAction(destinationMessage);
		
		//assertions
		assertThat(action.isPresent(), is(true));
		assertThat(action.get(), is(Event.Action.CREATED));
	}
	
	@Test
	public void shouldDetermineUpdatedAction() {
		Optional<String> destinationMessage = Optional.of("topic://UPDATED:org.openmrs.Patient");
		Optional<Event.Action> action = patientObserver.determineAction(destinationMessage);
		
		//assertions
		assertThat(action.isPresent(), is(true));
		assertThat(action.get(), is(Event.Action.UPDATED));
	}
	
	@Test
	public void shouldDetermineVoidedAction() {
		Optional<String> destinationMessage = Optional.of("topic://VOIDED:org.openmrs.Patient");
		Optional<Event.Action> action = patientObserver.determineAction(destinationMessage);
		
		//assertions
		assertThat(action.isPresent(), is(true));
		assertThat(action.get(), is(Event.Action.VOIDED));
	}
}
