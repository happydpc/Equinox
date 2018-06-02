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

import java.sql.Connection;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYSeriesCollection;

import equinox.Equinox;
import equinox.controller.LevelCrossingViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.input.LevelCrossingInput;
import equinox.process.PlotLevelCrossingProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinoxServer.remote.data.Permission;

/**
 * Class for plot level crossing task.
 *
 * @author Murat Artim
 * @date Jul 21, 2014
 * @time 11:11:54 AM
 */
public class PlotLevelCrossing extends InternalEquinoxTask<XYSeriesCollection> implements ShortRunningTask {

	/** Input. */
	private final LevelCrossingInput input_;

	/**
	 * Creates plot level crossing task.
	 *
	 * @param input
	 *            Level crossing input.
	 */
	public PlotLevelCrossing(LevelCrossingInput input) {
		input_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Plot level crossing";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected XYSeriesCollection call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_LEVEL_CROSSINGS);

		// update progress info
		updateTitle("Plotting level crossing...");

		// initialize dataset
		XYSeriesCollection dataset = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			dataset = new PlotLevelCrossingProcess(this, input_).start(connection);
		}

		// return data set
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get level crossing view panel
			LevelCrossingViewPanel panel = (LevelCrossingViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.LEVEL_CROSSING_VIEW);

			// set data
			String xAxisLabel = input_.isNormalize() ? "Number of Cycles (Normalized by spectrum validities)" : "Number of Cycles";
			panel.plottingCompleted(get(), xAxisLabel, false);

			// show level crossing view panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.LEVEL_CROSSING_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
