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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.controller.DownloadViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.Pair;
import equinox.data.fileType.Spectrum;
import equinox.dataServer.remote.data.DownloadInfo;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointSearchInput;
import equinox.dataServer.remote.message.AdvancedPilotPointSearchRequest;
import equinox.dataServer.remote.message.AdvancedPilotPointSearchResponse;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.ParameterizedTask;
import equinox.task.automation.ParameterizedTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for advanced pilot point search task.
 *
 * @author Murat Artim
 * @date Feb 15, 2016
 * @time 1:15:42 PM
 */
public class AdvancedPilotPointSearch extends InternalEquinoxTask<ArrayList<DownloadInfo>> implements ShortRunningTask, DatabaseQueryListenerTask, ParameterizedTaskOwner<Pair<PilotPointInfo, Spectrum>>, SingleInputTask<Spectrum> {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Automatic task input. */
	private Spectrum spectrum_;

	/** Search input. */
	private final PilotPointSearchInput input;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/** Automatic tasks. */
	private HashMap<String, ParameterizedTask<Pair<PilotPointInfo, Spectrum>>> automaticTasks_ = null;

	/** Automatic task execution mode. */
	private boolean executeAutomaticTasksInParallel_ = true;

	/**
	 * Creates advanced pilot point search task.
	 *
	 * @param input
	 *            Search input.
	 */
	public AdvancedPilotPointSearch(PilotPointSearchInput input) {
		this.input = input;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Search pilot points";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	public void setAutomaticTaskExecutionMode(boolean isParallel) {
		executeAutomaticTasksInParallel_ = isParallel;
	}

	@Override
	public void addParameterizedTask(String taskID, ParameterizedTask<Pair<PilotPointInfo, Spectrum>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, ParameterizedTask<Pair<PilotPointInfo, Spectrum>>> getParameterizedTasks() {
		return automaticTasks_;
	}

	@Override
	public void setAutomaticInput(Spectrum spectrum) {
		spectrum_ = spectrum;
	}

	@Override
	protected ArrayList<DownloadInfo> call() throws Exception {

		// check permission
		checkPermission(Permission.SEARCH_PILOT_POINT);

		// update progress info
		updateTitle("Searching pilot points");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		ArrayList<DownloadInfo> results = null;

		try {

			// create request message
			AdvancedPilotPointSearchRequest request = new AdvancedPilotPointSearchRequest();
			request.setListenerHashCode(hashCode());
			request.setInput(input);

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getDataServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForDataServer(this, isQueryCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// task cancelled
			if (isCancelled())
				return null;

			// get query message
			DataMessage message = serverMessageRef.get();

			// permission denied
			if (message instanceof DatabaseQueryPermissionDenied)
				throw new PermissionDeniedException(((DatabaseQueryPermissionDenied) message).getPermission());

			// query failed
			else if (message instanceof DatabaseQueryFailed)
				throw new ServerDatabaseQueryFailedException((DatabaseQueryFailed) message);

			// query succeeded
			else if (message instanceof AdvancedPilotPointSearchResponse) {
				results = ((AdvancedPilotPointSearchResponse) message).getSearchResults();
			}

			// return results
			return results;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set results to download panel
		try {

			// get results
			ArrayList<DownloadInfo> results = get();

			// executed by user
			if (automaticTasks_ == null) {
				DownloadViewPanel panel = (DownloadViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.DOWNLOAD_VIEW);
				panel.setDownloadItems(results, input);
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.DOWNLOAD_VIEW);
			}

			// executed for automatic tasks
			else {

				// no results found
				if (results == null || results.isEmpty()) {
					addWarning("No pilot point found with given search criteria. Cannot execute furter connected tasks.");
					return;
				}

				// get only first result
				PilotPointInfo firstResult = (PilotPointInfo) results.get(0);

				// manage automatic tasks
				taskSucceeded(new Pair<>(firstResult, spectrum_), automaticTasks_, taskPanel_, executeAutomaticTasksInParallel_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		taskFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		taskFailed(automaticTasks_);
	}
}
