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

import org.jfree.data.category.CategoryDataset;

import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.network.NetworkWatcher;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;
import equinoxServer.remote.data.Permission;
import equinoxServer.remote.message.DatabaseQueryFailed;
import equinoxServer.remote.message.DatabaseQueryMessage;
import equinoxServer.remote.message.DatabaseQueryPermissionDenied;
import equinoxServer.remote.message.PlotSpectrumSizeRequest;
import equinoxServer.remote.message.PlotSpectrumSizeResponse;

/**
 * Class for plot spectrum size task.
 *
 * @author Murat Artim
 * @date 27 Jul 2017
 * @time 12:50:11
 *
 */
public class PlotSpectrumSize extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Aircraft program, section and mission inputs. */
	private final String program_, section_, mission_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DatabaseQueryMessage> serverMessageRef;

	/**
	 * Creates plot spectrum size task.
	 *
	 * @param program
	 *            Aircraft program. Can be null for all programs.
	 * @param section
	 *            Aircraft section. Can be null for all sections.
	 * @param mission
	 *            Fatigue mission. Can be null for all missions.
	 */
	public PlotSpectrumSize(String program, String section, String mission) {
		program_ = program;
		section_ = section;
		mission_ = mission;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Plot spectrum size";
	}

	@Override
	public void respondToDatabaseQueryMessage(DatabaseQueryMessage message) throws Exception {
		processServerDatabaseQueryMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_SPECTRUM_SIZE);

		// update progress info
		updateTitle("Plotting spectrum size...");
		updateMessage("Please wait...");

		// initialize variables
		NetworkWatcher watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			PlotSpectrumSizeRequest request = new PlotSpectrumSizeRequest();
			request.setDatabaseQueryID(hashCode());
			request.setMission(mission_);
			request.setProgram(program_);
			request.setSection(section_);

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
			else if (message instanceof PlotSpectrumSizeResponse)
				return ((PlotSpectrumSizeResponse) message).getDataset();

			// invalid response
			throw new Exception("Invalid response received from server.");
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

		// set chart data
		try {

			// get dataset
			CategoryDataset dataset = get();

			// get column plot panel
			StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

			// setup plot labels
			String title = "Spectrum Size";
			String subTitle = "";
			subTitle += program_ == null ? "" : program_;
			subTitle += section_ == null ? "" : (", " + section_);
			String yAxisLabel = "Size [MB]";
			String xAxisLabel = null;
			if (program_ == null) {
				xAxisLabel = "Aircraft Program";
			}
			else if (section_ == null) {
				xAxisLabel = "Aircraft Section";
			}
			else if (mission_ == null) {
				xAxisLabel = "Fatigue Mission";
			}
			else {
				xAxisLabel = "Fatigue Mission";
			}

			// set chart data to panel
			panel.setPlotData(dataset, title, subTitle, xAxisLabel, yAxisLabel, false, false, false);

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
