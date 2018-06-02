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
import java.util.ResourceBundle;

import equinox.Equinox;
import equinox.data.EquinoxPluginManager;
import equinox.data.EquinoxTheme;
import equinox.data.Settings;
import equinox.data.ui.NotificationPanel;
import equinox.network.NetworkWatcher;
import equinox.plugin.FileType;
import equinox.task.DownloadSampleInput;
import equinox.utility.Animator;
import equinox.utility.Utility;
import equinoxServer.remote.data.Permission;
import equinoxServer.remote.listener.StandardMessageListener;
import equinoxServer.remote.message.Announcement;
import equinoxServer.remote.message.ChatMessage;
import equinoxServer.remote.message.Handshake;
import equinoxServer.remote.message.LoginFailed;
import equinoxServer.remote.message.LoginSuccessful;
import equinoxServer.remote.message.NetworkMessage;
import equinoxServer.remote.message.PermissionDenied;
import equinoxServer.remote.message.RoomChange;
import equinoxServer.remote.message.ShareFile;
import equinoxServer.remote.message.StatusChange;
import equinoxServer.remote.message.WhoRequest;
import equinoxServer.remote.message.WhoResponse;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Controller class for the main screen.
 *
 * @author Murat Artim
 * @date Dec 6, 2013
 * @time 10:40:41 AM
 */
public class MainScreen implements Initializable, StandardMessageListener {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** The owner application. */
	private Equinox owner_;

	/** Files panel. */
	private InputPanel inputPanel_;

	/** View panel. */
	private ViewPanel viewPanel_;

	/** Menu bar panel. */
	private MenuBarPanel menuBarPanel_;

	/** Notification pane. */
	private NotificationPanel notificationPane_;

	/** Plugin manager. */
	private EquinoxPluginManager pluginManager_;

	@FXML
	private StackPane stack_;

	@FXML
	private SplitPane frontLayer_;

	@FXML
	private VBox root_, backLayer_, menuBarLayer_;

	@FXML
	private ImageView version_;

	/** Active tasks panel. */
	private ActiveTasksPanel activeTasksPanel_;

	/** Queued tasks panel. */
	private QueuedTasksPanel queuedTasksPanel_;

	/** Saved tasks panel. */
	private SavedTasksPanel savedTasksPanel_;

	/** Scheduled tasks panel. */
	private ScheduledTasksPanel scheduledTasksPanel_;

	/** Task history panel. */
	private TaskHistoryPanel taskHistoryPanel_;

	/** File chooser. */
	private FileChooser fileChooser_;

	/** Directory chooser. */
	private DirectoryChooser directoryChooser_;

	/** Initial directory for file chooser. */
	private File initialDirectory_ = null;

	/** The network watcher of the application. */
	private NetworkWatcher networkWatcher_;

	/** List containing the available users. */
	private ObservableList<String> availableUsers_;

	/** User's status. */
	private SimpleBooleanProperty isAvailable_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// create choosers
		fileChooser_ = new FileChooser();
		directoryChooser_ = new DirectoryChooser();

		// load menu bar panel
		menuBarPanel_ = MenuBarPanel.load(this);
		menuBarLayer_.getChildren().add(menuBarPanel_.getRoot());

		// load and add control panels
		inputPanel_ = InputPanel.load(this);
		frontLayer_.getItems().add(inputPanel_.getRoot());

		// load and add view panel
		viewPanel_ = ViewPanel.load(this);
		frontLayer_.getItems().add(viewPanel_.getRoot());
		frontLayer_.setDividerPosition(0, 0.3);

		// load task manager panel
		activeTasksPanel_ = ActiveTasksPanel.load(this);
		queuedTasksPanel_ = QueuedTasksPanel.load(this);
		savedTasksPanel_ = SavedTasksPanel.load(this);
		scheduledTasksPanel_ = ScheduledTasksPanel.load(this);
		taskHistoryPanel_ = TaskHistoryPanel.load(this);

		// create network watcher
		networkWatcher_ = new NetworkWatcher(this);
		networkWatcher_.setStandardMessageListener(this);

