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

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.Settings;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinoxServer.remote.message.Handshake;
import equinoxServer.remote.utility.FilerConnection;

/**
 * Class for get server connection info task.
 *
 * @author Murat Artim
 * @date 3 Aug 2016
 * @time 15:44:35
 */
public class GetServerConnectionInfo extends TemporaryFileCreatingTask<HashMap<String, String>> implements ShortRunningTask {

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get server connection info";
	}

	@Override
	protected HashMap<String, String> call() throws Exception {

		// update info
		updateMessage("Getting server connection info...");

		// create array
		HashMap<String, String> connectionInfo = new HashMap<>();

		// create path to connection settings file
		Path connSettingsFile = getWorkingDirectory().resolve(FilerConnection.EXCHANGE_SERVER_CONN_FILE);

		// download connection settings file from filer
		try (FilerConnection filer = getFilerConnection()) {

			// get URL to connection settings file
			String url = filer.getDirectoryPath(FilerConnection.SETTINGS) + "/" + FilerConnection.EXCHANGE_SERVER_CONN_FILE;

			// download
			if (filer.fileExists(url)) {
				filer.getSftpChannel().get(url, connSettingsFile.toString());
			}

			// cannot locate connection settings file
			else
				throw new Exception("Cannot locate server connection settings file on filer. Cannot connect to Equinox server.");
		}

		// create file reader
		try (BufferedReader reader = Files.newBufferedReader(connSettingsFile, Charset.defaultCharset())) {

			// read file till the end
			String line;
			while ((line = reader.readLine()) != null) {

				// empty or comment line
				if (line.startsWith("#") || line.trim().isEmpty()) {
					continue;
				}

				// split line
				String[] split = line.split("=");
				connectionInfo.put(split[0].trim(), split[1].trim());
			}
		}

		// return connection info
		return connectionInfo;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get connection info
			HashMap<String, String> info = get();

			// null info
			if (info == null || info.isEmpty()) {
				String msg = "Exchange server connection information could not be obtained.";
				Equinox.LOGGER.warning(msg);
				taskPanel_.getOwner().getOwner().getNotificationPane().showWarning(msg, null);
				return;
			}

			// get connection info
			Settings settings = taskPanel_.getOwner().getOwner().getSettings();

			// set to settings
			settings.setValue(Settings.NETWORK_HOSTNAME, info.get("serverHostname"));
			settings.setValue(Settings.NETWORK_PORT, info.get("serverPort"));

			// save settings
			taskPanel_.getOwner().runTaskInParallel(new SaveSettings(settings, false));
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}

		// connect to network server
		finally {
			taskPanel_.getOwner().getOwner().getNetworkWatcher().connect(new Handshake(Equinox.USER.getAlias()));
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// connect to network server
		taskPanel_.getOwner().getOwner().getNetworkWatcher().connect(new Handshake(Equinox.USER.getAlias()));
	}
}
