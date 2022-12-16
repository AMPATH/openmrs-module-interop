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

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import groovy.util.logging.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.openmrs.module.interop.kafka.api.ProducerService;

@Slf4j
public class ProduceServiceImpl implements ProducerService<String, String> {
	
	private final KafkaProducer<String, String> producer;
	
	private final String topic;
	
	public static final String KAFKA_SERVER_URL = "localhost";
	
	public static final int KAFKA_SERVER_PORT = 9092;
	
	public static final String CLIENT_ID = "demo-producer";
	
	public ProduceServiceImpl(String topic) {
		this.topic = topic;
		this.producer = new KafkaProducer<>(getProperties());
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		properties.put("bootstrap.servers", KAFKA_SERVER_URL + ":" + KAFKA_SERVER_PORT);
		properties.put("client.id", CLIENT_ID);
		properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		return properties;
	}
	
	@Override
	public void produce(String key, String value) throws ExecutionException, InterruptedException {
		long startTime = System.currentTimeMillis();
		RecordMetadata metadata = producer.send(new ProducerRecord<>(topic, key, value)).get();
		System.out.println("Message with id: '" + key + "' sent to partition(" + metadata.partition() + "), offset("
		        + metadata.offset() + ") in " + (System.currentTimeMillis() - startTime) + " ms");
	}
}
