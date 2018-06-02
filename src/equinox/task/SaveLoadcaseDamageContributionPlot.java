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

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.MultiplePiePlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.TableOrder;

import equinox.Equinox;
import equinox.controller.DamageContributionViewPanel;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.ui.PieLabelGenerator;
import equinox.plugin.FileType;
import equinox.process.PlotDamageContributionsProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for save loadcase damage contribution plot task.
 *
 * @author Murat Artim
 * @date 26 Jul 2016
 * @time 09:55:26
 */
public class SaveLoadcaseDamageContributionPlot extends TemporaryFileCreatingTask<Void> implements ShortRunningTask {

	/** Damage contributions item. */
	private final LoadcaseDamageContributions contributions_;

	/**
	 * Creates save damage contribution plot task.
	 *
	 * @param contributions
	 *            Damage contribution.
	 */
	public SaveLoadcaseDamageContributionPlot(LoadcaseDamageContributions contributions) {
		contributions_ = contributions;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save loadcase damage contributions plot for '" + contributions_.getName() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// update info
		updateMessage("Saving loadcase damage contributions plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// plot
			Path file = plot(connection);

			// save plot
			savePlot(connection, file);
		}

		// return
		return null;
	}

	/**
	 * Plots damage contributions on a file.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Path to image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plot(Connection connection) throws Exception {

		// update info
		updateMessage("Plotting damage contributions...");

		// create path to output image
		Path output = getWorkingDirectory().resolve("damageContributions.png");

		// set shadow theme
		ChartFactory.setChartTheme(new StandardChartTheme("JFree/Shadow", true));

		// create chart
		String title = "Loadcase Damage Contributions of";
		title += "\n" + FileType.getNameWithoutExtension(contributions_.getParentItem().getName()) + "";
		JFreeChart chart = ChartFactory.createMultiplePieChart(title, null, TableOrder.BY_COLUMN, true, false, false);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup multiple plot
		MultiplePiePlot mplot = (MultiplePiePlot) chart.getPlot();
		mplot.setOutlinePaint(null);
		mplot.setBackgroundPaint(null);
		mplot.setNoDataMessage("No data available.");

		// setup sub-chart plot
		JFreeChart subchart = mplot.getPieChart();
		subchart.setBackgroundPaint(null);
		TextTitle subChartTitle = subchart.getTitle();
		subChartTitle.setPaint(new Color(112, 128, 144));
		subChartTitle.setFont(subChartTitle.getFont().deriveFont(14f));
		PiePlot splot = (PiePlot) subchart.getPlot();
		splot.setNoDataMessage("No data available.");
		splot.setLabelGenerator(new PieLabelGenerator("{0} ({2})"));
		splot.setLabelBackgroundPaint(new Color(220, 220, 220));
		splot.setIgnoreZeroValues(true);
		splot.setMaximumLabelWidth(0.20);
		splot.setInteriorGap(0.04);
		splot.setBaseSectionOutlinePaint(new Color(245, 245, 245));
		splot.setSectionOutlinesVisible(true);
		splot.setBaseSectionOutlineStroke(new BasicStroke(1.5f));
		splot.setBackgroundPaint(new Color(112, 128, 144, 20));
		splot.setOutlinePaint(new Color(112, 128, 144));
		splot.setExplodePercent("Rest", 0.20);

		// plot
		DefaultCategoryDataset dataset = new PlotDamageContributionsProcess(this, contributions_).start(connection);

		// set dataset
		mplot.setDataset(dataset);

		// set colors
		if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++)
				if (dataset.getRowKey(i).equals("Rest")) {
					splot.setSectionPaint(dataset.getRowKey(i), Color.LIGHT_GRAY);
				}
				else {
					splot.setSectionPaint(dataset.getRowKey(i), DamageContributionViewPanel.COLORS[i]);
				}
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
	 * Saves the level crossings plot to database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param file
	 *            Path to level crossings plot.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void savePlot(Connection connection, Path file) throws Exception {

		// update info
		updateMessage("Saving damage contributions plot to database...");

		// get pilot point id
		int id = contributions_.getParentItem().getID();

		// check if any data exists in database
		boolean exists = false;
		String sql = "select image from pilot_point_dc where id = " + id;
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					exists = true;
				}
			}
		}

		// create statement
		if (exists) {
			sql = "update pilot_point_dc set image = ? where id = " + id;
		}
		else {
			sql = "insert into pilot_point_dc(id, image) values(?, ?)";
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
