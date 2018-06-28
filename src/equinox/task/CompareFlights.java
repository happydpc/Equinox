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

import org.jfree.chart.JFreeChart;

import equinox.Equinox;
import equinox.controller.CompareFlightsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.input.FlightComparisonInput;
import equinox.process.CompareFlightsProcess;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for compare flights task.
 *
 * @author Murat Artim
 * @date Sep 16, 2014
 * @time 11:47:59 AM
 */
public class CompareFlights extends InternalEquinoxTask<JFreeChart> implements ShortRunningTask {

	/** Comparison input. */
	private final FlightComparisonInput input_;

	/**
	 * Creates compare flights task.
	 *
	 * @param input
	 *            Comparison input.
	 */
	public CompareFlights(FlightComparisonInput input) {
		input_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Compare flights";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected JFreeChart call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT_COMPARISON);

		// update progress info
		updateTitle("Comparing flights...");

		// initialize chart
		JFreeChart chart = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			chart = new CompareFlightsProcess(this, input_).start(connection);
		}

		// return chart
		return chart;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get comparison view panel
			CompareFlightsViewPanel panel = (CompareFlightsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.COMPARE_FLIGHTS_VIEW);

			// set data
			panel.setFlightComparisonChart(get(), false);

			// show flight comparison panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.COMPARE_FLIGHTS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
