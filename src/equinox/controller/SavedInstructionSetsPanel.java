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

import java.net.URL;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.data.ui.SavedInstructionSetItem;
import equinox.serverUtilities.ServerUtility;
import equinox.task.DeleteInstructionSets;
import equinox.task.DeleteSavedTasks;
import equinox.task.RunSavedTasks;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;

/**
 * Class for saved instruction sets panel controller.
 *
 * @author Murat Artim
 * @date 12 Sep 2018
 * @time 15:07:17
 */
public class SavedInstructionSetsPanel implements Initializable {

	/** The main screen. */
	private MainScreen owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_;

	@FXML
	private Label noSavedLabel_;

	@FXML
	private Button delete_, run_, save_, check_, share_;

	@FXML
	private ListView<SavedInstructionSetItem> instructionSetsList_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set multiple selection
		instructionSetsList_.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		// bind components
		delete_.disableProperty().bind(instructionSetsList_.getSelectionModel().selectedItemProperty().isNull());
		run_.disableProperty().bind(instructionSetsList_.getSelectionModel().selectedItemProperty().isNull());
		check_.disableProperty().bind(instructionSetsList_.getSelectionModel().selectedItemProperty().isNull());
		save_.disableProperty().bind(instructionSetsList_.getSelectionModel().selectedItemProperty().isNull());
		share_.disableProperty().bind(instructionSetsList_.getSelectionModel().selectedItemProperty().isNull());

		// bind no tasks label
		instructionSetsList_.getItems().addListener((ListChangeListener<SavedInstructionSetItem>) c -> noSavedLabel_.setVisible(instructionSetsList_.getItems().isEmpty()));

		// windows OS
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			run_.setPrefWidth(50);
		}
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public VBox getRoot() {
		return root_;
	}

	/**
	 * Returns the main screen of the application.
	 *
	 * @return The main screen of the application.
	 */
	public MainScreen getOwner() {
		return owner_;
	}

	/**
	 * Returns true if task manager is shown.
	 *
	 * @return True if task manager is shown.
	 */
	public boolean isShown() {
		return isShown_;
	}

	/**
	 * Shows this panel.
	 *
	 * @param node
	 *            Node to show the popup window.
	 */
	public void show(Node node) {

		// already shown
		if (isShown_)
			return;

		// create pop-over
		popOver_ = new PopOver();
		popOver_.setArrowLocation(ArrowLocation.TOP_RIGHT);
		popOver_.setDetachable(false);
		popOver_.setHideOnEscape(true);
		popOver_.setAutoHide(true);
		popOver_.setContentNode(root_);
		popOver_.setStyle("-fx-base: #ececec;");

		// set showing handler
		popOver_.setOnShowing(event -> isShown_ = true);

		// set hidden handler
		popOver_.setOnHidden(event -> isShown_ = false);

		// show
		popOver_.show(node);
	}

	/**
	 * Returns saved tasks list.
	 *
	 * @return Saved tasks list.
	 */
	public ListView<SavedInstructionSetItem> getSavedInstructionSets() {
		return instructionSetsList_;
	}

	@FXML
	private void onDeleteClicked() {

		// no tasks selected
		if (instructionSetsList_.getSelectionModel().isEmpty())
			return;

		// delete selected tasks
		owner_.getActiveTasksPanel().runTaskInParallel(new DeleteInstructionSets(instructionSetsList_.getSelectionModel().getSelectedItems(), false));
	}

	@FXML
	private void onRunClicked() {

		// no tasks selected
		if (instructionSetsList_.getSelectionModel().isEmpty())
			return;

		// get selected tasks
		SavedInstructionSetItem selected = instructionSetsList_.getSelectionModel().getSelectedItem();

		// run in parallel

		RunSavedTasks run = new RunSavedTasks(selected, false, false);
		DeleteSavedTasks delete = new DeleteSavedTasks(selected, false);
		owner_.getActiveTasksPanel().runTasksSequentially(run, delete);
	}
}