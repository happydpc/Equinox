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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.controller.DownloadViewPanel;
import equinox.network.NetworkWatcher;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;
import equinoxServer.remote.data.MultiplicationTableInfo;
import equinoxServer.remote.data.MultiplicationTableInfo.MultiplicationTableInfoType;
import equinoxServer.remote.data.Permission;
import equinoxServer.remote.message.DatabaseQueryFailed;
import equinoxServer.remote.message.DatabaseQueryMessage;
import equinoxServer.remote.message.DatabaseQueryPermissionDenied;
import equinoxServer.remote.message.DeleteMultiplicationTableRequest;
import equinoxServer.remote.message.DeleteMultiplicationTableResponse;

/**
 * Class for delete multiplication table from global database task.
 *
 * @author Murat Artim
 * @date Mar 30, 2016
 * @time 11:26:44 AM
 */
public class DeleteMultiplicationTableFromGlobalDB extends InternalEquinoxTask<Boolean> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Download view panel. */
	private final DownloadViewPanel downloadViewPanel_;

	/** Multiplication table to delete. */
	private final MultiplicationTableInfo multTable_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DatabaseQueryMessage> serverMessageRef;

	/**
	 * Creates delete multiplication table from global database task.
	 *
	 * @param multTable
	 *            Multiplication table to delete.
	 * @param downloadViewPanel
	 *            Download view panel.
	 */
	public DeleteMultiplicationTableFromGlobalDB(MultiplicationTableInfo multTable, DownloadViewPanel downloadViewPanel) {
		multTable_ = multTable;
		downloadViewPanel_ = downloadViewPanel;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Delete multiplication table '" + (String) multTable_.getInfo(MultiplicationTableInfoType.NAME) + "' from ESCSAS database";
	}

	@Override
	public void respondToDatabaseQueryMessage(DatabaseQueryMessage message) throws Exception {
		processServerDatabaseQueryMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.DELETE_MULTIPLICATION_TABLE);

		// update progress info
		updateTitle("Deleting multiplication table from the databse...");
		updateMessage("Please wait...");

		// initialize variables
		NetworkWatcher watcher = null;
		boolean removeListener = false;
		boolean isDeleted = false;

		try {

			// create request message
			DeleteMultiplicationTableRequest request = new DeleteMultiplicationTableRequest();
			request.setDatabaseQueryID(hashCode());
			request.setMultiplicationTableInfo(multTable_);

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
			else if (message instanceof DeleteMultiplicationTableResponse) {
				isDeleted = ((DeleteMultiplicationTableResponse) message).isMultiplicationTableDeleted();
			}

			// return result
			return isDeleted;
		}

		// remove from network watcher
		finally {
			if ((watcher != null) && removeListener) {
				watcher.removeDatabaseQueryListener(this);
			}
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set results to plugins panel
		try {

			// notify panel
			if (get()) {
				downloadViewPanel_.removeDownloadItem(multTable_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
