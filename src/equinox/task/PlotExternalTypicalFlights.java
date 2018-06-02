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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.Equinox;
import equinox.controller.ExternalPlotViewPanel;
import equinox.controller.InputPanel;
import equinox.controller.ViewPanel;
import equinox.data.SeriesKey;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.input.ExternalFlightPlotInput;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinoxServer.remote.data.Permission;

/**
 * Class for plot external typical flights task.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 5:10:42 PM
 */
public class PlotExternalTypicalFlights extends InternalEquinoxTask<XYDataset> implements ShortRunningTask {

	/** Plot input. */
	private final ExternalFlightPlotInput input_;

	/**
	 * Creates plot STH flights task.
	 *
	 * @param input
	 *            Plot input.
	 */
	public PlotExternalTypicalFlights(ExternalFlightPlotInput input) {
		input_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Plot external typical flights";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected XYDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT);

		// update progress info
		updateTitle("Plotting external typical flights...");

		// create dataset
		XYSeriesCollection dataset = new XYSeriesCollection();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// plot stresses
			plot(connection, dataset);
		}

		// return dataset
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get spectrum view panel
			ExternalPlotViewPanel panel = (ExternalPlotViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.EXTERNAL_PLOT_VIEW);

			// set data
			panel.plottingCompleted(get());

			// show spectrum plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.EXTERNAL_PLOT_VIEW);
			taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.PLOT_EXTERNAL_FLIGHTS_PANEL);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Plots input flights on the given chart.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plot(Connection connection, XYSeriesCollection dataset) throws Exception {

		// get flight IDs
		updateMessage("Plotting stresses...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// loop over flights
			for (ExternalFlight flight : input_.getFlights()) {

				// update info
				updateMessage("Getting peaks for flight '" + flight.getName() + "' from database...");

				// create series name
				String name = getFlightName(flight);

				// create series
				XYSeries series = new XYSeries(new SeriesKey(checkName(name, dataset), flight.getID()));

				// create query
				String sql = "select peak_num, peak_val";
				sql += " from ext_sth_peaks_" + flight.getParentItem().getParentItem().getID() + " where flight_id = " + flight.getID() + " order by peak_num";

				// add chart data to series
				try (ResultSet resultSet = statement.executeQuery(sql)) {

					// loop over peaks
					while (resultSet.next()) {

						// get peak number
						int peakNum = resultSet.getInt("peak_num");

						// get stress
						double stress = resultSet.getDouble("peak_val");

						// add to data
						series.add(peakNum, stress);
					}
				}

				// add series to dataset
				dataset.addSeries(series);
			}
		}
	}

	/**
	 * Returns flight name.
	 *
	 * @param flight
	 *            Flight.
	 * @return Flight name.
	 */
	private String getFlightName(ExternalFlight flight) {

		// initialize name
		String name = "";

		// get naming options
		boolean[] options = input_.getNamingOptions();

		// include stress sequence name
		if (options[0]) {
			name += flight.getParentItem().getParentItem().getName() + ", ";
		}

		// include EID
		if (options[1]) {
			name += ExternalStressSequence.getEID(flight.getParentItem().getParentItem().getName()) + ", ";
		}

		// include flight name
		if (options[2]) {
			name += flight.getName() + ", ";
		}

		// include A/C program
		if (options[3]) {
			name += flight.getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (options[4]) {
			name += flight.getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (options[5]) {
			name += flight.getParentItem().getParentItem().getMission() + ", ";
		}

		// return name
		return name.substring(0, name.lastIndexOf(", "));
	}

	/**
	 * Checks given series name for uniqueness.
	 *
	 * @param name
	 *            Series name.
	 * @param dataset
	 *            Dataset.
	 * @return Modified name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String checkName(String name, XYSeriesCollection dataset) throws Exception {
		if (dataset.indexOf(name) == -1)
			return name;
		return checkName(name + " ", dataset);
	}
}
