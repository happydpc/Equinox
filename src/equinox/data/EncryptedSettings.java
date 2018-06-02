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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import decoder.Base64Decoder;
import encoder.Base64Encoder;

/**
 * Class for encrypted settings.
 *
 * @author Murat Artim
 * @date 25 May 2018
 * @time 15:17:58
 */
public class EncryptedSettings implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Encrypted settings. */
	private final HashMap<Integer, Setting> encryptedSettings;

	/**
	 * Creates encrypted settings.
	 *
	 * @param settings
	 *            Settings to encrypt.
	 */
	public EncryptedSettings(Settings settings) {
		encryptedSettings = encrypt(settings);
	}

	/**
	 * Creates and returns settings from this object.
	 *
	 * @return Application settings.
	 */
	public Settings createSettings() {

		// create settings
		Settings settings = new Settings();

		// get iterator
		Iterator<Entry<Integer, Setting>> iterator = encryptedSettings.entrySet().iterator();

		// loop over mapping
		while (iterator.hasNext()) {

			// get entry
			Entry<Integer, Setting> entry = iterator.next();

			// null entry
			if (entry == null) {
				continue;
			}

			// get index and setting
			Integer index = entry.getKey();
			Setting setting = entry.getValue();

			// null setting
			if (setting == null) {
				settings.putSetting(index, null);
				continue;
			}

			// get value of setting
			Object value = setting.getValue();
			boolean restart = setting.requiresRestartOnChange();

			// string value
			if (value instanceof String) {
				settings.putSetting(index, new Setting(Base64Decoder.decodeString((String) value), restart));
			}

			// other type of value
			else {
				settings.putSetting(index, new Setting(value, restart));
			}
		}

		// return settings
		return settings;
	}

	/**
	 * Encrypts the given settings object.
	 *
	 * @param settings
	 *            Application settings.
	 * @return The encrypted settings mapping.
	 */
	private static HashMap<Integer, Setting> encrypt(Settings settings) {

		// create mapping
		HashMap<Integer, Setting> mapping = new HashMap<>();

		// get iterator
		Iterator<Entry<Integer, Setting>> iterator = settings.getSettings().entrySet().iterator();

		// loop over mapping
		while (iterator.hasNext()) {

			// get entry
			Entry<Integer, Setting> entry = iterator.next();

			// null entry
			if (entry == null) {
				continue;
			}

			// get index and value
			Integer index = entry.getKey();
			Setting setting = entry.getValue();

			// null setting
			if (setting == null) {
				mapping.put(index, null);
				continue;
			}

			// get value of setting
			Object value = setting.getValue();
			boolean restart = setting.requiresRestartOnChange();

			// string value
			if (value instanceof String) {
				mapping.put(index, new Setting(Base64Encoder.encodeString((String) value), restart));
			}

			// other type of value
			else {
				mapping.put(index, new Setting(value, restart));
			}
		}

		// return mapping
		return mapping;
	}
}
