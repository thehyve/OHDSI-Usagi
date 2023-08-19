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

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.ohdsi.usagi.ui.Global;

public class ClearSelectedAction extends AbstractAction {

	private static final long	serialVersionUID	= 3420357922150237898L;

	public ClearSelectedAction() {
		putValue(Action.NAME, "Clear selected");
		putValue(Action.SHORT_DESCRIPTION, "Clear all selected mappings (set target to 0)");
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Global.mappingTablePanel.clearSelected();
	}

}
