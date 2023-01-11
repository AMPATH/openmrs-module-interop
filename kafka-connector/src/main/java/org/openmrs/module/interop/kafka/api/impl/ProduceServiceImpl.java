/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.kafka.api.impl;

import groovy.util.logging.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.openmrs.module.interop.kafka.KafkaConfiguration;
import org.openmrs.module.interop.kafka.api.ProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProduceServiceImpl implements ProducerService<String, String> {
	
	@Autowired
	private KafkaConfiguration config;
	
	@Override
	public void produce(String topic, String key, String value) {
		long startTime = System.currentTimeMillis();
		try (KafkaProducer<String, String> producer = new KafkaProducer<>(config.getProperties())) {
			RecordMetadata metadata = producer.send(new ProducerRecord<>(topic, key, value)).get();
			System.out.println("Message with id: '" + key + "' sent to partition(" + metadata.partition() + "), offset("
			        + metadata.offset() + ") in " + (System.currentTimeMillis() - startTime) + " ms");
		}
		catch (Exception e) {
			// error handling code
		}
	}
}
