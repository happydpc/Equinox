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

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;

import equinox.Equinox;
import equinox.controller.DamageContributionViewPanel;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.ParameterizedTask;
import equinox.task.automation.ParameterizedTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for generate number of peaks statistics plot task.
 *
 * @author Murat Artim
 * @date 21 Jul 2016
 * @time 11:37:47
 */
public class GenerateNumPeaksPlot extends TemporaryFileCreatingTask<Path> implements LongRunningTask, SingleInputTask<SpectrumItem>, ParameterizedTaskOwner<Path> {

	/** Equivalent stress. */
	private SpectrumItem eqStress_;

	/** True to plot after generation. */
	private final boolean plot_;

	/** Path to output file. */
	private final Path output_;

	/** Automatic tasks. */
	private HashMap<String, ParameterizedTask<Path>> automaticTasks_ = null;

	/** Automatic task execution mode. */
	private boolean executeAutomaticTasksInParallel_ = true;

	/**
	 * Creates generate number of peaks statistics plot task.
	 *
	 * @param eqStress
	 *            Equivalent stress. Can be null for automatic execution.
	 * @param plot
	 *            True to plot after generation.
	 * @param output
	 *            Path to output file. Can be null if plot should not be saved to output file.
	 */
	public GenerateNumPeaksPlot(SpectrumItem eqStress, boolean plot, Path output) {
		eqStress_ = eqStress;
		plot_ = plot;
		output_ = output;
	}

	@Override
	public void setAutomaticInput(SpectrumItem input) {
		eqStress_ = input;
	}

	@Override
	public void setAutomaticTaskExecutionMode(boolean isParallel) {
		executeAutomaticTasksInParallel_ = isParallel;
	}

	@Override
	public void addParameterizedTask(String taskID, ParameterizedTask<Path> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, ParameterizedTask<Path>> getParameterizedTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Generate typical flight number of peaks plot";
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT_STATISTICS);

		// update info
		updateMessage("Generating typical flight number of peaks plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create statement
				try (Statement statement = connection.createStatement()) {

					// plot typical flight number of peaks
					Path file = plotNumPeaks(statement);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}

					// save plot
					savePlot(statement, connection, file);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					Thread.interrupted();
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}

		// return
		return output_;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// plot
		if (plot_) {
			taskPanel_.getOwner().runTaskInParallel(new PlotFastNumPeaks(eqStress_));
		}

		// no automatic task
		if (automaticTasks_ == null)
			return;

		try {

			// get output
			Path output = get();

			// manage automatic tasks
			parameterizedTaskOwnerSucceeded(output, automaticTasks_, taskPanel_, executeAutomaticTasksInParallel_);
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
		parameterizedTaskOwnerFailed(automaticTasks_, executeAutomaticTasksInParallel_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		parameterizedTaskOwnerFailed(automaticTasks_, executeAutomaticTasksInParallel_);
	}

	/**
	 * Plots number of peaks plot.
	 *
	 * @param statement
	 *            Database statement.
	 * @return The path to plot image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plotNumPeaks(Statement statement) throws Exception {

		// update info
		updateMessage("Plotting typical flight number of peaks...");

		// create path to output image
		Path output = output_ == null ? getWorkingDirectory().resolve("typicalFlightNumPeaks.png") : output_;

		// get STF file
		STFFile stfFile = (STFFile) eqStress_.getParentItem();

		// create chart
		String title = "Typical Flight Number of Peaks";
		title += "\n(" + FileType.getNameWithoutExtension(stfFile.getName()) + ")";
		JFreeChart chart = ChartFactory.createBarChart(title, "Flight", "Number of peaks", null, PlotOrientation.VERTICAL, false, false, false);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.getDomainAxis().setMaximumCategoryLabelLines(10);
		plot.setRangePannable(false);

		// set item label generator
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setBaseToolTipGenerator(null);

		// plot
		CategoryDataset dataset = generatePlot(statement, stfFile);

		// set dataset
		plot.setDataset(dataset);

		// set colors
		if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				renderer.setSeriesPaint(i, DamageContributionViewPanel.COLORS[i]);
			}
		}

		// set label visibility
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, true);
		}

		// setup chart dimensions
		int width = 658;
		int height = 597;

		// plot
		ChartUtilities.saveChartAsPNG(output.toFile(), chart, width, height);

		// return path to output image
		return output;
	}

	/**
	 * Generates statistics plot.
	 *
	 * @param statement
	 *            Database statement.
	 * @param stfFile
	 *            STF file.
	 * @return The chart dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private CategoryDataset generatePlot(Statement statement, STFFile stfFile) throws Exception {

		// update info
		updateMessage("Generating typical flight number of peaks plot...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get ANA file ID
		int anaFileID = ((Spectrum) eqStress_.getParentItem().getParentItem()).getANAFileID();

		// create and execute query
		String sql = "select name, num_peaks from ana_flights where ";
		sql += "file_id = " + anaFileID;
		sql += " order by num_peaks desc";
		statement.setMaxRows(10);
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// add data to series
			while (resultSet.next()) {

				// get flight name and peaks
				String name = resultSet.getString("name");
				int peaks = resultSet.getInt("num_peaks");

				// add chart series
				dataset.addValue(peaks, "Statistics", name);
			}
		}

		// reset statement
		statement.setMaxRows(0);

		// return dataset
		return dataset;
	}

	/**
	 * Saves the statistics plot to database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @param file
	 *            Path to level crossings plot.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void savePlot(Statement statement, Connection connection, Path file) throws Exception {

		// update info
		updateMessage("Saving typical flight number of peaks plot to database...");

		// get pilot point id
		int id = eqStress_.getParentItem().getID();

		// check if any data exists in database
		boolean exists = false;
		String sql = "select image from pilot_point_st_nop where id = " + id;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				exists = true;
			}
		}

		// create statement
		if (exists) {
			sql = "update pilot_point_st_nop set image = ? where id = " + id;
		}
		else {
			sql = "insert into pilot_point_st_nop(id, image) values(?, ?)";
		}
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			byte[] imageBytes = new byte[(int) file.toFile().length()];
			try (ImageInputStream imgStream = ImageIO.createImageInputStream(file.toFile())) {
				imgStream.read(imageBytes);
				try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
					if (exists) {
						update.setBlob(1, inputStream, imageBytes.length);
						update.executeUpdate();
					}
					else {
						update.setInt(1, id);
						update.setBlob(2, inputStream, imageBytes.length);
						update.executeUpdate();
					}
				}
			}
		}
	}
}
