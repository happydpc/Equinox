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
package equinox.task;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.controller.SettingsPanel;
import equinox.data.EncryptedSettings;
import equinox.data.Settings;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for read settings task.
 *
 * @author Murat Artim
 * @date Apr 30, 2014
 * @time 2:23:53 PM
 */
public class LoadSettings extends InternalEquinoxTask<Settings> implements ShortRunningTask {

	/** Settings panel. */
	private final SettingsPanel panel_;

	/**
	 * Creates read settings task.
	 *
	 * @param panel
	 *            Settings panel.
	 */
	public LoadSettings(SettingsPanel panel) {
		panel_ = panel;
	}

	@Override
	public String getTaskTitle() {
		return "Load settings";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Settings call() throws Exception {

		// file doesn't exist or cannot read
		if (!Files.exists(Equinox.SETTINGS_FILE))
			return loadDefaultSettings();

		// read and return settings object
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(Equinox.SETTINGS_FILE.toFile())))) {
			return ((EncryptedSettings) in.readObject()).createSettings();
		}

		// cannot read due to version
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Settings object version has changed. Creating new settings", e);
			return loadDefaultSettings();
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set settings to panel
		try {
			panel_.setSettings(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Loads default App Launcher settings from file, or creates new settings if settings file doesn't exist.
	 *
	 * @return App Launcher settings.
	 */
	private static Settings loadDefaultSettings() {

		// no default settings file
		if (!Files.exists(Equinox.DEFAULT_SETTINGS_FILE))
			return new Settings();

		// read settings file
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(Equinox.DEFAULT_SETTINGS_FILE.toFile())))) {
			return ((EncryptedSettings) in.readObject()).createSettings();
		}

		// exception occurred during reading
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during loading default settings.", e);
			return new Settings();
		}
	}
}
