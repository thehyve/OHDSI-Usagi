/*******************************************************************************
 * Copyright 2019 Observational Health Data Sciences and Informatics
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
package org.ohdsi.usagi.ui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.ohdsi.usagi.CodeMapping;
import org.ohdsi.usagi.ui.Global;
import org.ohdsi.usagi.ui.Mapping;
import org.ohdsi.utilities.collections.Pair;

import static org.ohdsi.usagi.ui.DataChangeEvent.*;

public class ApplyPreviousMappingAction extends AbstractAction {

	private static final long serialVersionUID = 3420357922150237898L;

	public ApplyPreviousMappingAction() {
		putValue(Action.NAME, "Apply previous mapping");
		putValue(Action.SHORT_DESCRIPTION, "Apply previous mapping to current code set");
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		JFileChooser fileChooser = new JFileChooser(Global.folder);
		FileFilter csvFilter = new FileNameExtensionFilter("CSV files", "csv");
		fileChooser.setFileFilter(csvFilter);
		if (fileChooser.showOpenDialog(Global.frame) == JFileChooser.APPROVE_OPTION) {
			int mappingsApplied = 0;
			int mappingsAdded = 0;

			// Existing code lookup
			Map<String, CodeMapping> codeToMapping = new HashMap<>();
			for (CodeMapping codeMapping: Global.mapping) {
				codeToMapping.put(codeMapping.getSourceCode().sourceCode, codeMapping);
			}

			// Open mapping file to be applied
			File file = fileChooser.getSelectedFile();
			Mapping mappingToBeApplied = new Mapping();
			mappingToBeApplied.loadFromFile(file.getAbsolutePath());

			// Additional columns
			List<String> existingAdditionalColumnNames = Global.mapping.getAdditionalColumnNames();

			// Apply mapping. Add mappings not currently present
			ApplyPreviousChangeSummary summary = new ApplyPreviousChangeSummary();
			for (CodeMapping codeMappingToBeApplied : mappingToBeApplied) {
				CodeMapping existingMapping = codeToMapping.get(codeMappingToBeApplied.getSourceCode().sourceCode);
				if (existingMapping != null) {
					summary.compare(existingMapping, codeMappingToBeApplied);
					existingMapping.getSourceCode().sourceName = codeMappingToBeApplied.getSourceCode().sourceName;
					existingMapping.setTargetConcepts(codeMappingToBeApplied.getTargetConcepts());
					existingMapping.setMappingStatus(codeMappingToBeApplied.getMappingStatus());
					existingMapping.setAssignedReviewer(codeMappingToBeApplied.getAssignedReviewer());
					existingMapping.setEquivalence(codeMappingToBeApplied.getEquivalence());
					existingMapping.setComment(codeMappingToBeApplied.getComment());
					existingMapping.setStatusSetBy(codeMappingToBeApplied.getStatusSetBy());
					existingMapping.setStatusSetOn(codeMappingToBeApplied.getStatusSetOn());
					mappingsApplied++;
				} else {
					// Add empty additional columns if these existed in existing mapping
					codeMappingToBeApplied.getSourceCode().sourceAdditionalInfo = existingAdditionalColumnNames.stream()
							.map(x -> new Pair<>(x, ""))
							.collect(Collectors.toList());
					Global.mapping.add(codeMappingToBeApplied);
					mappingsAdded++;
				}
			}

			String message = "The applied mapping contained " + mappingToBeApplied.size() + " mappings of which " + mappingsApplied
					+ " were applied to the current mapping and " + mappingsAdded + " were newly added.\n\n" + summary.createReport();
			Global.mappingTablePanel.updateUI();
			Global.mappingDetailPanel.updateUI();
			Global.mapping.fireDataChanged(APPROVE_EVENT); // To update the footer
			if (mappingsAdded > 0) {
				Global.usagiSearchEngine.close();
				Global.usagiSearchEngine.createDerivedIndex(Global.mapping.getSourceCodes(), Global.frame);
				Global.mappingDetailPanel.doSearch();
			}
			JOptionPane.showMessageDialog(Global.frame, message, "Summary", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private static class ApplyPreviousChangeSummary {
		private int nChanged = 0;
		private int nSourceNameChanged = 0;
		private int nTargetConceptsChanged = 0;
		private int nMappingStatusChanged = 0;
		private int nEquivalenceChanged = 0;

		private void compare(CodeMapping A, CodeMapping B) {
			boolean hasChanged = false;
			if (!A.getSourceCode().sourceName.equals(B.getSourceCode().sourceName)) {
				nSourceNameChanged++;
				hasChanged = true;
			}
			if (!A.getTargetConcepts().equals(B.getTargetConcepts())) {
				nTargetConceptsChanged++; // This could be target concept, size OR type
				hasChanged = true;
			}
			if (!A.getMappingStatus().equals(B.getMappingStatus())) {
				nMappingStatusChanged++;
				hasChanged = true;
			}
			if (!A.getEquivalence().equals(B.getEquivalence())) {
				nEquivalenceChanged++;
				hasChanged = true;
			}
			if (hasChanged) {
				nChanged++;
			}
		}

		private String createReport() {
			StringBuilder report = new StringBuilder();
			report.append("Of the applied mappings, " + nChanged + " mappings changed.");
			report.append("\n\tSource name: " + nSourceNameChanged);
			report.append("\n\tTarget concept: " + nTargetConceptsChanged + "\t(changed target concept, target type AND/OR number of targets)");
			report.append("\n\tMapping status: " + nMappingStatusChanged);
			report.append("\n\tMapping equivalence: " + nEquivalenceChanged);
			return report.toString();
		}
	}
}
