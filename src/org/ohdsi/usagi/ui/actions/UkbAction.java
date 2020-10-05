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

import org.ohdsi.usagi.CodeMapping;
import org.ohdsi.usagi.Concept;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class UkbAction extends AbstractAction {

	private static final long serialVersionUID = -46340646508713618L;
	private static final String UKB_URL = "https://biobank.ctsu.ox.ac.uk/crystal/field.cgi?id=";
	private String selectedCode;

	public UkbAction() {
		putValue(Action.NAME, "UKB (web)");
		putValue(Action.SHORT_DESCRIPTION, "Link out to UKB data dictionary, showing page of currently selected UKB code.");
		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK));
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		try {
			Desktop desktop = Desktop.getDesktop();
			desktop.browse(new URI(UKB_URL + selectedCode));
		} catch (URISyntaxException | IOException ex) {

		}
	}

	public void setCode(String ukbCode) {
		selectedCode = ukbCode;
	}
}
