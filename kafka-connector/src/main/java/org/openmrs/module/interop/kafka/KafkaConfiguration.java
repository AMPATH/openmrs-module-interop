/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.kafka;

import static org.openmrs.module.interop.kafka.KafkaConnectorConstants.GP_KAFKA_CLIENT_ID;
import static org.openmrs.module.interop.kafka.KafkaConnectorConstants.GP_KAFKA_KEY_SERIALIZERS;
import static org.openmrs.module.interop.kafka.KafkaConnectorConstants.GP_KAFKA_SERVER_URL;
import static org.openmrs.module.interop.kafka.KafkaConnectorConstants.GP_KAFKA_VALUE_SERIALIZERS;

import java.util.Properties;

import org.openmrs.api.AdministrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class KafkaConfiguration {
	
	@Autowired
	@Qualifier("adminService")
	private AdministrationService administrationService;
	
	public Properties getProperties() {
		Properties properties = new Properties();
		properties.put("bootstrap.servers", getKafkaServerUrl());
		properties.put("client.id", getClientId());
		properties.put("key.serializer", getKeySerializer());
		properties.put("value.serializer", getValueSerializer());
		return properties;
	}
	
	private String getKafkaServerUrl() {
		return administrationService.getGlobalProperty(GP_KAFKA_SERVER_URL);
	}
	
	private String getClientId() {
		return administrationService.getGlobalProperty(GP_KAFKA_CLIENT_ID);
	}
	
	private String getKeySerializer() {
		return administrationService.getGlobalProperty(GP_KAFKA_KEY_SERIALIZERS);
	}
	
	private String getValueSerializer() {
		return administrationService.getGlobalProperty(GP_KAFKA_VALUE_SERIALIZERS);
	}
}
