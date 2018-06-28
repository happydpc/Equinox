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
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.BasicSearchInput;
import equinox.dataServer.remote.data.MultiplicationTableInfo.MultiplicationTableInfoType;
import equinox.dataServer.remote.data.MultiplicationTableSearchInput;
import equinox.dataServer.remote.data.SearchItem;
import equinox.task.AdvancedMultiplicationTableSearch;
import equinox.task.BasicMultiplicationTableSearch;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/**
 * Class for search multiplication tables panel controller.
 *
 * @author Murat Artim
 * @date Feb 29, 2016
 * @time 11:19:28 AM
 */
public class SearchMultiplicationTablesPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField name_, cdfName_, ppName_, acProgram_, acSection_, fatMission_, keywords_, issue_, deliveryRef_, description_;

	@FXML
	private ToggleGroup nameCriteria_, cdfCriteria_, ppCriteria_, acProgramCriteria_, acSectionCriteria_, missionCriteria_, issueCriteria_, descriptionCriteria_, deliveryRefCriteria_;

	@FXML
	private Button clear_, search_;

	@FXML
	private TitledPane basicSearchPane_, advancedSearchPane_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup search field
		keywords_.textProperty().addListener((ChangeListener<String>) (ov, old_Val, new_val) -> clear_.setVisible(!new_val.isEmpty()));

		// set hand cursor to clear button
		Utility.setHandCursor(clear_);

		// bind components
		search_.disableProperty().bind(basicSearchPane_.expandedProperty().not().and(advancedSearchPane_.expandedProperty().not()));

		// expand first pane
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
		// no implementation
	}

	@Override
	public String getHeader() {
		return "Search Loadcase Factors Files";
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset search fields
		TextField[] fields = { name_, cdfName_, ppName_, acProgram_, acSection_, fatMission_, issue_, deliveryRef_, description_ };
		for (TextField field : fields) {
			field.clear();
		}

		// reset keywords
		keywords_.clear();

		// reset criterion
		ToggleGroup[] toggles = { nameCriteria_, cdfCriteria_, ppCriteria_, acProgramCriteria_, acSectionCriteria_, missionCriteria_, issueCriteria_, descriptionCriteria_, deliveryRefCriteria_ };
		for (ToggleGroup tg : toggles) {
			tg.getToggles().get(0).setSelected(true);
		}
	}

	@FXML
	private void onClearSearchClicked() {
		keywords_.clear();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to search and download spectra from AFM database", null);
	}

	@FXML
	private void onSearchClicked() {

		try {

			// get search type
			boolean advanced = advancedSearchPane_.isExpanded();

			// advanced search
			if (advanced) {
				advancedSearch();
			}
			else {
				basicSeach();
			}
		}

		// exception occurred
		catch (Exception e) {

			// log exception
			Equinox.LOGGER.log(Level.WARNING, "Exception occured during setting database search inputs.", e);

			// show warning
			String message = "Exception occured during setting database search inputs: " + e.getLocalizedMessage();
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(search_);
		}
	}

	/**
	 * Performs basic search.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void basicSeach() throws Exception {

		// get keywords
		String keywords = keywords_.getText().trim();

		// no keyword entered
		if (keywords == null || keywords.isEmpty()) {
			showWarningMessage("No search keyword entered. Please enter keywords to proceed.", keywords_);
			return;
		}

		// create input list
		ArrayList<String> inputs = new ArrayList<>();

		// multiple keywords
		if (keywords.contains(",")) {

			// split
			String[] split = keywords.split(",");

			// no keywords
			if (split.length == 0) {
				showWarningMessage("No search keyword entered. Please enter keywords to proceed.", keywords_);
				return;
			}

			// add words to inputs
			for (String word : split) {
				word = word.trim();
				if (!word.isEmpty()) {
					inputs.add(word);
				}
			}

			// no keywords
			if (inputs.isEmpty()) {
				showWarningMessage("No search keyword entered. Please enter keywords to proceed.", keywords_);
				return;
			}
		}
		else {
			inputs.add(keywords);
		}

		// create input
		BasicSearchInput input = new BasicSearchInput();
		input.setKeywords(inputs);

		// set engine settings
		SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) owner_.getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
		panel.setEngineSettings(input);

		// search
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new BasicMultiplicationTableSearch(input));
	}

	/**
	 * Performs advanced search.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void advancedSearch() throws Exception {

		// create search input
		MultiplicationTableSearchInput input = new MultiplicationTableSearchInput();

		// add search items
		String name = name_.getText();
		if (name != null && !name.isEmpty()) {
			input.addInput(MultiplicationTableInfoType.NAME, new SearchItem(name, getSelectedCriteria(nameCriteria_)));
		}
		String spectrumName = cdfName_.getText();
		if (spectrumName != null && !spectrumName.isEmpty()) {
			input.addInput(MultiplicationTableInfoType.SPECTRUM_NAME, new SearchItem(spectrumName, getSelectedCriteria(cdfCriteria_)));
		}
		String ppName = ppName_.getText();
		if (ppName != null && !ppName.isEmpty()) {
			input.addInput(MultiplicationTableInfoType.PILOT_POINT_NAME, new SearchItem(ppName, getSelectedCriteria(ppCriteria_)));
		}
		String acProgram = acProgram_.getText();
		if (acProgram != null && !acProgram.isEmpty()) {
			input.addInput(MultiplicationTableInfoType.AC_PROGRAM, new SearchItem(acProgram, getSelectedCriteria(acProgramCriteria_)));
		}
		String acSection = acSection_.getText();
		if (acSection != null && !acSection.isEmpty()) {
			input.addInput(MultiplicationTableInfoType.AC_SECTION, new SearchItem(acSection, getSelectedCriteria(acSectionCriteria_)));
		}
		String fatMission = fatMission_.getText();
		if (fatMission != null && !fatMission.isEmpty()) {
			input.addInput(MultiplicationTableInfoType.FAT_MISSION, new SearchItem(fatMission, getSelectedCriteria(missionCriteria_)));
		}
		String issue = issue_.getText();
		if (issue != null && !issue.isEmpty()) {
			input.addInput(MultiplicationTableInfoType.ISSUE, new SearchItem(issue, getSelectedCriteria(issueCriteria_)));
		}
		String deliveryRef = deliveryRef_.getText();
		if (deliveryRef != null && !deliveryRef.isEmpty()) {
			input.addInput(MultiplicationTableInfoType.DELIVERY_REF, new SearchItem(deliveryRef, getSelectedCriteria(deliveryRefCriteria_)));
		}
		String description = description_.getText();
		if (description != null && !description.isEmpty()) {
			input.addInput(MultiplicationTableInfoType.DESCRIPTION, new SearchItem(description, getSelectedCriteria(descriptionCriteria_)));
		}

		// no search items entered
		if (input.isEmpty()) {
			showWarningMessage("No search criteria entered. Please enter at least 1 search item to proceed.", search_);
			return;
		}

		// set engine settings
		SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) owner_.getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
		panel.setEngineSettings(input);

		// search
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new AdvancedMultiplicationTableSearch(input));
	}

	/**
	 * Shows missing input warning message.
	 *
	 * @param message
	 *            Message text to show.
	 * @param node
	 *            Node to show the warning message on.
	 */
	private static void showWarningMessage(String message, Node node) {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
	}

	/**
	 * Returns the selected criteria for the given toggle group.
	 *
	 * @param tg
	 *            Toggle group.
	 * @return The selected criteria.
	 */
	private static int getSelectedCriteria(ToggleGroup tg) {
		int index = 0;
		for (Toggle t : tg.getToggles()) {
			if (t.isSelected()) {
				break;
			}
			index++;
		}
		return index;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static SearchMultiplicationTablesPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SearchMultiplicationTablesPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			SearchMultiplicationTablesPanel controller = (SearchMultiplicationTablesPanel) fxmlLoader.getController();

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
