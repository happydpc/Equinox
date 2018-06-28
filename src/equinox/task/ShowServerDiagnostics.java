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

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import equinox.controller.TimeStatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.network.NetworkWatcher;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for show server diagnostics task.
 *
 * @author Murat Artim
 * @date 21 Jul 2017
 * @time 10:38:49
 *
 */
public class ShowServerDiagnostics extends InternalEquinoxTask<TimeSeriesCollection> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Diagnostics interval dates. */
	private final Timestamp from_, to_;

	/** Target server statistic. */
	private final ServerStatistic statistic_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates show server statistics task.
	 *
	 * @param statistic
	 *            Target server statistic.
	 * @param from
	 *            From date.
	 * @param to
	 *            To date.
	 */
	public ShowServerDiagnostics(ServerStatistic statistic, Timestamp from, Timestamp to) {
		statistic_ = statistic;
		from_ = from;
		to_ = to;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Show server diagnostics";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected TimeSeriesCollection call() throws Exception {

		// check permission
		checkPermission(Permission.SHOW_SERVER_DIAGNOSTICS);

		// update progress info
		updateTitle("Plotting contribution statistics...");
		updateMessage("Please wait...");

		// initialize variables
		NetworkWatcher watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			ShowServerDiagnosticsRequest request = new ShowServerDiagnosticsRequest();
			request.setDatabaseQueryID(hashCode());
			request.setFrom(from_);
			request.setTo(to_);
			request.setStatistic(statistic_);

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getNetworkWatcher();
			watcher.addDatabaseQueryListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForServer(this, isQueryCompleted);

			// remove from network watcher
			watcher.removeDatabaseQueryListener(this);
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
			else if (message instanceof ShowServerDiagnosticsResponse) {

				// get data
				HashMap<Timestamp, Integer> dataset = ((ShowServerDiagnosticsResponse) message).getDataset();

				// create dataset
				TimeSeries series = new TimeSeries(statistic_.getLabel());

				// add data to time series
				Iterator<Entry<Timestamp, Integer>> iterator = dataset.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Timestamp, Integer> entry = iterator.next();
					Minute minute = new Minute(new Date(entry.getKey().getTime()));
					series.add(minute, entry.getValue());
				}

				// return time series collection
				return new TimeSeriesCollection(series);
			}

			// invalid response
			throw new Exception("Invalid response received from server.");
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

		// set chart data
		try {

			// get dataset
			TimeSeriesCollection dataset = get();

			// get column plot panel
			TimeStatisticsViewPanel panel = (TimeStatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.TIME_STATS_VIEW);

			// setup plot labels
			String title = statistic_.getLabel();
			String yAxisLabel = statistic_.getLabel();
			String xAxisLabel = "Time";

			// set chart data to panel
			panel.setPlotData(dataset, title, xAxisLabel, yAxisLabel, false);

			// show chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.TIME_STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
