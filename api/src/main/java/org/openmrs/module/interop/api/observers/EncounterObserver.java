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

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.Event;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.Subscribable;
import org.openmrs.module.interop.api.processors.AppointmentProcessor;
import org.openmrs.module.interop.api.metadata.EventMetadata;
import org.openmrs.module.interop.api.processors.ConditionProcessor;
import org.openmrs.module.interop.utils.ObserverUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.jms.Message;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("interop.encounterCreationObserver")
public class EncounterObserver extends BaseObserver implements Subscribable<org.openmrs.Encounter> {
	
	@Autowired
	private EncounterTranslator<Encounter> encounterTranslator;
	
	@Autowired
	private ObservationTranslator observationTranslator;
	
	@Autowired
	private ConditionProcessor conditionProcessor;
	
	@Autowired
	private AppointmentProcessor appointmentProcessor;
	
	@Override
	public Class<?> clazz() {
		return Encounter.class;
	}
	
	@Override
	public List<Event.Action> actions() {
		return ObserverUtils.defaultActions();
	}
	
	@Override
	public void onMessage(Message message) {
		processMessage(message).ifPresent(metadata -> {
			//formatter:off
			Daemon.runInDaemonThread(() -> prepareEncounterMessage(metadata), getDaemonToken());
			//formatter:on
		});
	}
	
	private void prepareEncounterMessage(@NotNull EventMetadata metadata) {
		//Create bundle
		Encounter encounter = Context.getEncounterService().getEncounterByUuid(metadata.getString("uuid"));
		Bundle preparedBundle = new Bundle();
		
		this.processBrokers(encounter, preparedBundle);

		org.hl7.fhir.r4.model.Encounter fhirEncounter = encounterTranslator.toFhirResource(encounter);
		fhirEncounter.getSubject().setIdentifier(buildPatientUpiIdentifier(encounter.getPatient()));
		
		Bundle.BundleEntryComponent encounterBundleEntryComponent = new Bundle.BundleEntryComponent();
		Bundle.BundleEntryRequestComponent bundleEntryRequestComponent = new Bundle.BundleEntryRequestComponent();
		bundleEntryRequestComponent.setMethod(Bundle.HTTPVerb.POST);
		bundleEntryRequestComponent.setUrl("Encounter");
		encounterBundleEntryComponent.setRequest(bundleEntryRequestComponent);
		encounterBundleEntryComponent.setResource(fhirEncounter);
		preparedBundle.addEntry(encounterBundleEntryComponent);
		
		//Observations
		List<Obs> encounterObservations = new ArrayList<>(encounter.getObs());
		for (Obs obs : encounterObservations) {
			Observation fhirObs = observationTranslator.toFhirResource(obs);
			fhirObs.getSubject().setIdentifier(buildPatientUpiIdentifier(encounter.getPatient()));

			Bundle.BundleEntryComponent obsBundleEntry = new Bundle.BundleEntryComponent();
			Bundle.BundleEntryRequestComponent requestComponent = new Bundle.BundleEntryRequestComponent();
			requestComponent.setMethod(Bundle.HTTPVerb.POST);
			requestComponent.setUrl("Observation");
			obsBundleEntry.setRequest(requestComponent);
			obsBundleEntry.setResource(fhirObs);
			preparedBundle.addEntry(obsBundleEntry);
		}
		
		log.error("Bundled resources :: {}",
		    getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(preparedBundle));
		
		this.publish(preparedBundle);
	}
	
	private Bundle.BundleEntryComponent buildConditionBundleEntry(Condition condition) {
		Bundle.BundleEntryRequestComponent bundleEntryRequestComponent = new Bundle.BundleEntryRequestComponent();
		bundleEntryRequestComponent.setMethod(Bundle.HTTPVerb.POST);
		bundleEntryRequestComponent.setUrl("Condition");
		Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
		bundleEntryComponent.setRequest(bundleEntryRequestComponent);
		bundleEntryComponent.setResource(condition);
		return bundleEntryComponent;
	}
	
	private Bundle.BundleEntryComponent getAppointmentBundleComponent(Appointment appointment) {
		Bundle.BundleEntryRequestComponent bundleEntryRequestComponent = new Bundle.BundleEntryRequestComponent();
		bundleEntryRequestComponent.setMethod(Bundle.HTTPVerb.POST);
		bundleEntryRequestComponent.setUrl("Appointment");
		Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
		bundleEntryComponent.setRequest(bundleEntryRequestComponent);
		bundleEntryComponent.setResource(appointment);
		return bundleEntryComponent;
	}
	
	private void processBrokers(@Nonnull Encounter encounter, @NotNull Bundle bundle) {
		List<Condition> conditions = conditionProcessor.process(encounter);
		conditions.forEach(condition -> {
			condition.getSubject().setIdentifier(buildPatientUpiIdentifier(encounter.getPatient()));
			condition.getRecorder().setIdentifier(buildProviderIdentifier(encounter));
			bundle.addEntry(buildConditionBundleEntry(condition));
		});

		List<Appointment> appointments = appointmentProcessor.process(encounter);
		appointments.forEach(appointment -> {
			for (Appointment.AppointmentParticipantComponent participantComponent : appointment.getParticipant()) {
				participantComponent.getActor().setIdentifier(buildPatientUpiIdentifier(encounter.getPatient()));
			}
			bundle.addEntry(getAppointmentBundleComponent(appointment));
		});
		
	}

	private Identifier buildPatientUpiIdentifier(@NotNull Patient patient) {
		Identifier identifier = new Identifier();
		identifier.setSystem(InteropConstant.SYSTEM_URL);
		identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
		identifier.setValue(getPatientNUPI(patient));

		return identifier;
	}

	private Identifier buildProviderIdentifier(@NotNull Encounter encounter) {
		Identifier identifier = new Identifier();
		identifier.setSystem(InteropConstant.SYSTEM_URL);
		identifier.setUse(Identifier.IdentifierUse.OFFICIAL);

		List<String> identifiers = getProviderUniversalIdentifiers(encounter);
		if (!identifiers.isEmpty()) {
			identifier.setValue(identifiers.get(0));
		}

		return identifier;
	}
	
	private String getPatientNUPI(Patient patient) {
		if (ObserverUtils.getNUPIIdentifierType() != null) {
			List<PatientIdentifier> nUpi = patient.getActiveIdentifiers().stream()
			        .filter(id -> id.getIdentifierType().getUuid().equals(ObserverUtils.getNUPIIdentifierType().getUuid()))
			        .collect(Collectors.toList());
			return nUpi.isEmpty() ? "" : nUpi.get(0).getIdentifier();
		}
		return "";
	}
	
	private List<String> getProviderUniversalIdentifiers(Encounter encounter) {
		List<String> practitioners = new ArrayList<>();
		List<EncounterProvider> encounterProviders = new ArrayList<>(encounter.getActiveEncounterProviders());
		encounterProviders.forEach(provider -> practitioners.add(providerUniversalIdentifier(provider.getProvider())));
		return practitioners;
	}
	
	private String providerUniversalIdentifier(Provider provider) {
		List<ProviderAttribute> attributes = provider.getActiveAttributes().stream().filter(
		    attribute -> attribute.getAttributeType().getUuid().equals(ObserverUtils.getProviderAttributeType().getUuid()))
		        .collect(Collectors.toList());
		
		return attributes.isEmpty() ? "" : attributes.get(0).getValue().toString();
	}
}
