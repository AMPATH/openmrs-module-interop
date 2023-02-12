/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.NhddMapping.translations;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.openmrs.Concept;
import org.openmrs.module.fhir2.api.translators.impl.ConceptTranslatorImpl;
import org.openmrs.module.interop.NhddMapping.CsvUtil.CsvParser;
import org.openmrs.module.interop.NhddMapping.CsvUtil.NHDDConceptMapDTO;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
public class InteropConceptTranslatorImpl extends ConceptTranslatorImpl {
	
	List<NHDDConceptMapDTO> concepts = new ArrayList<>();
	
	@Override
	public CodeableConcept toFhirResource(@Nonnull Concept concept) {
		CodeableConcept codeableConcept = super.toFhirResource(concept);
		if (codeableConcept == null) {
			return null;
		}
		initConcepts();
		
		if (!codeableConcept.getCoding().isEmpty()) {
			List<Coding> cielCode = codeableConcept.getCoding().stream().filter(ci -> {
				if (ci.getSystem() != null) {
					return ci.getSystem().equals("https://openconceptlab.org/orgs/CIEL/sources/CIEL");
				}
				return false;
			}).collect(Collectors.toList());
			if (!cielCode.isEmpty()) {
				concepts = concepts.stream().filter((i) -> i.getCIELId().equals(cielCode.get(0).getCode()))
				        .collect(Collectors.toList());
				if (!concepts.isEmpty()) {
					NHDDConceptMapDTO c = concepts.get(0);
					codeableConcept.setCoding(new ArrayList<>());
					Coding coding = new Coding();
					coding.setCode(c.getNhddId());
					coding.setDisplay(cielCode.get(0).getDisplay());
					coding.setSystem("https://nhdd.health.go.ke/");
					codeableConcept.getCoding().add(coding);
				}
				
			}
		}
		return codeableConcept;
	}
	
	@Override
	public Concept toOpenmrsType(@Nonnull CodeableConcept concept) {
		return super.toOpenmrsType(concept);
	}
	
	private void initConcepts() {
		try {
			URL resource = this.getClass().getClassLoader().getResource("metadata/NHDDConceptMapping.csv");
			File file = Paths.get(resource.toURI()).toFile();
			
			CsvParser parser = new CsvParser(file);
			
			try {
				concepts = parser.getConcepts();
			}
			catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
