/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.interop.NhddMapping.CsvUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import com.opencsv.bean.CsvToBeanBuilder;

public class CsvParser {
	
	private File fileName;
	
	public CsvParser(File fileName) {
		this.fileName = fileName;
	}
	
	public List<NHDDConceptMapDTO> getConcepts() throws FileNotFoundException {
		List<NHDDConceptMapDTO> concepts = new CsvToBeanBuilder<NHDDConceptMapDTO>(new FileReader(fileName))
		        .withType(NHDDConceptMapDTO.class).build().parse();
		
		return concepts;
	}
	
}
