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
package org.ohdsi.usagi.ui;

import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class AboutDialog extends JDialog {

	private static final long	serialVersionUID	= 2028328868610404663L;

	public AboutDialog() {
		setTitle("About Usagi v" + UsagiMain.version);
		setLayout(new GridBagLayout());

		GridBagConstraints g = new GridBagConstraints();
		g.fill = GridBagConstraints.BOTH;
		g.ipadx = 10;
		g.ipady = 10;

		g.gridx = 0;
		g.gridy = 0;

		Image icon = Toolkit.getDefaultToolkit().getImage(UsagiMain.class.getResource("Usagi64.png"));
		add(new JLabel(new ImageIcon(icon)), g);

		g.gridx = 1;
		g.gridy = 0;

		JEditorPane text = new JEditorPane(
				"text/html",
				"Usagi is maintained by The Hyve (www.thehyve.nl), and originally developed by Martijn Schuemie" +
						"<br/>in <a href=\"http://ohdsi.org\">Observational Health Data Sciences and Informatics</a> (OHDSI)." +
						"<br/><br/>For help, please review the <a href =\"http://www.ohdsi.org/web/wiki/doku.php?id=documentation:software:usagi\">Usagi Wiki</a>." +
						"<br/><br/>Equivalence definitions based on <a href=\"https://www.hl7.org/fhir/valueset-concept-map-equivalence.html\">HL7 concept-map-quivalence</a>:" +
						"<ul>" +
						"<li>Equal = The concepts are exactly the same (i.e. intentionally identical).</li>" +
						"<li>Equivalent = The concepts mean the same thing (i.e. extensionally identical).</li>" +
						"<li>Wider = The target contains more information than to the source.</li>" +
						"<li>Narrower = The target contains less information than the source.</li>" +
						"<li>Inexact = The target overlaps with the source, but both source and target cover additional meaning.</li>" +
						"<li>Unmatched = There is no match for this concept in the target code system.</li>" +
						"</ul>"
		);

		text.setEditable(false);
		text.setOpaque(false);

		text.addHyperlinkListener(event -> {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) {
				try {
					Desktop desktop = Desktop.getDesktop();
					desktop.browse(new URI(event.getURL().toString()));
				} catch (URISyntaxException | IOException ex) {
					// url could not be opened
				}
			}
		});
		add(text, g);

		g.gridx = 0;
		g.gridy = 1;
		g.gridwidth = 2;

		JPanel buttonPanel = new JPanel();

		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(Box.createHorizontalGlue());

		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				close();

			}
		});
		buttonPanel.add(okButton);
		buttonPanel.add(Box.createHorizontalGlue());

		add(buttonPanel, g);

		setModal(true);
		setResizable(false);
		pack();

	}

	private void close() {
		setVisible(false);
	}
}
