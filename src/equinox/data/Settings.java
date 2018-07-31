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
package equinox.data;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for settings.
 *
 * @author Murat Artim
 * @date Apr 30, 2014
 * @time 1:27:59 PM
 */
public class Settings {

	/** Setting index. */
	public static final int NOTIFY_EQUINOX_UPDATES = 0, NOTIFY_PLUGIN_UPDATES = 1, NOTIFY_MATERIAL_UPDATES = 2, LIKES = 3, ANALYSIS_SERVER_HOSTNAME = 4, ANALYSIS_SERVER_PORT = 5, WEB_HOSTNAME = 6, WEB_PORT = 7, NOTIFY_MESSAGES = 8, NOTIFY_FILES = 9, NOTIFY_ERRORS = 10, NOTIFY_WARNINGS = 11,
			NOTIFY_INFO = 12, NOTIFY_QUEUED = 13, NOTIFY_SUBMITTED = 14, NOTIFY_SUCCEEDED = 15, USE_SYSTEMTRAY = 16, ANALYSIS_ENGINE = 17, ISAMI_SUB_VERSION = 18, FALLBACK_TO_INBUILT = 19, APPLY_COMPRESSION = 20, NOTIFY_SAVED = 21, NOTIFY_SCHEDULED = 22, SHOW_NOTIFY_FROM_BOTTOM = 23,
			KEEP_ANALYSIS_OUTPUTS = 24, DETAILED_ANALYSIS = 25, SHOW_HEALTH_MONITORING = 26, ISAMI_VERSION = 27, FILER_USERNAME = 28, FILER_HOSTNAME = 29, FILER_PORT = 30, FILER_PASSWORD = 31, FILER_ROOT_PATH = 32, WEB_PATH = 33, EXCHANGE_SERVER_HOSTNAME = 34, EXCHANGE_SERVER_PORT = 35,
			DATA_SERVER_HOSTNAME = 36, DATA_SERVER_PORT = 37;

	/** Settings. */
	private final HashMap<Integer, Setting> settings_;

	/**
	 * Creates settings.
	 */
	public Settings() {
		settings_ = new HashMap<>();
	}

	/**
	 * Returns the value of the setting at the given index.
	 *
	 * @param index
	 *            Index of the demanded setting.
	 * @return The value of the setting.
	 */
	public Object getValue(int index) {
		return settings_.get(index).getValue();
	}

	/**
	 * Returns true if the given wish ID is already liked.
	 *
	 * @param wishID
	 *            Wish ID to check.
	 * @return True if the given wish ID is already liked.
	 */
	@SuppressWarnings("unchecked")
	public boolean isWishLiked(long wishID) {
		Setting setting = settings_.get(LIKES);
		ArrayList<Long> likes = (ArrayList<Long>) setting.getValue();
		for (long id : likes)
			if (id == wishID)
				return true;
		return false;
	}

	/**
	 * Sets the value of the setting at the given index.
	 *
	 * @param index
	 *            Index of the setting to set.
	 * @param value
	 *            The value to set.
	 * @return True if application restart is required.
	 */
	public boolean setValue(int index, Object value) {
		return settings_.get(index).setValue(value);
	}

	/**
	 * Adds given wish ID to liked wishes.
	 *
	 * @param wishID
	 *            Wish ID to add.
	 */
	@SuppressWarnings("unchecked")
	public void addToLikedWishes(long wishID) {
		Setting setting = settings_.get(LIKES);
		ArrayList<Long> likes = (ArrayList<Long>) setting.getValue();
		likes.add(wishID);
	}

	/**
	 * Returns all settings mapping. Note that this method is meant to be called for encryption.
	 *
	 * @return All settings.
	 */
	public HashMap<Integer, Setting> getSettings() {
		return settings_;
	}

	/**
	 * Puts setting into settings mapping. Note that this method is meant to be called for descryption.
	 *
	 * @param index
	 *            Index of setting.
	 * @param setting
	 *            Setting.
	 */
	public void putSetting(int index, Setting setting) {
		settings_.put(index, setting);
	}
}
