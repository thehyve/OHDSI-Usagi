/*******************************************************************************
 * Copyright 2020 Observational Health Data Sciences and Informatics & The Hyve
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohdsi.usagi;

/**
 * Class for holding information about a single (target) concept in the Vocabulary
 */
public class MappingTarget{
	public enum Type {
		EVENT, VALUE, UNIT, OPERATOR, VISIT, TYPE, CONDITION_STATUS
	};

	public Concept concept;
	public Type mappingType;
	public String createdBy;
	public long createdOn;

	public MappingTarget() {
		this.concept = Concept.createEmptyConcept();
		this.mappingType = Type.EVENT;
		this.createdBy = "";
		this.createdOn = 0;
	}

	public MappingTarget(Concept concept, String createdBy) {
		this(concept, Type.EVENT, createdBy);
	}

	public MappingTarget(Concept concept, Type mappingType, String createdBy) {
		this.concept = concept;
		this.mappingType = mappingType;
		this.createdBy = createdBy;
		this.createdOn = System.currentTimeMillis();
	}

	public MappingTarget(Concept concept, Type mappingType, String createdBy, long createdOn) {
		this.concept = concept;
		this.mappingType = mappingType;
		this.createdBy = createdBy;
		this.createdOn = createdOn;
	}

	public Concept getConcept() {
		return concept;
	}

	public void setConcept(Concept concept) {
		this.concept = concept;
	}

	public Type getMappingType() {
		return mappingType;
	}

	public void setMappingType(Type mappingType) {
		this.mappingType = mappingType;
	}
}
