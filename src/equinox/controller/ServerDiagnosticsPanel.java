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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.font.IconicFont;
import equinox.task.ShowServerDiagnostics;
import equinox.utility.Utility;
import equinoxServer.remote.data.ServerStatistic;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * Class for server statistics input panel controller.
 *
 * @author Murat Artim
 * @date 20 Jul 2017
 * @time 15:56:38
 *
 */
public class ServerDiagnosticsPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<ServerStatistic> statistic_;

	@FXML
	private DatePicker from_, to_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set statistics
		statistic_.getItems().setAll(ServerStatistic.values());
		statistic_.getItems().remove(0);

		// setup date fields
		setupDate(from_, LocalDate.now().minusWeeks(1l));
		setupDate(to_, LocalDate.now());
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
		return "Server Diagnostics";
	}

	@FXML
	private void onOkClicked() {

		// get selected statistic
		ServerStatistic stat = statistic_.getSelectionModel().getSelectedItem();

		// no selection
		if (stat == null) {
			String message = "Please select target statistic to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(statistic_);
			return;
		}

		// get from date
		LocalDate from = from_.getValue();
		if (from == null || from.isAfter(LocalDate.now())) {
			String message = "Invalid date specified. Please select a valid date to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(from_);
			return;
		}

		// get to date
		LocalDate to = to_.getValue();
		if (to == null || to.isAfter(LocalDate.now()) || to.isBefore(from)) {
			String message = "Invalid date specified. Please select a valid date to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(to_);
			return;
		}

		// create calendar dates
		Timestamp fromTS = new Timestamp(Date.from(from.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()).getTime());
		Timestamp toTS = new Timestamp(Date.from(to.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()).getTime());

		// start task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new ShowServerDiagnostics(stat, fromTS, toTS));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		statistic_.getSelectionModel().clearSelection();
		from_.setValue(LocalDate.now().minusWeeks(1l));
		to_.setValue(LocalDate.now());
	}

	/**
	 * Sets up date picker component.
	 *
	 * @param datePicker
	 *            Date picker to setup.
	 * @param date
	 *            Initial date.
	 */
	private static void setupDate(DatePicker datePicker, LocalDate date) {

		// disable future days
		final Callback<DatePicker, DateCell> dayCellFactory = datePicker1 -> new DateCell() {

			@Override
			public void updateItem(LocalDate item, boolean empty) {
				super.updateItem(item, empty);
				if (item.isAfter(LocalDate.now())) {
					setDisable(true);
					setStyle("-fx-background-color: #ffc0cb;");
				}
			}
		};
		datePicker.setDayCellFactory(dayCellFactory);

		// create and set converter to generation date picker
		datePicker.setConverter(new StringConverter<LocalDate>() {

			/** Date formatter. */
			private final DateTimeFormatter formatter_ = DateTimeFormatter.ofPattern("dd/MM/yyyy");

			@Override
			public String toString(LocalDate date) {
				return date != null ? formatter_.format(date) : "";
			}

			@Override
			public LocalDate fromString(String string) {
				return string != null && !string.isEmpty() ? LocalDate.parse(string, formatter_) : null;
			}
		});

		// set current date
		datePicker.setValue(date);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static ServerDiagnosticsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ServerDiagnosticsPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ServerDiagnosticsPanel controller = (ServerDiagnosticsPanel) fxmlLoader.getController();

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
