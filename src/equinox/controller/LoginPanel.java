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

import encoder.Base64Encoder;
import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.utility.Utility;
import equinoxServer.remote.data.Permission;
import equinoxServer.remote.message.Login;
import equinoxServer.remote.message.LoginSuccessful;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for login panel.
 *
 * @author Murat Artim
 * @date May 14, 2014
 * @time 4:09:16 PM
 */
public class LoginPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField username_;

	@FXML
	private PasswordField password_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
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

		// set username
		username_.setText(Equinox.USER.getUsername());

		// clear password
		password_.clear();
	}

	@Override
	public String getHeader() {
		return "Login";
	}

	/**
	 * Called if login is successful.
	 *
	 * @param message
	 *            Login successful message.
	 */
	public void loginSuccessful(LoginSuccessful message) {

		// set user as logged in
		Equinox.USER.loginAsAdministrator();

		// add administrator permissions
		message.getAdminPermissionNames().forEach(p -> Equinox.USER.addPermission(p));

		// setup UI
		owner_.getOwner().getMenuBarPanel().removeLoginItem();
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Called if login is failed.
	 */
	public void loginFailed() {
		String message = "Invalid administrator password given. Please supply a valid password.";
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(password_);
	}

	@FXML
	private void onOkClicked() {

		// has no permission
		if (!Equinox.USER.hasPermission(Permission.LOGIN_AS_ADMINISTRATOR, true, owner_.getOwner()))
			return;

		// get inputs
		String username = username_.getText();
		String password = password_.getText();

		// check invalid values
		if (!checkInvalidValues(username, password))
			return;

		// send login request
		owner_.getOwner().getNetworkWatcher().sendMessage(new Login(Base64Encoder.encodeString(password)));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		password_.clear();
	}

	/**
	 * Checks for invalid input values.
	 *
	 * @param username
	 *            Username.
	 * @param password
	 *            Password.
	 * @return True if the values are valid.
	 */
	private boolean checkInvalidValues(String username, String password) {

		// initialize message
		String message = null;
		Node node = null;

		// check username
		if (username == null || username.trim().isEmpty()) {
			message = "";
			node = username_;
		}

		// check password
		else if (password == null || password.trim().isEmpty()) {
			message = "";
			node = password_;
		}

		// invalid value found
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(node);
			return false;
		}

		// valid values
		return true;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static LoginPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("LoginPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			LoginPanel controller = (LoginPanel) fxmlLoader.getController();

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
