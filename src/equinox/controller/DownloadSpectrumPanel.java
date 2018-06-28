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
import java.nio.file.Path;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.DownloadViewPanel.DownloadItemPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.DownloadInfo;
import equinox.dataServer.remote.data.MultiplicationTableInfo.MultiplicationTableInfoType;
import equinox.dataServer.remote.data.MultiplicationTableSearchInput;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.data.PilotPointSearchInput;
import equinox.dataServer.remote.data.SearchItem;
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.AdvancedMultiplicationTableSearch;
import equinox.task.AdvancedPilotPointSearch;
import equinox.task.DeleteSpectrumFromGlobalDB;
import equinox.task.DownloadSpectrum;
import equinox.task.UpdateSpectrumInfoInGlobalDB;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;

/**
 * Class for download spectrum panel.
 *
 * @author Murat Artim
 * @date May 5, 2014
 * @time 4:56:12 PM
 */
public class DownloadSpectrumPanel implements Initializable, DownloadItemPanel {

	/** The owner panel. */
	private DownloadViewPanel owner_;

	/** CDF set info. */
	private SpectrumInfo info_;

	@FXML
	private HBox root_;

	@FXML
	private Button infoButton_, delete_;

	@FXML
	private ToggleSwitch select_;

	@FXML
	private Label title_, infoLabel_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set listener
		select_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> owner_.setupControls());
	}

	@Override
	public void start() {

		// remove delete button if not administrator or logged in
		if (!Equinox.USER.hasPermission(Permission.DELETE_SPECTRUM, false, null)) {
			root_.getChildren().remove(delete_);
		}
	}

	@Override
	public String getHeader() {
		return "Fatigue Spectra";
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public DownloadViewPanel getOwner() {
		return owner_;
	}

	@Override
	public HBox getRoot() {
		return root_;
	}

	@Override
	public ToggleSwitch getSelectSwitch() {
		return select_;
	}

	@Override
	public DownloadInfo getInfo() {
		return info_;
	}

	@Override
	public void download(Path directory, int index) {

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

		// create path to temporary file
		String name = (String) info_.getInfo(SpectrumInfoType.NAME);
		Path output = directory.resolve(name + "_" + index + FileType.ZIP.getExtension());

		// add task
		tm.runTaskInParallel(new DownloadSpectrum(info_, output, false));
	}

	@Override
	public void add() {
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new DownloadSpectrum(info_, null, true));
	}

	@Override
	public void setInfo(DownloadInfo info) {

		// set info
		info_ = (SpectrumInfo) info;

		// set title
		title_.setText((String) info_.getInfo(SpectrumInfoType.NAME));

		// set info label
		String label = (String) info_.getInfo(SpectrumInfoType.AC_PROGRAM);
		label += ", " + (String) info_.getInfo(SpectrumInfoType.AC_SECTION);
		label += ", " + (String) info_.getInfo(SpectrumInfoType.FAT_MISSION);
		label += ", " + (String) info_.getInfo(SpectrumInfoType.DELIVERY_REF);
		infoLabel_.setText(label);
	}

	@Override
	public boolean canBeDownloaded() {
		return true;
	}

	@Override
	public boolean canBeAdded() {
		return true;
	}

	/**
	 * Updates spectrum info.
	 *
	 * @param info
	 *            Spectrum info.
	 */
	public void updateInfo(SpectrumInfo info) {

		// set spectrum ID and name
		info.setInfo(SpectrumInfoType.ID, info_.getInfo(SpectrumInfoType.ID));
		info.setInfo(SpectrumInfoType.NAME, info_.getInfo(SpectrumInfoType.NAME));
		info.setInfo(SpectrumInfoType.DATA_SIZE, info_.getInfo(SpectrumInfoType.DATA_SIZE));
		info.setInfo(SpectrumInfoType.DATA_URL, info_.getInfo(SpectrumInfoType.DATA_URL));
		info.setInfo(SpectrumInfoType.PILOT_POINTS, info_.getInfo(SpectrumInfoType.PILOT_POINTS));
		info.setInfo(SpectrumInfoType.MULT_TABLES, info_.getInfo(SpectrumInfoType.MULT_TABLES));

		// update info
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new UpdateSpectrumInfoInGlobalDB(info, owner_));
	}

	@FXML
	private void onInfoClicked() {

		// check if the current user can edit spectrum info
		boolean canEdit = Equinox.USER.hasPermission(Permission.UPDATE_SPECTRUM_INFO, false, null);

		// show info
		PopOver popOver = new PopOver();
		double buttonY = infoButton_.localToScreen(infoButton_.getBoundsInLocal()).getMinY();
		double height = Screen.getPrimary().getBounds().getHeight();
		popOver.setArrowLocation(buttonY <= height * 0.5 ? ArrowLocation.TOP_RIGHT : ArrowLocation.BOTTOM_RIGHT);
		popOver.setDetachable(true);
		popOver.setContentNode(canEdit ? DownloadSpectrumInfoEditPanel.load(info_, this, popOver) : DownloadSpectrumInfoPanel.load(info_, popOver));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.setAutoFix(false);
		popOver.setTitle((String) info_.getInfo(SpectrumInfoType.NAME));
		popOver.show(infoButton_);
	}

	@FXML
	private void onDownloadClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

		// show save dialog
		String name = (String) info_.getInfo(SpectrumInfoType.NAME);
		fileChooser.setInitialFileName(name + FileType.ZIP.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.ZIP);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new DownloadSpectrum(info_, output.toPath(), false));
	}

	@FXML
	private void onAddClicked() {
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new DownloadSpectrum(info_, null, true));
	}

	@FXML
	private void onSearchPilotPointsClicked() {

		// create search input
		PilotPointSearchInput input = new PilotPointSearchInput();

		// get search info
		String spectrumName = (String) info_.getInfo(SpectrumInfoType.NAME);
		String program = (String) info_.getInfo(SpectrumInfoType.AC_PROGRAM);
		String section = (String) info_.getInfo(SpectrumInfoType.AC_SECTION);

		// add inputs
		input.addInput(PilotPointInfoType.SPECTRUM_NAME, new SearchItem(spectrumName, 1));
		input.addInput(PilotPointInfoType.AC_PROGRAM, new SearchItem(program, 1));
		input.addInput(PilotPointInfoType.AC_SECTION, new SearchItem(section, 1));

		// set engine settings
		SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) owner_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
		panel.setEngineSettings(input);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new AdvancedPilotPointSearch(input));
	}

	@FXML
	private void onSearchMultiplicationTablesClicked() {

		// create search input
		MultiplicationTableSearchInput input = new MultiplicationTableSearchInput();

		// get search info
		String spectrumName = (String) info_.getInfo(SpectrumInfoType.NAME);
		String program = (String) info_.getInfo(SpectrumInfoType.AC_PROGRAM);
		String section = (String) info_.getInfo(SpectrumInfoType.AC_SECTION);

		// add inputs
		input.addInput(MultiplicationTableInfoType.SPECTRUM_NAME, new SearchItem(spectrumName, 1));
		input.addInput(MultiplicationTableInfoType.AC_PROGRAM, new SearchItem(program, 1));
		input.addInput(MultiplicationTableInfoType.AC_SECTION, new SearchItem(section, 1));

		// set engine settings
		SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) owner_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
		panel.setEngineSettings(input);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new AdvancedMultiplicationTableSearch(input));
	}

	@FXML
	private void onDeleteClicked() {

		// create confirmation action
		PopOver popOver = new PopOver();
		EventHandler<ActionEvent> handler = event -> {

			// get task panel
			ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

			// delete
			tm.runTaskInParallel(new DeleteSpectrumFromGlobalDB(info_, owner_));

			// hide pop-over
			popOver.hide();
		};

		// show message
		String message = "Are you sure you want to delete the spectrum from ESCSAS global database?";
		popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel2.load(popOver, message, 50, "Delete", handler, NotificationPanel2.QUESTION));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);

		// show popup
		popOver.show(delete_);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static DownloadSpectrumPanel load(DownloadViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadSpectrumPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DownloadSpectrumPanel controller = (DownloadSpectrumPanel) fxmlLoader.getController();

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
