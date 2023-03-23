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

public class InteropConstant {
	
	public static final String GP_ENABLE_KAFKA = "interop.enableKafka";
	
	public static final String GP_ENABLE_OPENHIM = "interop.enableOpenHIM";
	
	public static final String NATIONAL_UNIQUE_PATIENT_NUMBER_UUID = "interop.nupi.patientIdentifierTypeUuid";
	
	public static final String INTEROP_MFLCODE_LOCATION_ATTRIBUTE_TYPE_UUID = "interop.mflcode.locationAttributeTypeUuid";
	
	public static final String INTEROP_PROVIDER_ATTRIBUTE_TYPE_UUID = "interop.practitionerAttributeTypeUuid";
	
	public static String CONDITIONS_CONCEPT_UUID = "interop.conditions";
	
	public static String CONDITION_BROKER_ENCOUNTER_TYPE_UUIDS = "interop.encounterTypes.enabled";
	
	public static String SYSTEM_URL = "interop.system.url.configuration";
	
	public static String APPOINTMENT_PROCESSOR_ENCOUNTER_TYPE_UUIDS = "interop.encounterTypes.appointments";
	
	public static String APPOINTMENT_WITH_CODED_TYPES = "interop.appointmentWithCodedTypes";
	
	public static String APPOINTMENT_WITH_NON_CODED_TYPES = "interop.appointmentWithNonCodedTypes";
	
	public static String ALLERGY_PROCESSOR_ENCOUNTER_TYPE_UUIDS = "interop.encounterTypes.allergyIntolerance";
	
	public static String ALLERGY_CONCEPT_UUID = "interop.allergyIntolerance";
	
	public static String DIAGNOSIS_TREATMENT_PLAN_CONCEPT_UUID = "interop.treatmentPlan";
	
}
