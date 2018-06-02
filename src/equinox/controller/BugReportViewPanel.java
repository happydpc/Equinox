/*
 * Copyright 2018 Murat Artim (muratartim@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package equinox.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.plugin.FileType;
import equinox.task.GetBugReports;
import equinox.task.SaveImage;
import equinox.utility.Utility;
import equinoxServer.remote.data.BugReport;
import equinoxServer.remote.message.GetBugReportsRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for bug report view panel controller.
 *
 * @author Murat Artim
 * @date Sep 12, 2014
 * @time 4:31:17 PM
 */
public class BugReportViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Control panel. */
	private BugReportViewControls controls_;

	@FXML
	private VBox root_, container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create controls
		controls_ = BugReportViewControls.load(this);
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public void hiding() {
		// no implementation
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public HBox getControls() {
		return controls_.getRoot();
	}

	@Override
	public String getHeader() {
		return "Bug Reports";
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Bug Reports" + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = container_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	@Override
	public String getViewName() {
		return "Bug Reports";
	}

	@Override
	public WritableImage getViewImage() {
		return container_.snapshot(null, null);
	}

	/**
	 * Called when status changed.
	 *
	 * @param status
	 *            Bug status.
	 */
	public void onStatusSelected(int status) {

		// get bug reports
		if (status == GetBugReportsRequest.ALL) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetBugReports(GetBugReportsRequest.ALL));
		}
		else if (status == GetBugReportsRequest.OPEN) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetBugReports(GetBugReportsRequest.OPEN));
		}
		else if (status == GetBugReportsRequest.CLOSED) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetBugReports(GetBugReportsRequest.CLOSED));
		}
	}

	/**
	 * Sets bug reports to this panel.
	 *
	 * @param reports
	 *            Reports to set.
	 * @param status
	 *            Status.
	 */
	public void setReports(ArrayList<BugReport> reports, int status) {

		try {

			// clear container
			container_.getChildren().clear();

			// create list for wish panels
			ArrayList<VBox> panels = new ArrayList<>();

			// add results to container
			for (BugReport item : reports) {
				BugPanel panel = BugPanel.load(this);
				panel.setReport(item);
				panels.add(panel.getRoot());
			}
			container_.getChildren().addAll(panels);

			// reset controls
			controls_.reset(status);
		}

		// exception occurred
		catch (Exception e) {

			// log exception
			Equinox.LOGGER.log(Level.WARNING, "Exception occured during setting bug reports.", e);

			// create and show notification
			String message = "Exception occurred during setting bug reports: " + e.getLocalizedMessage();
			message += " Click 'Details' for more information.";
			owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
		}
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static BugReportViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("BugReportViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			BugReportViewPanel controller = (BugReportViewPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
