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

import equinox.network.NetworkWatcher;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;
import equinoxServer.remote.message.DatabaseQueryFailed;
import equinoxServer.remote.message.DatabaseQueryMessage;
import equinoxServer.remote.message.DatabaseQueryPermissionDenied;
import equinoxServer.remote.message.GetUserPermissionsRequest;
import equinoxServer.remote.message.GetUserPermissionsResponse;
import equinoxServer.remote.utility.Permission;

/**
 * Class for get user permissions task.
 *
 * @author Murat Artim
 * @date 6 Apr 2018
 * @time 01:45:33
 */
public class GetUserPermissions extends InternalEquinoxTask<Permission[]> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** User info. */
	private final String alias;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DatabaseQueryMessage> serverMessageRef;

	/** Requesting panel. */
	private final UserPermissionRequestingPanel panel;

	/**
	 * Creates get user permissions task.
	 *
	 * @param alias
	 *            User alias.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetUserPermissions(String alias, UserPermissionRequestingPanel panel) {
		this.alias = alias;
		this.panel = panel;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Get user permissions";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public void respondToDatabaseQueryMessage(DatabaseQueryMessage message) throws Exception {
		processServerDatabaseQueryMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Permission[] call() throws Exception {

		// check permission
		checkPermission(Permission.GET_USER_PERMISSIONS);

		// update progress info
		updateTitle("Retrieving user permissions...");
		updateMessage("Please wait...");

		// initialize variables
		NetworkWatcher watcher = null;
		boolean removeListener = false;
		Permission[] permissions = null;

		try {

			// create request message
			GetUserPermissionsRequest request = new GetUserPermissionsRequest();
			request.setDatabaseQueryID(hashCode());
			request.setAlias(alias);

			// task cancelled
			if (isCancelled())
				return null;

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
			else if (message instanceof GetUserPermissionsResponse) {
				permissions = ((GetUserPermissionsResponse) message).getPermissions();
			}

			// return result
			return permissions;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeDatabaseQueryListener(this);
			}
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			panel.setUserPermissions(alias, get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for all user permission requesting panels.
	 *
	 * @author Murat Artim
	 * @date 6 Apr 2018
	 * @time 11:35:52
	 */
	public interface UserPermissionRequestingPanel {

		/**
		 * Sets user permissions.
		 *
		 * @param alias
		 *            User alias.
		 * @param permissions
		 *            User permissions.
		 */
		void setUserPermissions(String alias, Permission[] permissions);
	}
}
