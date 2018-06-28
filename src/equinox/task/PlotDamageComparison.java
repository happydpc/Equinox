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
import java.util.concurrent.ExecutionException;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.CompareDamageContributionsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.input.CompareDamageContributionsInput;
import equinox.dataServer.remote.data.ContributionType;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for plot damage comparison task.
 *
 * @author Murat Artim
 * @date Apr 16, 2015
 * @time 12:25:45 PM
 */
public class PlotDamageComparison extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** Input. */
	private final CompareDamageContributionsInput input_;

	/** Contribution type. */
	private final ContributionType contributionType_;

	/**
	 * Creates plot damage comparison task.
	 *
	 * @param input
	 *            Input.
	 * @param contributionType
	 *            Contribution type.
	 */
	public PlotDamageComparison(CompareDamageContributionsInput input, ContributionType contributionType) {
		input_ = input;
		contributionType_ = contributionType;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Plot damage comparison";
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// update progress info
		updateTitle("Plotting damage contribution comparison...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement for getting total damages
			String sql = "select stress from dam_contributions where contributions_id = ?";
			try (PreparedStatement getTotalDamage = connection.prepareStatement(sql)) {

				// prepare statement for getting individual damages
				sql = "select stress from dam_contribution where contributions_id = ? and name = ?";
				try (PreparedStatement getDamage = connection.prepareStatement(sql)) {

					// loop over contributions
					for (LoadcaseDamageContributions contribution : input_.getContributions()) {

						// get series name
						String seriesName = getSeriesName(contribution, dataset);

						// get total damage
						double totalDamage = 0.0;
						getTotalDamage.setInt(1, contribution.getID());
						try (ResultSet resultSet = getTotalDamage.executeQuery()) {
							while (resultSet.next()) {
								totalDamage = resultSet.getDouble("stress");
							}
						}

						// loop over contribution names
						double totalCont = 0.0;
						getDamage.setInt(1, contribution.getID());
						for (String name : input_.getContributionNames()) {

							// get damage
							getDamage.setString(2, name);
							try (ResultSet resultSet1 = getDamage.executeQuery()) {
								while (resultSet1.next()) {

									// get damage percentage
									double damage = resultSet1.getDouble("stress");
									if (name.equals(ContributionType.GAG.getName())) {
										damage = damage * 100.0 / totalDamage;
									}
									else {
										damage = (totalDamage - damage) * 100.0 / totalDamage;
									}
									damage = Math.floor(damage);
									totalCont += damage;

									// add to dataset
									dataset.addValue(damage, name, seriesName);
								}
							}
						}

						// add rest
						dataset.addValue(Math.floor(100.0 - totalCont), "Rest", seriesName);
					}
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

			// get level crossing view panel
			CompareDamageContributionsViewPanel panel = (CompareDamageContributionsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.COMPARE_DAMAGE_CONTRIBUTIONS_VIEW);

			// set data
			panel.setPlotData(get(), contributionType_);

			// show damage contributions view panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.COMPARE_DAMAGE_CONTRIBUTIONS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Returns series name according selected naming options.
	 *
	 * @param contribution
	 *            Contribution to get name for.
	 * @param dataset
	 *            Dataset.
	 * @return Series name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getSeriesName(LoadcaseDamageContributions contribution, DefaultCategoryDataset dataset) throws Exception {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.includeName(CompareDamageContributionsInput.SPECTRUM_NAME)) {
			name += contribution.getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.includeName(CompareDamageContributionsInput.STF_NAME)) {
			name += FileType.getNameWithoutExtension(contribution.getParentItem().getName()) + "\n";
		}

		// include EID
		if (input_.includeName(CompareDamageContributionsInput.EID)) {
			name += contribution.getParentItem().getEID() + "\n";
		}

		// include material name
		if (input_.includeName(CompareDamageContributionsInput.MATERIAL_NAME)) {
			name += contribution.getMaterialName() + "\n";
		}

		// include A/C program
		if (input_.includeName(CompareDamageContributionsInput.PROGRAM)) {
			name += contribution.getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.includeName(CompareDamageContributionsInput.SECTION)) {
			name += contribution.getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.includeName(CompareDamageContributionsInput.MISSION)) {
			name += contribution.getParentItem().getMission() + "\n";
		}

		// return name
		return checkName(name.substring(0, name.lastIndexOf("\n")), dataset);
	}

	/**
	 * Checks series name for uniqueness.
	 *
	 * @param name
	 *            Series name to check.
	 * @param dataset
	 *            Plot series that this stress belongs to.
	 * @return The name.
	 */
	private String checkName(String name, DefaultCategoryDataset dataset) {
		for (int i = 0; i < dataset.getColumnCount(); i++) {
			if (dataset.getColumnKey(i).equals(name)) {
				name += " ";
				return checkName(name, dataset);
			}
		}
		return name;
	}
}
