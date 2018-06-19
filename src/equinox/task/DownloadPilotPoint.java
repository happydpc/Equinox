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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.data.fileType.Spectrum;
import equinox.network.NetworkWatcher;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;
import equinoxServer.remote.data.PilotPointInfo;
import equinoxServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinoxServer.remote.message.DatabaseQueryFailed;
import equinoxServer.remote.message.DatabaseQueryMessage;
import equinoxServer.remote.message.DatabaseQueryPermissionDenied;
import equinoxServer.remote.message.DownloadPilotPointRequest;
import equinoxServer.remote.message.DownloadPilotPointResponse;
import equinoxServer.remote.utility.FilerConnection;
import equinoxServer.remote.utility.Permission;

/**
 * Class for download pilot point task.
 *
 * @author Murat Artim
 * @date Feb 16, 2016
 * @time 11:04:03 AM
 */
public class DownloadPilotPoint extends TemporaryFileCreatingTask<AddSTFFiles> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Pilot point info. */
	private final PilotPointInfo info_;

	/** Output file. */
	private Path output_;

	/** Spectrum to add the pilot point. */
	private final Spectrum spectrum_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DatabaseQueryMessage> serverMessageRef;

	/**
	 * Creates download pilot point task.
	 *
	 * @param info
	 *            Pilot point info.
	 * @param output
	 *            Output file. Null should be given if the pilot point should be added to spectrum.
	 * @param spectrum
	 *            Spectrum to add the pilot point. Can be null for just downloading.
	 */
	public DownloadPilotPoint(PilotPointInfo info, Path output, Spectrum spectrum) {
		info_ = info;
		output_ = output;
		spectrum_ = spectrum;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Download pilot point";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public void respondToDatabaseQueryMessage(DatabaseQueryMessage message) throws Exception {
		processServerDatabaseQueryMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected AddSTFFiles call() throws Exception {

		// check permission
		checkPermission(Permission.DOWNLOAD_PILOT_POINT);

		// update progress info
		updateTitle("Downloading pilot point");

		// check output file
		if (output_ == null) {
			String name = (String) info_.getInfo(PilotPointInfoType.NAME);
			output_ = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
		}

		// download pilot point data
		downloadData();

		// add to database
		if (spectrum_ != null) {

			// extract archive
			Path stfFile = Utility.extractFileFromZIP(output_, this, FileType.STF, getWorkingDirectory());

			// get STF file name path
			Path stfFileNamePath = stfFile.getFileName();

			// file name exists
			if (stfFileNamePath != null) {

				// create add STF files task
				ArrayList<PilotPointInfo> info = new ArrayList<>();
				info.add(info_);
				AddSTFFiles task = new AddSTFFiles(null, spectrum_, info);

				// copy and set STF file
				ArrayList<File> inputFiles = new ArrayList<>();
				Path stfFileCopy = task.getWorkingDirectory().resolve(stfFileNamePath.toString());
				inputFiles.add(Files.copy(stfFile, stfFileCopy, StandardCopyOption.REPLACE_EXISTING).toFile());
				task.setSTFFiles(inputFiles);

				// return task
				return task;
			}
		}

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// add to database
			AddSTFFiles task = get();
			if (task != null) {
				taskPanel_.getOwner().runTaskSequentially(task);
			}
		}

		// exception occurred
		catch (Exception e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Downloads pilot point data from filer.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void downloadData() throws Exception {

		// update progress info
		updateTitle("Downloading pilot point");
		updateMessage("Downloading pilot point '" + (String) info_.getInfo(PilotPointInfoType.NAME) + "'...");

		// initialize variables
		NetworkWatcher watcher = null;
		boolean removeListener = false;

		try {

			// get pilot point ID
			long ppID = (long) info_.getInfo(PilotPointInfoType.ID);

			// create request message
			DownloadPilotPointRequest request = new DownloadPilotPointRequest();
			request.setDatabaseQueryID(hashCode());
			request.setDownloadId(ppID);

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getNetworkWatcher();
			watcher.addDatabaseQueryListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForQuery(this, isQueryCompleted);

			// remove from network watcher
			watcher.removeDatabaseQueryListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// task cancelled
			if (isCancelled())
				return;

			// get query message
			DatabaseQueryMessage message = serverMessageRef.get();

			// permission denied
			if (message instanceof DatabaseQueryPermissionDenied)
				throw new PermissionDeniedException(((DatabaseQueryPermissionDenied) message).getPermission());

			// query failed
			else if (message instanceof DatabaseQueryFailed)
				throw new ServerDatabaseQueryFailedException((DatabaseQueryFailed) message);

			// query succeeded
			else if (message instanceof DownloadPilotPointResponse) {

				// get download URL
				String downloadUrl = ((DownloadPilotPointResponse) message).getDownloadUrl();

				// download file
				if (downloadUrl != null) {
					try (FilerConnection filer = getFilerConnection()) {
						if (filer.fileExists(downloadUrl)) {
							filer.getSftpChannel().get(downloadUrl, output_.toString());
						}
					}
				}
			}
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeDatabaseQueryListener(this);
			}
		}
	}
}
