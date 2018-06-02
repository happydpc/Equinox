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

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.input.ExternalStressSequenceComparisonInput;
import equinox.data.input.ExternalStressSequenceComparisonInput.ExternalComparisonCriteria;
import equinox.task.CompareExternalStressSequences;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for compare external stress sequence panel controller.
 *
 * @author Murat Artim
 * @date Mar 14, 2015
 * @time 8:34:53 PM
 */
public class CompareExternalStressSequencePanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ChoiceBox<ExternalComparisonCriteria> criteria_;

	@FXML
	private ChoiceBox<String> order_;

	@FXML
	private ToggleSwitch dataLabels_, includeSequenceName_, includeEID_, includeProgram_, includeSection_, includeMission_;

	@FXML
	private TitledPane namingPane_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set results order
		order_.setItems(FXCollections.observableArrayList("Descending", "Ascending"));
		order_.getSelectionModel().select(0);

		// set comparison criteria
		criteria_.getItems().setAll(ExternalComparisonCriteria.values());
		criteria_.getSelectionModel().select(0);

		// set listeners
		dataLabels_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				StatisticsViewPanel panel = (StatisticsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);
				panel.setLabelsVisible(newValue);
			}
		});

		// expand first panel
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		onOkClicked();
	}

	@Override
	public String getHeader() {
		return "Compare Stress Sequences";
	}

	@FXML
	private void onResetClicked() {
		criteria_.getSelectionModel().select(0);
		order_.getSelectionModel().select(0);
		if (!dataLabels_.isSelected()) {
			dataLabels_.setSelected(true);
		}
		if (!includeSequenceName_.isSelected()) {
			includeSequenceName_.setSelected(true);
		}
		includeEID_.setSelected(false);
		includeProgram_.setSelected(false);
		includeSection_.setSelected(false);
		includeMission_.setSelected(false);
	}

	@FXML
	private void onOkClicked() {

		// get naming parameters
		boolean includeSequenceName = includeSequenceName_.isSelected();
		boolean includeEID = includeEID_.isSelected();
		boolean includeProgram = includeProgram_.isSelected();
		boolean includeSection = includeSection_.isSelected();
		boolean includeMission = includeMission_.isSelected();

		// no naming selected
		if (!includeSequenceName && !includeEID && !includeProgram && !includeSection && !includeMission) {
			String message = "Please select at least 1 naming for plot series.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(namingPane_);
			return;
		}

		// create input
		ExternalStressSequenceComparisonInput input = new ExternalStressSequenceComparisonInput();

		// set inputs
		input.setCriteria(criteria_.getSelectionModel().getSelectedItem());
		input.setLabelDisplay(dataLabels_.isSelected());
		input.setOrder(order_.getSelectionModel().getSelectedItem().equals("Descending"));

		// set naming parameters
		input.setIncludeSequenceName(includeSequenceName);
		input.setIncludeEID(includeEID);
		input.setIncludeProgram(includeProgram);
		input.setIncludeSection(includeSection);
		input.setIncludeMission(includeMission);

		// get selected files
		for (TreeItem<String> item : owner_.getSelectedFiles()) {
			input.addStressSequence((ExternalStressSequence) item);
		}

		// create and start comparison task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CompareExternalStressSequences(input));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to compare stress sequences", null);
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static CompareExternalStressSequencePanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareExternalStressSequencePanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareExternalStressSequencePanel controller = (CompareExternalStressSequencePanel) fxmlLoader.getController();

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
