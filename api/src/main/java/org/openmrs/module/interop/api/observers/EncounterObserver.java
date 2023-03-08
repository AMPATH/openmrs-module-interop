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

import javax.annotation.Nonnull;
import javax.jms.Message;
import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.openmrs.Encounter;
import org.openmrs.LocationAttribute;
import org.openmrs.Obs;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.Event;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.interop.InteropConstant;
import org.openmrs.module.interop.api.Subscribable;
import org.openmrs.module.interop.api.metadata.EventMetadata;
import org.openmrs.module.interop.utils.ObserverUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.openmrs.module.interop.api.InteropBroker;
import org.openmrs.module.interop.api.Subscribable;
import org.openmrs.module.interop.api.metadata.EventMetadata;
import org.openmrs.module.interop.utils.ClassUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component("interop.encounterCreationObserver")
public class EncounterObserver extends BaseObserver implements Subscribable<org.openmrs.Encounter> {
	
	@Autowired
	private EncounterTranslator<Encounter> encounterTranslator;
	
	@Autowired
	private ObservationTranslator observationTranslator;
	
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
			Daemon.runInDaemonThread(() -> prepareEncounterMessage(metadata), getDaemonToken());
		});
	}
	
	private void prepareEncounterMessage(@NotNull EventMetadata metadata) {
		//Create bundle
		Encounter encounter = Context.getEncounterService().getEncounterByUuid(metadata.getString("uuid"));
		Set<Obs> obs;
		
		org.hl7.fhir.r4.model.Encounter encounter1 = encounterTranslator.toFhirResource(encounter);
		
		Bundle encBundle = new Bundle();
		encBundle.setType(Bundle.BundleType.TRANSACTION);
		
		List<PatientIdentifier> nUpi = encounter.getPatient().getActiveIdentifiers().stream()
		        .filter(id -> id.getIdentifierType().getUuid().equals(InteropConstant.NATIONAL_UNIQUE_PATIENT_NUMBER_UUID))
		        .collect(Collectors.toList());
		List<LocationAttribute> mfl = encounter.getLocation().getActiveAttributes().stream()
		        .filter(at -> at.getAttributeType().getUuid().equals(InteropConstant.MFL_LOCATION_ATTRIBUTE_UUID))
		        .collect(Collectors.toList());
		if (!nUpi.isEmpty() && !mfl.isEmpty()) {
			Bundle subjectResource = fetchPatientResource(nUpi.get(0).getIdentifier());
			Patient subject;
			Reference subjectReference = new Reference();
			if (subjectResource.hasEntry()) {
				subject = (Patient) subjectResource.getEntry().get(0).getResource();
				encounter.getPatient().setUuid(getResourceUuid(subject.getId()));
				subjectReference = createPatientReference(subject);
				encounter1.setSubject(subjectReference);
			}
			Bundle facilityResource = fetchLocationResource("29593");
			Location facility;
			if (facilityResource.hasEntry()) {
				facility = (Location) facilityResource.getEntry().get(0).getResource();
				encounter.getLocation().setUuid(getResourceUuid(facility.getId()));
				encounter1.getLocationFirstRep().setLocation(createLocationReference(facility));
				
			}
			
			Reference practitionerRef = createPractitionerReferenceBase(
			    (Practitioner) fetchFhirResource("Practitioner", InteropConstant.INTEROP_PROVIDER_UUID));
			encounter1.setParticipant(new ArrayList<>());
			encounter1.addParticipant(
			    new org.hl7.fhir.r4.model.Encounter.EncounterParticipantComponent().setIndividual(practitionerRef));
			
			//Encounter
			Bundle.BundleEntryComponent encounterEntry = new Bundle.BundleEntryComponent();
			Bundle.BundleEntryRequestComponent ec = new Bundle.BundleEntryRequestComponent();
			ec.setMethod(Bundle.HTTPVerb.POST);
			ec.setUrl("Encounter");
			encounterEntry.setRequest(ec);
			encounterEntry.setResource(encounter1);
			encBundle.addEntry(encounterEntry);
			
			//Observations
			obs = encounter.getObs();
			for (Obs obj : obs) {
				Observation fhirObs = observationTranslator.toFhirResource(obj);
				fhirObs.setSubject(subjectReference);
				Bundle.BundleEntryComponent obsEntry = new Bundle.BundleEntryComponent();
				Bundle.BundleEntryRequestComponent obsC = new Bundle.BundleEntryRequestComponent();
				obsC.setMethod(Bundle.HTTPVerb.POST);
				obsC.setUrl("Observation");
				obsEntry.setRequest(obsC);
				obsEntry.setResource(fhirObs);
				encBundle.addEntry(obsEntry);
			}
			publish(encBundle);
		} else {
			log.error("ONE OF THE REFERENCES WAS NULL");
		}

		this.processBrokers(encounter);
	}
	
	public String getResourceUuid(String resourceUrl) {
		String[] sepUrl = resourceUrl.split("/");
		return sepUrl[sepUrl.length - 3];
	}
	
	public IGenericClient getSourceClient() {
		FhirContext fhirContext = FhirContext.forR4();
		String username = "fhiruser";
		String password = "change-password";
		String serverUrl = "https://41.89.92.203:9443/fhir-server/api/v4/";
		IClientInterceptor authInterceptor = new BasicAuthInterceptor(username, password);
		fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
		
		IGenericClient client = fhirContext.getRestfulClientFactory().newGenericClient(serverUrl);
		client.registerInterceptor(authInterceptor);
		
		return client;
	}
	
	public Bundle fetchPatientResource(String identifier) {
		try {
			
			IGenericClient client = getSourceClient();
			
			Bundle resource = client.search().forResource("Patient").where(Patient.IDENTIFIER.exactly().code(identifier))
			        .returnBundle(Bundle.class).execute();
			log.error("resource " + resource.hasEntry());
			return resource;
		}
		catch (Exception e) {
			log.error(String.format("Failed fetching FHIR resource %s", e));
			return null;
		}
	}
	
	public Bundle fetchLocationResource(String identifier) {
		try {
			IGenericClient client = getSourceClient();
			Bundle resource = client.search().forResource("Location").where(Location.IDENTIFIER.exactly().code(identifier))
			        .returnBundle(Bundle.class).execute();
			return resource;
		}
		catch (Exception e) {
			log.error(String.format("Failed fetching FHIR resource %s", e));
			return null;
		}
	}
	
	public Resource fetchFhirResource(String resourceType, String resourceId) {
		try {
			IGenericClient client = getSourceClient();
			IBaseResource resource = client.read().resource(resourceType).withId(resourceId).execute();
			return (Resource) resource;
		}
		catch (Exception e) {
			log.error(String.format("Failed fetching FHIR %s resource with Id %s: %s", resourceType, resourceId, e));
			return null;
		}
	}
	
	private Reference createPatientReference(@Nonnull Patient patient) {
		Reference reference = new Reference().setReference("/Patient/" + getResourceUuid(patient.getId()))
		        .setType("Patient");
		return reference;
	}
	
	private Reference createPractitionerReferenceBase(@Nonnull Practitioner practitioner) {
		Reference reference = (new Reference()).setReference("Practitioner/" + getResourceUuid(practitioner.getId()))
		        .setType("Practitioner");
		if (!practitioner.getName().isEmpty()) {
			reference.setDisplay(practitioner.getName().get(0).getGivenAsSingleString());
		}
		
		return reference;
	}
	
	protected Reference createLocationReference(@Nonnull Location location) {
		Reference reference = (new Reference()).setReference("Location/" + getResourceUuid(location.getId()))
		        .setType("Location");
		if (!location.getName().isEmpty()) {
			reference.setDisplay(location.getName());
		}
		
		return reference;
	}
	
	private void processThroughBrokers(@NotNull Encounter encounter, Bundle bundle) {
		
	}
	
	private Bundle.BundleEntryComponent getConditionBundleComponent(Condition condition) {
		Bundle.BundleEntryRequestComponent bundleEntryRequestComponent = new Bundle.BundleEntryRequestComponent();
		bundleEntryRequestComponent.setMethod(Bundle.HTTPVerb.POST);
		bundleEntryRequestComponent.setUrl("Condition");
		Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
		bundleEntryComponent.setRequest(bundleEntryRequestComponent);
		bundleEntryComponent.setResource(condition);
		
		log.error("Conditions component: {}", bundleEntryComponent);
		
		return bundleEntryComponent;
	}

	private void processBrokers(@Nonnull Encounter encounter) {
		log.error("encounter {}", encounter);
		Bundle bundle = new Bundle();
		this.processThroughBrokers(encounter, bundle);

		List<Class<? extends InteropBroker>> brokers = new ArrayList<>(ClassUtils.getInteropBrokers());
		log.error("Num of brokers :: {}", brokers.size());
		log.error("Brokers :: {}", brokers);
		brokers.forEach(broker -> {
			InteropBroker newInstanceBroker;
			try {
				newInstanceBroker = broker.getDeclaredConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				log.error("Unable to instantiate {} broker class", broker.getSimpleName());
				throw new RuntimeException(e);
			}
			// Publish to enabled connectors
			List<Condition> conditions = newInstanceBroker.processEncounter(encounter);
			log.error("Conditions {}", conditions);
			conditions.forEach(condition -> bundle.addEntry(getConditionBundleComponent(condition)));
			log.error("++++++++");
			System.out.println("Resources:: " + getFhirContext().newJsonParser().encodeResourceToString(bundle));
		});
	}
}
