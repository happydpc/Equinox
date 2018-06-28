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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.Equinox;
import equinox.controller.MissionParameterPlotViewPanel;
import equinox.controller.MissionParameterPlotViewPanel.PlotCompletionPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.EquivalentStressRatioComparisonInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for generate stress ratios with mission parameters task.
 *
 * @author Murat Artim
 * @date Jun 23, 2015
 * @time 9:41:48 AM
 */
public class GenerateStressRatiosWithMissionParameters extends InternalEquinoxTask<XYSeriesCollection> implements ShortRunningTask {

	/** Input. */
	private final EquivalentStressRatioComparisonInput input_;

	/** Requesting panel. */
	private final PlotCompletionPanel panel_;

	/**
	 * Creates generate stress ratios with mission parameters task.
	 *
	 * @param input
	 *            Comparison input.
	 * @param panel
	 *            Requesting panel.
	 */
	public GenerateStressRatiosWithMissionParameters(EquivalentStressRatioComparisonInput input, PlotCompletionPanel panel) {
		input_ = input;
		panel_ = panel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Generate equivalent stress ratios with mission parameters";
	}

	@Override
	protected XYSeriesCollection call() throws Exception {

		// check permission
		checkPermission(Permission.GENERATE_EQUIVALENT_STRESS_RATIOS);

		// update progress info
		updateTitle("Generating stress ratios...");

		// create data set
		XYSeriesCollection dataset = new XYSeriesCollection();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// get type of equivalent stress
			SpectrumItem item = input_.getEquivalentStresses().get(0);

			// equivalent stress
			if (item instanceof FatigueEquivalentStress || item instanceof PreffasEquivalentStress || item instanceof LinearEquivalentStress || item instanceof FastFatigueEquivalentStress || item instanceof FastPreffasEquivalentStress || item instanceof FastLinearEquivalentStress) {
				generateStressRatios(connection, dataset);
			}
			else if (item instanceof ExternalFatigueEquivalentStress || item instanceof ExternalPreffasEquivalentStress || item instanceof ExternalLinearEquivalentStress) {
				generateExternalStressRatios(connection, dataset);
			}
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

			// get mission parameters view panel
			MissionParameterPlotViewPanel panel = (MissionParameterPlotViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);

			// set data
			String ratioType = getRatioType(input_.getEquivalentStresses().get(0));
			String title = ratioType + "\nbased on mission '" + input_.getBasisMission() + "'";
			String xAxisLabel = input_.getMissionParameterName();
			String yAxisLabel = ratioType;
			panel.plottingCompleted(get(), title, xAxisLabel, yAxisLabel, panel_, false, false, "Mission Parameters View");

			// show mission parameters view panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Returns ratio type.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Spectrum item.
	 */
	private static String getRatioType(SpectrumItem item) {
		String stressType = null;
		if (item instanceof FatigueEquivalentStress) {
			stressType = "Fatigue Eq. Stress Ratio";
		}
		else if (item instanceof PreffasEquivalentStress) {
			stressType = "Preffas Eq. Stress Ratio";
		}
		else if (item instanceof LinearEquivalentStress) {
			stressType = "Linear Prop. Eq. Stress Ratio";
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			stressType = "Fatigue Eq. Stress Ratio";
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			stressType = "Preffas Eq. Stress Ratio";
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			stressType = "Linear Prop. Eq. Stress Ratio";
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			stressType = "Fatigue Eq. Stress Ratio";
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			stressType = "Preffas Eq. Stress Ratio";
		}
		else if (item instanceof FastLinearEquivalentStress) {
			stressType = "Linear Prop. Eq. Stress Ratio";
		}
		return stressType;
	}

	/**
	 * Generates life factors.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void generateExternalStressRatios(Connection connection, XYSeriesCollection dataset) throws Exception {

		// update progress info
		updateMessage("Computing stress ratios...");

		// get inputs
		String basisMission = input_.getBasisMission();
		String missionParameterName = input_.getMissionParameterName();
		ArrayList<SpectrumItem> equivalentStresses = input_.getEquivalentStresses();

		// set table name
		String tableName = getTableName(equivalentStresses.get(0));

		// prepare statement to get equivalent stress and material slope for the basis
		String sql = "select stress from " + tableName + " where id = ?";
		try (PreparedStatement getBasis = connection.prepareStatement(sql)) {

			// prepare statement to get life factor
			sql = "select id, stress/? as ratio from " + tableName + " where id = ?";
			try (PreparedStatement getRatio = connection.prepareStatement(sql)) {

				// prepare statement to get mission parameter values
				sql = "select val from ext_sth_mission_parameters where sth_id = ? and name = '" + missionParameterName + "'";
				try (PreparedStatement getMP = connection.prepareStatement(sql)) {

					// loop over equivalent stresses
					for (SpectrumItem item1 : equivalentStresses) {

						// get mission
						String mission = getMission(item1);

						// basis
						if (basisMission.equals(mission)) {

							// get mission parameter value
							Double missionParameterValue = getMissionParameterValueForExternalStressRatio(item1, getMP);

							// no mission parameter defined
							if (missionParameterValue == null) {
								continue;
							}

							// create series
							String stressName1 = getStressName(item1);
							XYSeries series = getSeries(stressName1, dataset);

							// get basis material slope and equivalent stress
							double basisStress = 0.0;
							getBasis.setInt(1, item1.getID());
							try (ResultSet resultSet = getBasis.executeQuery()) {
								while (resultSet.next()) {
									basisStress = resultSet.getDouble("stress");
								}
							}
							getRatio.setDouble(1, basisStress);

							// loop over equivalent stresses
							for (SpectrumItem item2 : equivalentStresses) {

								// get mission
								mission = getMission(item2);

								// don't include basis mission
								if (!input_.getIncludeBasisMission() && basisMission.equals(mission)) {
									continue;
								}

								// get stress name
								String stressName2 = getStressName(item2);

								// same name
								if (stressName2.equals(stressName1)) {

									// get mission parameter value
									missionParameterValue = getMissionParameterValueForExternalStressRatio(item2, getMP);

									// no mission parameter defined
									if (missionParameterValue == null) {
										continue;
									}

									// add data
									getRatio.setInt(2, item2.getID());
									try (ResultSet resultSet = getRatio.executeQuery()) {
										while (resultSet.next()) {
											double ratio = resultSet.getDouble("ratio");
											series.add(missionParameterValue.doubleValue(), ratio);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Generates life factors.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void generateStressRatios(Connection connection, XYSeriesCollection dataset) throws Exception {

		// update progress info
		updateMessage("Computing stress ratios...");

		// get inputs
		String basisMission = input_.getBasisMission();
		String missionParameterName = input_.getMissionParameterName();
		ArrayList<SpectrumItem> equivalentStresses = input_.getEquivalentStresses();

		// set table name
		String tableName = getTableName(equivalentStresses.get(0));

		// prepare statement to get equivalent stress and material slope for the basis
		String sql = "select stress from " + tableName + " where id = ?";
		try (PreparedStatement getBasis = connection.prepareStatement(sql)) {

			// prepare statement to get life factor
			sql = "select id, stress/? as ratio from " + tableName + " where id = ?";
			try (PreparedStatement getRatio = connection.prepareStatement(sql)) {

				// prepare statement to get mission parameters from STF file
				sql = "select val from stf_mission_parameters where stf_id = ? and name = '" + missionParameterName + "'";
				try (PreparedStatement getMPFromSTF = connection.prepareStatement(sql)) {

					// prepare statement to get mission parameter values from spectrum
					sql = "select val from cdf_mission_parameters where cdf_id = ? and name = '" + missionParameterName + "'";
					try (PreparedStatement getMPFromCDF = connection.prepareStatement(sql)) {

						// loop over equivalent stresses
						for (SpectrumItem item1 : equivalentStresses) {

							// get mission
							String mission = getMission(item1);

							// basis
							if (basisMission.equals(mission)) {

								// get mission parameter value
								Double missionParameterValue = getMissionParameterValueForStressRatio(item1, getMPFromSTF, getMPFromCDF);

								// no mission parameter defined
								if (missionParameterValue == null) {
									continue;
								}

								// create series
								String stressName1 = getStressName(item1);
								XYSeries series = getSeries(stressName1, dataset);

								// get basis material slope and equivalent stress
								double basisStress = 0.0;
								getBasis.setInt(1, item1.getID());
								try (ResultSet resultSet = getBasis.executeQuery()) {
									while (resultSet.next()) {
										basisStress = resultSet.getDouble("stress");
									}
								}
								getRatio.setDouble(1, basisStress);

								// loop over equivalent stresses
								for (SpectrumItem item2 : equivalentStresses) {

									// get mission
									mission = getMission(item2);

									// don't include basis mission
									if (!input_.getIncludeBasisMission() && basisMission.equals(mission)) {
										continue;
									}

									// get stress name
									String stressName2 = getStressName(item2);

									// same name
									if (stressName2.equals(stressName1)) {

										// get mission parameter value
										missionParameterValue = getMissionParameterValueForStressRatio(item2, getMPFromSTF, getMPFromCDF);

										// no mission parameter defined
										if (missionParameterValue == null) {
											continue;
										}

										// add data
										getRatio.setInt(2, item2.getID());
										try (ResultSet resultSet = getRatio.executeQuery()) {
											while (resultSet.next()) {
												double ratio = resultSet.getDouble("ratio");
												series.add(missionParameterValue.doubleValue(), ratio);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Returns database table name.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Database table name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getTableName(SpectrumItem item) throws Exception {
		String tableName = null;
		if (item instanceof FatigueEquivalentStress) {
			tableName = "fatigue_equivalent_stresses";
		}
		else if (item instanceof PreffasEquivalentStress) {
			tableName = "preffas_equivalent_stresses";
		}
		else if (item instanceof LinearEquivalentStress) {
			tableName = "linear_equivalent_stresses";
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			tableName = "ext_fatigue_equivalent_stresses";
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			tableName = "ext_preffas_equivalent_stresses";
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			tableName = "ext_linear_equivalent_stresses";
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			tableName = "fast_fatigue_equivalent_stresses";
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			tableName = "fast_preffas_equivalent_stresses";
		}
		else if (item instanceof FastLinearEquivalentStress) {
			tableName = "fast_linear_equivalent_stresses";
		}
		return tableName;
	}

	/**
	 * Returns stress name.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressName(SpectrumItem item) throws Exception {
		String name = "";
		if (item instanceof FatigueEquivalentStress) {
			name = getStressNameForFatigueEquivalentStress((FatigueEquivalentStress) item);
		}
		else if (item instanceof PreffasEquivalentStress) {
			name = getStressNameForPreffasEquivalentStress((PreffasEquivalentStress) item);
		}
		else if (item instanceof LinearEquivalentStress) {
			name = getStressNameForLinearEquivalentStress((LinearEquivalentStress) item);
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			name = getStressNameForExternalFatigueEquivalentStress((ExternalFatigueEquivalentStress) item);
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			name = getStressNameForExternalPreffasEquivalentStress((ExternalPreffasEquivalentStress) item);
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			name = getStressNameForExternalLinearEquivalentStress((ExternalLinearEquivalentStress) item);
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			name = getStressNameForFastFatigueEquivalentStress((FastFatigueEquivalentStress) item);
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			name = getStressNameForFastPreffasEquivalentStress((FastPreffasEquivalentStress) item);
		}
		else if (item instanceof FastLinearEquivalentStress) {
			name = getStressNameForFastLinearEquivalentStress((FastLinearEquivalentStress) item);
		}
		return name.substring(0, name.lastIndexOf("\n"));
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForFastFatigueEquivalentStress(FastFatigueEquivalentStress stress) throws Exception {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getEID() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForFastPreffasEquivalentStress(FastPreffasEquivalentStress stress) throws Exception {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getEID() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForFastLinearEquivalentStress(FastLinearEquivalentStress stress) throws Exception {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getEID() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForFatigueEquivalentStress(FatigueEquivalentStress stress) throws Exception {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getParentItem().getEID() + "\n";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForPreffasEquivalentStress(PreffasEquivalentStress stress) throws Exception {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getParentItem().getEID() + "\n";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForLinearEquivalentStress(LinearEquivalentStress stress) throws Exception {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getParentItem().getEID() + "\n";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForExternalFatigueEquivalentStress(ExternalFatigueEquivalentStress stress) throws Exception {

		// initialize name
		String name = "";

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(stress.getParentItem().getName()) + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForExternalPreffasEquivalentStress(ExternalPreffasEquivalentStress stress) throws Exception {

		// initialize name
		String name = "";

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(stress.getParentItem().getName()) + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForExternalLinearEquivalentStress(ExternalLinearEquivalentStress stress) throws Exception {

		// initialize name
		String name = "";

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(stress.getParentItem().getName()) + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns fatigue mission.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Fatigue mission.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getMission(SpectrumItem item) throws Exception {
		String mission = null;
		if (item instanceof FatigueEquivalentStress) {
			mission = ((FatigueEquivalentStress) item).getParentItem().getParentItem().getMission();
		}
		else if (item instanceof PreffasEquivalentStress) {
			mission = ((PreffasEquivalentStress) item).getParentItem().getParentItem().getMission();
		}
		else if (item instanceof LinearEquivalentStress) {
			mission = ((LinearEquivalentStress) item).getParentItem().getParentItem().getMission();
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			mission = ((ExternalFatigueEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			mission = ((ExternalPreffasEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			mission = ((ExternalLinearEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			mission = ((FastFatigueEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			mission = ((FastPreffasEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof FastLinearEquivalentStress) {
			mission = ((FastLinearEquivalentStress) item).getParentItem().getMission();
		}
		return mission;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param item
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForStressRatio(SpectrumItem item, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {
		Double value = null;
		if (item instanceof FatigueEquivalentStress) {
			value = getMissionParameterValueForFatigueStressRatio((FatigueEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof PreffasEquivalentStress) {
			value = getMissionParameterValueForPreffasStressRatio((PreffasEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof LinearEquivalentStress) {
			value = getMissionParameterValueForLinearStressRatio((LinearEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			value = getMissionParameterValueForFastFatigueStressRatio((FastFatigueEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			value = getMissionParameterValueForFastPreffasStressRatio((FastPreffasEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof FastLinearEquivalentStress) {
			value = getMissionParameterValueForFastLinearStressRatio((FastLinearEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForFastFatigueStressRatio(FastFatigueEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForFastPreffasStressRatio(FastPreffasEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForFastLinearStressRatio(FastLinearEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForFatigueStressRatio(FatigueEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForPreffasStressRatio(PreffasEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForLinearStressRatio(LinearEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param item
	 *            External equivalent stress.
	 * @param getMP
	 *            Database statement for getting mission parameter values.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForExternalStressRatio(SpectrumItem item, PreparedStatement getMP) throws Exception {
		Double value = null;
		if (item instanceof ExternalFatigueEquivalentStress) {
			value = getMissionParameterValueForExternalFatigueStressRatio((ExternalFatigueEquivalentStress) item, getMP);
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			value = getMissionParameterValueForExternalPreffasStressRatio((ExternalPreffasEquivalentStress) item, getMP);
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			value = getMissionParameterValueForExternalLinearStressRatio((ExternalLinearEquivalentStress) item, getMP);
		}
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            External equivalent stress.
	 * @param getMP
	 *            Database statement for getting mission parameter values.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForExternalFatigueStressRatio(ExternalFatigueEquivalentStress stress, PreparedStatement getMP) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value
		getMP.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMP.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            External equivalent stress.
	 * @param getMP
	 *            Database statement for getting mission parameter values.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForExternalPreffasStressRatio(ExternalPreffasEquivalentStress stress, PreparedStatement getMP) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value
		getMP.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMP.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            External equivalent stress.
	 * @param getMP
	 *            Database statement for getting mission parameter values.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForExternalLinearStressRatio(ExternalLinearEquivalentStress stress, PreparedStatement getMP) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value
		getMP.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMP.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// return value
		return value;
	}

	/**
	 * Gets the series for the given name.
	 *
	 * @param seriesName
	 *            Series name.
	 * @param dataset
	 *            List containing all the series.
	 * @return The series for the given name.
	 */
	private static XYSeries getSeries(String seriesName, XYSeriesCollection dataset) {

		// series already exists
		int index = dataset.indexOf(seriesName);
		if (index != -1)
			return dataset.getSeries(index);

		// create new series
		XYSeries series = new XYSeries(seriesName, true, false);
		dataset.addSeries(series);

		// return series
		return series;
	}
}
