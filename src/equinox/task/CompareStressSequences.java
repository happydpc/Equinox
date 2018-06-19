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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.StressSequence;
import equinox.data.input.StressSequenceComparisonInput;
import equinox.data.input.StressSequenceComparisonInput.ComparisonCriteria;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinoxServer.remote.utility.Permission;

/**
 * Class for compare stress sequences task.
 *
 * @author Murat Artim
 * @date Apr 6, 2016
 * @time 9:11:56 PM
 */
public class CompareStressSequences extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** Comparison input. */
	private final StressSequenceComparisonInput input_;

	/** Chart labels. */
	private String xAxisLabel_, yAxisLabel_;

	/**
	 * Creates compare stress sequences task.
	 *
	 * @param input
	 *            Comparison input.
	 */
	public CompareStressSequences(StressSequenceComparisonInput input) {
		input_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Compare stress sequences";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_STRESS_SEQUENCE_COMPARISON);

		// update progress info
		updateTitle("Comparing stress sequences...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get criteria
				ComparisonCriteria criteria = input_.getCriteria();

				// number of flight types
				if (criteria.equals(ComparisonCriteria.NUM_FLIGHT_TYPES)) {
					numberOfFlightTypes(connection, statement, dataset);
				}
				else if (criteria.equals(ComparisonCriteria.NUM_PEAKS_WITH_OCCURRENCE) || criteria.equals(ComparisonCriteria.NUM_PEAKS_WITHOUT_OCCURRENCE)) {
					numberOfPeaks(connection, dataset, criteria.equals(ComparisonCriteria.NUM_PEAKS_WITH_OCCURRENCE));
				}
				else if (criteria.equals(ComparisonCriteria.VALIDITY)) {
					validities(connection, dataset);
				}
				else if (criteria.equals(ComparisonCriteria.AVG_NUM_PEAKS)) {
					averageNumberOfPeaks(connection, dataset);
				}
				else if (criteria.equals(ComparisonCriteria.MAX_TOTAL)) {
					getHighestLowest(connection, dataset, "max_val", true);
				}
				else if (criteria.equals(ComparisonCriteria.MAX_1G)) {
					getHighestLowest(connection, dataset, "max_1g", true);
				}
				else if (criteria.equals(ComparisonCriteria.MAX_INC)) {
					getHighestLowest(connection, dataset, "max_inc", true);
				}
				else if (criteria.equals(ComparisonCriteria.MAX_DP)) {
					getHighestLowest(connection, dataset, "max_dp", true);
				}
				else if (criteria.equals(ComparisonCriteria.MAX_DT)) {
					getHighestLowest(connection, dataset, "max_dt", true);
				}
				else if (criteria.equals(ComparisonCriteria.MIN_TOTAL)) {
					getHighestLowest(connection, dataset, "min_val", false);
				}
				else if (criteria.equals(ComparisonCriteria.MIN_1G)) {
					getHighestLowest(connection, dataset, "min_1g", false);
				}
				else if (criteria.equals(ComparisonCriteria.MIN_INC)) {
					getHighestLowest(connection, dataset, "min_inc", false);
				}
				else if (criteria.equals(ComparisonCriteria.MIN_DP)) {
					getHighestLowest(connection, dataset, "min_dp", false);
				}
				else if (criteria.equals(ComparisonCriteria.MIN_DT)) {
					getHighestLowest(connection, dataset, "min_dt", false);
				}
			}
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

			// get dataset
			CategoryDataset dataset = get();

			// get column plot panel
			StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

			// set chart data to panel
			boolean legendVisible = dataset.getRowCount() > 1;
			panel.setPlotData(dataset, yAxisLabel_, null, xAxisLabel_, yAxisLabel_, legendVisible, input_.getLabelDisplay(), false);

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Generates comparison for number of flight types.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param dataset
	 *            Chart series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void numberOfFlightTypes(Connection connection, Statement statement, DefaultCategoryDataset dataset) throws Exception {

		// update progress info
		updateMessage("Retrieving number of flight types...");

		// set labels
		xAxisLabel_ = "Stress Sequence";
		yAxisLabel_ = "Number of flight types";

		// create query
		String sql = "select file_id, num_flights from sth_files where ";
		ArrayList<StressSequence> sequences = input_.getStressSequences();
		for (int i = 0; i < sequences.size(); i++) {
			sql += "file_id = " + sequences.get(i).getID() + (i == sequences.size() - 1 ? "" : " or ");
		}
		sql += " order by num_flights " + (input_.getOrder() ? "desc" : "asc");

		// execute query for getting number of flights
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// add data to series
			while (resultSet.next()) {

				// get STH file ID
				int sthID = resultSet.getInt("file_id");

				// get mission
				String mission = getMission(sthID, sequences);

				// get sequence name
				String name = getSequenceName(sthID, sequences, dataset);

				// add chart series
				dataset.addValue(resultSet.getInt("num_flights"), mission, name);
			}
		}
	}

	/**
	 * Generates comparison for number of peaks.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart series.
	 * @param withOccurrence
	 *            True if with flight occurrences.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void numberOfPeaks(Connection connection, DefaultCategoryDataset dataset, boolean withOccurrence) throws Exception {

		// update progress info
		updateMessage("Retrieving number of peaks...");

		// set labels
		xAxisLabel_ = "Stress Sequence";
		yAxisLabel_ = "Number of peaks " + (withOccurrence ? "with occurrences" : "without occurrences");

		// prepare statement
		String sql = "select ";
		sql += withOccurrence ? "sum(num_peaks*validity)" : "sum(num_peaks)";
		sql += " as peaks from sth_flights where file_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			// loop over sequences
			ArrayList<StressSequence> sequences = input_.getStressSequences();
			for (StressSequence sequence : sequences) {

				// get STH file ID
				int sthID = sequence.getID();

				// get mission
				String mission = sequence.getParentItem().getMission();

				// get sequence name
				String name = getSequenceName(sthID, sequences, dataset);

				// set STH file ID and execute query
				statement.setInt(1, sthID);
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						dataset.addValue(resultSet.getInt("peaks"), mission, name);
					}
				}
			}
		}
	}

	/**
	 * Generates comparison for validities.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void validities(Connection connection, DefaultCategoryDataset dataset) throws Exception {

		// update progress info
		updateMessage("Retrieving number of flights...");

		// set labels
		xAxisLabel_ = "Stress Sequence";
		yAxisLabel_ = "Number of Flights";

		// prepare statement
		String sql = "select sum(validity) as validities from sth_flights where file_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			// loop over sequences
			ArrayList<StressSequence> sequences = input_.getStressSequences();
			for (StressSequence sequence : sequences) {

				// get STH file ID
				int sthID = sequence.getID();

				// get mission
				String mission = sequence.getParentItem().getMission();

				// get sequence name
				String name = getSequenceName(sthID, sequences, dataset);

				// set sequence ID and execute query
				statement.setInt(1, sthID);
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						dataset.addValue(resultSet.getDouble("validities"), mission, name);
					}
				}
			}
		}
	}

	/**
	 * Generates comparison for average number of peaks.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            File info table list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void averageNumberOfPeaks(Connection connection, DefaultCategoryDataset dataset) throws Exception {

		// update progress info
		updateMessage("Retrieving number of peaks...");

		// set labels
		xAxisLabel_ = "Stress Sequence";
		yAxisLabel_ = "Average Number of Peaks";

		// prepare statement
		String sql = "select sum(num_peaks*validity) as peaks, sum(validity) as validities";
		sql += " from sth_flights where file_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			// loop over sequences
			ArrayList<StressSequence> sequences = input_.getStressSequences();
			for (StressSequence sequence : sequences) {

				// get STH file ID
				int sthID = sequence.getID();

				// get mission
				String mission = sequence.getParentItem().getMission();

				// get sequence name
				String name = getSequenceName(sthID, sequences, dataset);

				// set STH file ID and execute query
				statement.setInt(1, sthID);
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						double avg = resultSet.getInt("peaks") / resultSet.getDouble("validities");
						dataset.addValue((int) avg, mission, name);
					}
				}
			}
		}
	}

	/**
	 * Gets the spectrum with the highest or lowest value of the input from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            File info table list.
	 * @param colName
	 *            Database column name.
	 * @param isDesc
	 *            True if descending order.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getHighestLowest(Connection connection, DefaultCategoryDataset dataset, String colName, boolean isDesc) throws Exception {

		// set label
		String label = input_.getCriteria().toString();

		// update progress info
		updateMessage("Retrieving " + label + "...");

		// set labels
		xAxisLabel_ = "Stress Sequence";
		yAxisLabel_ = label;

		// prepare statement
		String sql = "select " + colName + " from sth_flights where file_id = ? order by " + colName + (isDesc ? " desc" : " asc");
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setMaxRows(1);

			// loop over sequences
			ArrayList<StressSequence> sequences = input_.getStressSequences();
			for (StressSequence sequence : sequences) {

				// get STH file ID
				int sthID = sequence.getID();

				// get mission
				String mission = sequence.getParentItem().getMission();

				// get sequence name
				String name = getSequenceName(sthID, sequences, dataset);

				// set STH file ID and execute query
				statement.setInt(1, sthID);
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						dataset.addValue(resultSet.getDouble(colName), mission, name);
					}
				}
			}

			// reset statement
			statement.setMaxRows(0);
		}
	}

	/**
	 * Returns the name of sequence.
	 *
	 * @param sequenceID
	 *            Spectrum ID.
	 * @param sequences
	 *            List of compared spectra.
	 * @param dataset
	 *            Series that this sequence belongs to.
	 * @return The name of sequence.
	 */
	private String getSequenceName(int sequenceID, ArrayList<StressSequence> sequences, DefaultCategoryDataset dataset) {

		// initialize name
		String name = "";

		// loop over sequences
		for (StressSequence sequence : sequences) {

			// not the sequence
			if (sequenceID != sequence.getID()) {
				continue;
			}

			// include spectrum name
			if (input_.getIncludeSpectrumName()) {
				name += sequence.getParentItem().getParentItem().getName() + "\n";
			}

			// include STF file name
			if (input_.getIncludeSTFName()) {
				name += sequence.getParentItem().getName() + "\n";
			}

			// include EID
			if (input_.getIncludeEID()) {
				name += sequence.getParentItem().getEID() + "\n";
			}

			// include stress sequence name
			if (input_.getIncludeSequenceName()) {
				name += sequence.getName() + "\n";
			}

			// include A/C program
			if (input_.getIncludeProgram()) {
				name += sequence.getParentItem().getParentItem().getProgram() + "\n";
			}

			// include A/C section
			if (input_.getIncludeSection()) {
				name += sequence.getParentItem().getParentItem().getSection() + "\n";
			}

			// include fatigue mission
			if (input_.getIncludeMission()) {
				name += sequence.getParentItem().getMission() + "\n";
			}

			// break
			break;
		}

		// return name
		return name.substring(0, name.lastIndexOf("\n"));
	}

	/**
	 * Returns the name of mission.
	 *
	 * @param sequenceID
	 *            Stress sequence ID.
	 * @param spectra
	 *            List of compared sequences.
	 * @return The name of series.
	 */
	private static String getMission(int sequenceID, ArrayList<StressSequence> spectra) {
		for (StressSequence spectrum : spectra) {
			if (sequenceID == spectrum.getID())
				return spectrum.getParentItem().getMission();
		}
		return null;
	}
}
