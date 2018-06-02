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

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import equinox.data.EquinoxTheme;
import equinox.utility.Utility;
import equinoxServer.remote.message.GetBugReportsRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Class for bug report view controls.
 *
 * @author Murat Artim
 * @date Jun 24, 2015
 * @time 3:03:40 PM
 */
public class BugReportViewControls implements Initializable {

	/** The owner panel. */
	private BugReportViewPanel owner_;

	@FXML
	private HBox root_;

	@FXML
	private RadioMenuItem all_, open_, closed_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set root to grow always
		HBox.setHgrow(root_, Priority.ALWAYS);
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public BugReportViewPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public HBox getRoot() {
		return root_;
	}

	/**
	 * Resets this panel.
	 *
	 * @param status
	 *            Status index.
	 */
	public void reset(int status) {
		if (status == GetBugReportsRequest.ALL) {
			all_.setSelected(true);
		}
		else if (status == GetBugReportsRequest.OPEN) {
			open_.setSelected(true);
		}
		else if (status == GetBugReportsRequest.CLOSED) {
			closed_.setSelected(true);
		}
	}

	@FXML
	private void onStatusSelected() {
		if (all_.isSelected()) {
			owner_.onStatusSelected(GetBugReportsRequest.ALL);
		}
		else if (open_.isSelected()) {
			owner_.onStatusSelected(GetBugReportsRequest.OPEN);
		}
		else if (closed_.isSelected()) {
			owner_.onStatusSelected(GetBugReportsRequest.CLOSED);
		}
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static BugReportViewControls load(BugReportViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("BugReportViewControls.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			BugReportViewControls controller = (BugReportViewControls) fxmlLoader.getController();

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
