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

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.Equinox;
import equinox.network.NetworkWatcher;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;
import equinoxServer.remote.data.SharedFileInfo;
import equinoxServer.remote.data.SharedFileInfo.SharedFileInfoType;
import equinoxServer.remote.message.DatabaseQueryFailed;
import equinoxServer.remote.message.DatabaseQueryMessage;
import equinoxServer.remote.message.DatabaseQueryPermissionDenied;
import equinoxServer.remote.message.UploadDamageContributionsRequest;
import equinoxServer.remote.message.UploadDamageContributionsResponse;
import equinoxServer.remote.utility.FilerConnection;
import equinoxServer.remote.utility.Permission;

/**
 * Class for upload damage contributions task.
 *
 * @author Murat Artim
 * @date 19 Aug 2017
 * @time 21:49:50
 *
 */
public class UploadDamageContributions extends TemporaryFileCreatingTask<Boolean> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Path to MS Excel file. */
	private final Path xlsFile_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DatabaseQueryMessage> serverMessageRef;

	/**
	 * Creates upload damage contributions task.
	 *
	 * @param xlsFile
	 *            Path to MS Excel file.
	 */
	public UploadDamageContributions(Path xlsFile) {
		xlsFile_ = xlsFile;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Upload damage contributions";
	}

	@Override
	public void respondToDatabaseQueryMessage(DatabaseQueryMessage message) throws Exception {
		processServerDatabaseQueryMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.UPLOAD_DAMAGE_CONTRIBUTIONS);

		// update progress info
		updateTitle("Uploading damage contributions...");
		updateMessage("Please wait...");

		// upload file
		String url = uploadInputFile();

		// initialize variables
		NetworkWatcher watcher = null;
		boolean removeListener = false;
		boolean isUploaded = false;

		try {

			// create request message
			UploadDamageContributionsRequest request = new UploadDamageContributionsRequest();
			request.setDatabaseQueryID(hashCode());

			// create and set share file info
			SharedFileInfo info = new SharedFileInfo();
			info.setInfo(SharedFileInfoType.OWNER, Equinox.USER.getUsername());
			info.setInfo(SharedFileInfoType.FILE_TYPE, SharedFileInfo.FILE);
			info.setInfo(SharedFileInfoType.FILE_NAME, xlsFile_.getFileName().toString());
			info.setInfo(SharedFileInfoType.DATA_SIZE, xlsFile_.toFile().length());
			info.setInfo(SharedFileInfoType.DATA_URL, url);
			request.setSharedFileInfo(info);

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
				return null;

			// get query message
			DatabaseQueryMessage message = serverMessageRef.get();

			// permission denied
			if (message instanceof DatabaseQueryPermissionDenied)
				throw new PermissionDeniedException(((DatabaseQueryPermissionDenied) message).getPermission());

			// query failed
			else if (message instanceof DatabaseQueryFailed)
				throw new ServerDatabaseQueryFailedException((DatabaseQueryFailed) message);

			// query succeeded
			else if (message instanceof UploadDamageContributionsResponse) {
				isUploaded = ((UploadDamageContributionsResponse) message).isUploaded();
			}

			// return result
			return isUploaded;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeDatabaseQueryListener(this);
			}
		}
	}

	/**
	 * Uploads input Excel file and returns URL to the uploaded file.
	 *
	 * @return URL to the uploaded file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String uploadInputFile() throws Exception {

		// zip input file
		Path zipFile = getWorkingDirectory().resolve(FileType.getNameWithoutExtension(xlsFile_));
		Utility.zipFile(xlsFile_, zipFile.toFile(), this);

		// update info
		updateMessage("Uploading input file to filer...");
		String url = null;

		// get filer connection
		try (FilerConnection filer = getFilerConnection()) {

			// set path to destination file
			url = filer.getDirectoryPath(FilerConnection.EXCHANGE) + "/" + Equinox.USER.getAlias() + "_" + getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".zip";

			// upload file to filer
			filer.getSftpChannel().put(zipFile.toString(), url);
		}

		// return url
		return url;
	}
}
