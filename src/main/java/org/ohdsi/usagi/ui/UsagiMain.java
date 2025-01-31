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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.*;

import org.apache.poi.util.StringUtil;
import org.ohdsi.usagi.BerkeleyDbEngine;
import org.ohdsi.usagi.UsagiSearchEngine;
import org.ohdsi.usagi.ui.actions.*;
import org.ohdsi.utilities.StringUtilities;
import org.ohdsi.utilities.files.ReadTextFile;

/**
 * The main application class
 */
public class UsagiMain implements ActionListener {

	private static final String version = getVersionFromProperties();
	public static final String NO_VERSION = "(no version)";

	public static void main(String[] args) {
		new UsagiMain(true, args);
	}

	private final JFrame frame;
	private AuthorDialog authorDialog;
	private boolean skipRebuildIndexAction = false;

	public UsagiMain(boolean doInitialize, String[] args) {
		frame = new JFrame("Usagi v" + UsagiMain.version);
		if (doInitialize) {
			initializeUsagi(args);
		}
	}

	public void initializeUsagi(String[] args) {
		// Initialize global variables:
		Global.mapping = new Mapping();
		if (args.length == 1) {
			Global.folder = args[0];
		} else {
			Global.folder = new File("").getAbsolutePath();
		}
		Global.usagiSearchEngine = new UsagiSearchEngine(Global.folder);
		Global.dbEngine = new BerkeleyDbEngine(Global.folder);
		if (Global.usagiSearchEngine.mainIndexExists()) {
			Global.usagiSearchEngine.openIndexForSearching(false);
			Global.dbEngine.openForReading();
		}

		loadAuthor(Global.folder);
		loadVocabularyVersion(Global.folder);
		Global.conceptClassIds = loadVectorFromFile(Global.folder + "/ConceptClassIds.txt");
		Global.vocabularyIds = loadVectorFromFile(Global.folder + "/VocabularyIds.txt");
		Global.domainIds = loadVectorFromFile(Global.folder + "/DomainIds.txt");

		Global.conceptInformationDialog = new ConceptInformationDialog();
		Global.frame = frame;
		Global.openAction = new OpenAction();
		Global.applyPreviousMappingAction = new ApplyPreviousMappingAction();
		Global.importAction = new ImportAction();
		Global.exportAction = new ExportSourceToConceptMapAction();
		Global.exportForReviewAction = new ExportForReviewAction();
		Global.saveAction = new SaveAction();
		Global.saveAsAction = new SaveAsAction();
		Global.approveAction = new ApproveAction();
		Global.flagAction = new FlagAction();
		Global.reviewerAssignmentAction = new ReviewerAssignmentAction();
		Global.conceptInfoAction = new ConceptInformationAction();
		Global.athenaAction = new AthenaAction();
		Global.googleSearchAction = new GoogleSearchAction();
		Global.showStatsAction = new ShowStatsAction();
		Global.showReviewStatsAction = new ShowReviewStatsAction();
		Global.aboutAction = new AboutAction();
		Global.rebuildIndexAction = new RebuildIndexAction();
		Global.exitAction = new ExitAction();

		Global.applyPreviousMappingAction.setEnabled(false);
		Global.saveAction.setEnabled(false);
		Global.saveAsAction.setEnabled(false);
		Global.exportAction.setEnabled(false);
		Global.exportForReviewAction.setEnabled(false);
		Global.approveAction.setEnabled(false);
		Global.flagAction.setEnabled(false);
		Global.clearSelectedAction = new ClearSelectedAction();
		Global.clearSelectedAction.setEnabled(false);
		Global.conceptInfoAction.setEnabled(false);
		Global.athenaAction.setEnabled(false);
		Global.googleSearchAction.setEnabled(false);

		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (UsagiDialogs.askBeforeClose()) {
					Global.dbEngine.shutdown();
					System.exit(0);
				}
			}
		});
		frame.setLayout(new BorderLayout());
		frame.setJMenuBar(new UsagiMenubar());

		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());

		Global.mappingTablePanel = new MappingTablePanel();
		Global.mappingDetailPanel = new MappingDetailPanel();
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Global.mappingTablePanel, Global.mappingDetailPanel);
		Global.mappingTablePanel.setMinimumSize(new Dimension(500, 100));
		Global.mappingDetailPanel.setMinimumSize(new Dimension(500, 400));

		main.add(splitPane, BorderLayout.CENTER);

		Global.mappingTablePanel.addCodeSelectedListener(Global.mappingDetailPanel);
		frame.add(main, BorderLayout.CENTER);

		Global.statusBar = new UsagiStatusBar();
		frame.add(Global.statusBar, BorderLayout.SOUTH);

		loadIcons(frame);
		frame.pack();
		frame.setVisible(true);

		if (!Global.usagiSearchEngine.mainIndexExists()) {
			if (!skipRebuildIndexAction) {
				Global.rebuildIndexAction.actionPerformed(null);
			}
		}

		if (args.length > 1 && args[0].equals("--file")) {
			OpenAction.open(new File(args[1]));
		}
	}

	private void loadVocabularyVersion(String folder) {
		String versionFileName = folder + "/vocabularyVersion.txt";
		Global.vocabularyVersion = "Unknown";
		if (new File(versionFileName).exists()) {
			for (String line : new ReadTextFile(versionFileName)) {
				Global.vocabularyVersion = line;
			}
		}
	}

	private void loadAuthor(String folder) {
		String authorFileName = folder + "/authorName.txt";
		if (new File(authorFileName).exists()) {
			// Read from file
			for (String line : new ReadTextFile(authorFileName)) {
				Global.author = line;
			}
		} else {
			// Dialog to ask user to input name
			AuthorDialog authorDialog = new AuthorDialog();
			authorDialog.setAuthorFileName(authorFileName);
			authorDialog.setVisible(true);
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {

	}

	protected static void loadIcons(JFrame f) {
		List<Image> icons = new ArrayList<Image>();
		icons.add(loadIcon("/Usagi16.png", f));
		icons.add(loadIcon("/Usagi32.png", f));
		icons.add(loadIcon("/Usagi48.png", f));
		icons.add(loadIcon("/Usagi64.png", f));
		icons.add(loadIcon("/Usagi128.png", f));
		icons.add(loadIcon("/Usagi256.png", f));
		f.setIconImages(icons);
	}

	private static Image loadIcon(String name, JFrame f) {
		Image icon = Toolkit.getDefaultToolkit().getImage(UsagiMain.class.getResource(name));
		MediaTracker mediaTracker = new MediaTracker(f);
		mediaTracker.addImage(icon, 0);
		try {
			mediaTracker.waitForID(0);
			return icon;
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}

	private Vector<String> loadVectorFromFile(String fileName) {
		if (new File(fileName).exists()) {
			Vector<String> vector = new Vector<>();
			for (String line : new ReadTextFile(fileName))
				vector.add(line);
			return vector;
		} else
			return new Vector<>();
	}

	public JFrame getFrame() {
		return this.frame;
	}

	/*
	 * For automated test purposes only!
	 * The design of UsagiMain is a bit unfortunate for automated UI tests in the sense that some UI
	 * actions are triggered before the main application window is properly initialized. This method allows
	 * test code to let UsagiMain skip the rebuildIndexAction.
	 */
	public void setSkipRebuildIndexAction(boolean doSkip) {
		this.skipRebuildIndexAction = doSkip;
	}

	public static String getVersion() {
		return version;
	}

	private static String getVersionFromProperties() {
		String version = NO_VERSION;
		final Properties properties = new Properties();
		try {
			properties.load(UsagiMain.class.getClassLoader().getResourceAsStream("project.properties"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		String versionProperty = properties.getProperty("org.ohdsi.usagi.version");
		if (versionProperty != null && !versionProperty.isEmpty()) {
			version = versionProperty;
		}
		return version;
	}
}