		// create available users list
		availableUsers_ = FXCollections.observableArrayList();
		availableUsers_.addListener((ChatPopup) inputPanel_.getPopup(InputPanel.CHAT_POPUP));
		availableUsers_.addListener((ShareFilePopup) inputPanel_.getPopup(InputPanel.SHARE_FILE_POPUP));
		availableUsers_.addListener((SaveEquivalentStressInfoPanel) inputPanel_.getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL));
		availableUsers_.addListener((SaveLifeFactorPanel) inputPanel_.getSubPanel(InputPanel.SAVE_LIFE_FACTOR_PANEL));
		availableUsers_.addListener((SaveEquivalentStressRatioPanel) inputPanel_.getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_RATIO_PANEL));
		availableUsers_.addListener((RfortReportPanel) inputPanel_.getSubPanel(InputPanel.RFORT_REPORT_PANEL));
		availableUsers_.addListener((SaveDamageContributionsPanel) inputPanel_.getSubPanel(InputPanel.SAVE_DAMAGE_CONTRIBUTIONS_PANEL));
		availableUsers_.addListener((SaveAircraftLifeFactorPanel) inputPanel_.getSubPanel(InputPanel.SAVE_AC_LIFE_FACTOR_PANEL));
		availableUsers_.addListener((SaveAircraftEquivalentStressRatioPanel) inputPanel_.getSubPanel(InputPanel.SAVE_AC_EQUIVALENT_STRESS_RATIO_PANEL));
		availableUsers_.addListener(viewPanel_.getShareViewPanel());
		availableUsers_.addListener(menuBarPanel_);

		// create user status
		isAvailable_ = new SimpleBooleanProperty(true);

		// create and add notification pane
		notificationPane_ = new NotificationPanel(this);
		root_.getChildren().remove(stack_);
		notificationPane_.setContent(stack_);
		VBox.setVgrow(notificationPane_, Priority.ALWAYS);
		root_.getChildren().add(notificationPane_);

		// create plugin manager
		pluginManager_ = new EquinoxPluginManager(this);
	}

	/**
	 * Starts main screen.
	 *
	 */
	public void start() {

		// start sub panels
		inputPanel_.start();
		viewPanel_.start();
		menuBarPanel_.start();

		// load and show intro panel
		IntroPanel introPanel = IntroPanel.load(this);
		introPanel.showIntro();
	}

	/**
	 * Stops main screen.
	 *
	 */
	public void stop() {

		// disconnect from server and stop network thread
		networkWatcher_.stop();

		// stop files panel
		inputPanel_.stop();
	}

	@Override
	public void respond(NetworkMessage message) throws Exception {

		// run in javafx thread
		Platform.runLater(() -> {

			// handshake
			if (message instanceof Handshake) {
				processHandshake((Handshake) message);
			}

			// who
			else if (message instanceof WhoResponse) {
				processWho((WhoResponse) message);
			}

			// chat message
			else if (message instanceof ChatMessage) {
				processChatMessage((ChatMessage) message);
			}

			// room change
			else if (message instanceof RoomChange) {
				processRoomChange((RoomChange) message);
			}

			// status change
			else if (message instanceof StatusChange) {
				processStatusChange((StatusChange) message);
			}

			// share file
			else if (message instanceof ShareFile) {
				processShareFile((ShareFile) message);
			}

			// announcement
			else if (message instanceof Announcement) {
				notificationPane_.showServerAnnouncement((Announcement) message);
			}

			// permission denied
			else if (message instanceof PermissionDenied) {
				notificationPane_.showPermissionDenied(((PermissionDenied) message).getPermission());
			}

			// login successful
			else if (message instanceof LoginSuccessful) {
				((LoginPanel) inputPanel_.getSubPanel(InputPanel.LOGIN_PANEL)).loginSuccessful((LoginSuccessful) message);
			}

			// login failed
			else if (message instanceof LoginFailed) {
				((LoginPanel) inputPanel_.getSubPanel(InputPanel.LOGIN_PANEL)).loginFailed();
			}
		});
	}

	/**
	 * Returns the owner application.
	 *
	 * @return The owner application.
	 */
	public Equinox getOwner() {
		return owner_;
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root container.
	 */
	public VBox getRoot() {
		return root_;
	}

	/**
	 * Returns the input panel of the main screen.
	 *
	 * @return The input panel of the main screen.
	 */
	public InputPanel getInputPanel() {
		return inputPanel_;
	}

	/**
	 * Returns the view panel of the main screen.
	 *
	 * @return The view panel of the main screen.
	 */
	public ViewPanel getViewPanel() {
		return viewPanel_;
	}

	/**
	 * Returns menu bar panel of the main screen.
	 *
	 * @return Menu bar panel of the main screen.
	 */
	public MenuBarPanel getMenuBarPanel() {
		return menuBarPanel_;
	}

	/**
	 * Returns active tasks panel.
	 *
	 * @return Active tasks panel.
	 */
	public ActiveTasksPanel getActiveTasksPanel() {
		return activeTasksPanel_;
	}

	/**
	 * Returns queued tasks panel.
	 *
	 * @return Queued tasks panel.
	 */
	public QueuedTasksPanel getQueuedTasksPanel() {
		return queuedTasksPanel_;
	}

	/**
	 * Returns saved tasks panel.
	 *
	 * @return Saved tasks panel.
	 */
	public SavedTasksPanel getSavedTasksPanel() {
		return savedTasksPanel_;
	}

	/**
	 * Returns scheduled tasks panel.
	 *
	 * @return Scheduled tasks panel.
	 */
	public ScheduledTasksPanel getScheduledTasksPanel() {
		return scheduledTasksPanel_;
	}

	/**
	 * Returns task history panel.
	 *
	 * @return Task history panel.
	 */
	public TaskHistoryPanel getTaskHistoryPanel() {
		return taskHistoryPanel_;
	}

	/**
	 * Returns notification pane.
	 *
	 * @return Notification pane.
	 */
	public NotificationPanel getNotificationPane() {
		return notificationPane_;
	}

	/**
	 * Returns plugin manager.
	 *
	 * @return Plugin manager.
	 */
	public EquinoxPluginManager getPluginManager() {
		return pluginManager_;
	}

	/**
	 * Returns true if network status is available.
	 *
	 * @return True if network status is available.
	 */
	public boolean isAvailable() {
		return isAvailable_.get();
	}

	/**
	 * Returns true if there is any available user connected to the server.
	 *
	 * @return True if there is any available user connected to the server.
	 */
	public boolean areThereAvailableMembers() {
		return !availableUsers_.isEmpty();
	}

	/**
	 * Loads given page to help view.
	 *
	 * @param page
	 *            Page name. Not that, this doesn't include the file extension.
	 * @param location
	 *            Location within the page. This can be null for start of page.
	 */
	public void showHelp(String page, String location) {
		((WebViewPanel) viewPanel_.getSubPanel(ViewPanel.WEB_VIEW)).showHelp(page, location);
		viewPanel_.showSubPanel(ViewPanel.WEB_VIEW);
	}

	/**
	 * Adds a modal layer to the main screen. This will disable all inputs to the screen.
	 *
	 * @param id
	 *            ID of the modal layer. This is used to remove the layer when requested again.
	 */
	public void addModalLayer(String id) {
		VBox modalLayer = new VBox();
		modalLayer.setId(id);
		modalLayer.setStyle("-fx-background-color: rgba(0,0,0,0.6)");
		stack_.getChildren().add(modalLayer);
		Animator.fade(true, 0, 500, null, modalLayer).play();
	}

	/**
	 * Removes the modal layer from the main screen. This will enable all inputs to the screen.
	 *
	 * @param id
	 *            ID of the modal layer.
	 */
	public void removeModalLayer(String id) {

		// loop over children
		for (Node node : stack_.getChildren())
			// found modal layer
			if (node.getId().equals(id)) {

				// remove (with fade out animation)
				Animator.fade(false, 0, 500, event -> stack_.getChildren().remove(node), node).play();
				break;
			}
	}

	/**
	 * Returns the front layer.
	 *
	 * @return The front layer.
	 */
	public SplitPane getFrontLayer() {
		return frontLayer_;
	}

	/**
	 * Returns the background layer.
	 *
	 * @return The background layer.
	 */
	public VBox getBackLayer() {
		return backLayer_;
	}

	/**
	 * Returns the menu bar layer.
	 *
	 * @return The menu bar layer.
	 */
	public VBox getMenuBarLayer() {
		return menuBarLayer_;
	}

	/**
	 * Sets initial directory for the file chooser.
	 *
	 * @param initialDirectory
	 *            Initial directory to set.
	 */
	public void setInitialDirectory(File initialDirectory) {
		initialDirectory_ = initialDirectory.isDirectory() ? initialDirectory : initialDirectory.getParentFile();
	}

	/**
	 * Downloads sample inputs for the given name key.
	 *
	 * @param name
	 *            Name key for sample input.
	 */
	public void downloadSampleInput(String name) {

		// get file chooser
		FileChooser fileChooser = getFileChooser(FileType.ZIP.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName(name + FileType.ZIP.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.ZIP);

		// save file
		activeTasksPanel_.runTaskInParallel(new DownloadSampleInput(name, output.toPath()));
	}

	/**
	 * Returns the file chooser.
	 *
	 * @param filters
	 *            Extension filters.
	 * @return The file chooser.
	 */
	public FileChooser getFileChooser(ExtensionFilter... filters) {

		// add extension filters
		fileChooser_.getExtensionFilters().clear();
		for (ExtensionFilter filter : filters) {
			fileChooser_.getExtensionFilters().add(filter);
		}

		// set initial directory
		if (initialDirectory_ != null && initialDirectory_.exists() && initialDirectory_.isDirectory()) {
			fileChooser_.setInitialDirectory(initialDirectory_);
		}
		else {
			fileChooser_.setInitialDirectory(null);
		}

		// return file chooser
		return fileChooser_;
	}

	/**
	 * Returns the directory chooser.
	 *
	 * @return The directory chooser.
	 */
	public DirectoryChooser getDirectoryChooser() {

		// set initial directory
		if (initialDirectory_ != null && initialDirectory_.exists() && initialDirectory_.isDirectory()) {
			directoryChooser_.setInitialDirectory(initialDirectory_);
		}
		else {
			directoryChooser_.setInitialDirectory(null);
		}

		// return file chooser
		return directoryChooser_;
	}

	/**
	 * Returns the settings.
	 *
	 * @return The settings.
	 */
	public Settings getSettings() {
		return inputPanel_.getSettings();
	}

	/**
	 * Returns the network watcher of the application.
	 *
	 * @return The network watcher of the application.
	 */
	public NetworkWatcher getNetworkWatcher() {
		return networkWatcher_;
	}

	/**
	 * Closes back layer.
	 *
	 */
	public void closeBackLayer() {
		frontLayer_.setDisable(false);
		menuBarLayer_.setDisable(false);
	}

	/**
	 * Opens back layer.
	 *
	 */
	public void openBackLayer() {
		frontLayer_.setDisable(true);
		menuBarLayer_.setDisable(true);
	}

	/**
	 * Processes handshake message from the server.
	 *
	 * @param message
	 *            Handshake message.
	 */
	private void processHandshake(Handshake message) {

		// successful
		if (message.isHandshakeSuccessful()) {

			// log info
			Equinox.LOGGER.info("Server handshake successfully completed.");

			// set user attributes
			Equinox.USER.setUsername(message.getUsername());
			Equinox.USER.setAsAdministrator(message.isAdministrator());

			// add non-administrative permissions
			message.getPermissionNames().forEach(p -> Equinox.USER.addPermission(p));

			// send who request
			if (Equinox.USER.hasPermission(Permission.SEE_CONNECTED_USERS, false, null)) {
				networkWatcher_.sendMessage(new WhoRequest());
			}

			// setup administrator menu
			menuBarPanel_.setupAdministratorMenu(message.isAdministrator());
		}

		// unsuccessful
		else {
			String text = "Cannot connect to network server. Please check your network connection and try again.";
			notificationPane_.showWarning(text, null);
		}
	}

	/**
	 * Processes who message from the server.
	 *
	 * @param message
	 *            Who message.
	 */
	private void processWho(WhoResponse message) {
		for (int i = 0; i < message.size(); i++) {
			availableUsers_.add(message.getMember(i));
		}
	}

	/**
	 * Processes chat message from the server.
	 *
	 * @param message
	 *            Chat message.
	 */
	private void processChatMessage(ChatMessage message) {

		// add to chat panel
		ChatPopup cp = (ChatPopup) inputPanel_.getPopup(InputPanel.CHAT_POPUP);
		cp.addChatMessage(message);

		// chat panel not shown
		if (!cp.isShown()) {
			notificationPane_.showMessage(cp, message);
		}
	}

	/**
	 * Processes room change message from the server.
	 *
	 * @param message
	 *            Room change message.
	 */
	private void processRoomChange(RoomChange message) {
		String username = message.getUsername();
		if (message.entered()) {
			availableUsers_.add(username);
		}
		else {
			availableUsers_.remove(username);
		}
	}

	/**
	 * Processes status change message from the server.
	 *
	 * @param message
	 *            Status change message.
	 */
	private void processStatusChange(StatusChange message) {

		// get user name
		String username = message.getUsername();

		// this client
		if (username.equals(Equinox.USER.getUsername())) {
			isAvailable_.set(message.isAvailable());
			menuBarPanel_.getNetworkStatusItem().setSelected(message.isAvailable());
		}
		else if (message.isAvailable()) {
			availableUsers_.add(username);
		}
		else {
			availableUsers_.remove(username);
		}
	}

	/**
	 * Processes share view message from the server.
	 *
	 * @param message
	 *            Share file message.
	 */
	private void processShareFile(ShareFile message) {

		// add to chat panel
		ChatPopup cp = (ChatPopup) inputPanel_.getPopup(InputPanel.CHAT_POPUP);
		cp.addShareFileChatMessage(message);

		// chat panel shown
		if (cp.isShown())
			return;

		// create and show notification
		notificationPane_.showIncoming(message);
	}

	/**
	 * Loads and returns main screen.
	 *
	 * @param owner
	 *            The owner application.
	 * @return The newly loaded main screen.
	 */
	public static MainScreen load(Equinox owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("MainScreen.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			MainScreen controller = (MainScreen) fxmlLoader.getController();

			// set root and parent
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
