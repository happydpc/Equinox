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

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.DamageAngle;
import equinox.process.PlotDamageAnglesProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for plot damage angles task.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 9:31:32 AM
 */
public class PlotDamageAngles extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/**
	 * Enumeration for results ordering.
	 *
	 * @author Murat Artim
	 * @date Jul 10, 2014
	 * @time 1:05:06 PM
	 */
	public enum ResultOrdering {

		/** Ordering. */
		ANGLE("Order by angles"), DESCENDING("Order by descending eq. stresses"), ASCENDING("Order by ascending eq. stresses");

		/** Name of ordering. */
		private final String name_;

		/**
		 * Creates ordering constant.
		 *
		 * @param name
		 *            Name of ordering.
		 */
		ResultOrdering(String name) {
			name_ = name;
		}

		@Override
		public String toString() {
			return name_;
		}
	}

	/** Order and data label options. */
	private final boolean showlabels_;

	/** Damage angles. */
	private final DamageAngle[] damageAngles_;

	/** Results ordering. */
	private final ResultOrdering order_;

	/**
	 * Creates plot damage angles task.
	 *
	 * @param damageAngles
	 *            Damage angles.
	 * @param order
	 *            Results ordering.
	 * @param showlabels
	 *            True to show labels.
	 */
	public PlotDamageAngles(DamageAngle[] damageAngles, ResultOrdering order, boolean showlabels) {
		damageAngles_ = damageAngles;
		order_ = order;
		showlabels_ = showlabels;
	}

	@Override
	public String getTaskTitle() {
		return "Plot damage angles";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// update progress info
		updateTitle("Plotting damage angles...");

		// create data set
		DefaultCategoryDataset dataset = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			dataset = new PlotDamageAnglesProcess(this, damageAngles_, order_).start(connection);
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

			// setup plot labels
			String title = "Damage Angles";
			String xAxisLabel = "Angle (in degrees)";
			String yAxisLabel = "Fatigue Equivalent Stress";

			// set chart data to panel
			panel.setPlotData(dataset, title, null, xAxisLabel, yAxisLabel, damageAngles_.length > 1, showlabels_, false);

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
