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
import java.sql.Statement;

import equinox.Equinox;
import equinox.data.MissionParameter;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTask;

/**
 * Class for assign mission parameters task.
 *
 * @author Murat Artim
 * @date Oct 7, 2014
 * @time 5:21:10 PM
 */
public class AssignMissionParameters extends InternalEquinoxTask<Void> implements ShortRunningTask, AutomaticTask<Spectrum> {

	/** Spectrum item. */
	private SpectrumItem spectrumItem_;

	/** Mission parameters. */
	private final MissionParameter[] missionParameters_;

	/**
	 * Creates assign mission parameters task.
	 *
	 * @param spectrumItem
	 *            Spectrum item. Can be null for automatic execution.
	 * @param missionParameters
	 *            Mission parameters.
	 */
	public AssignMissionParameters(SpectrumItem spectrumItem, MissionParameter[] missionParameters) {
		spectrumItem_ = spectrumItem;
		missionParameters_ = missionParameters;
	}

	@Override
	public void setAutomaticInput(Spectrum spectrum) {
		spectrumItem_ = spectrum;
	}

	@Override
	public String getTaskTitle() {
		return "Assign mission parameters";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.ASSIGN_MISSION_PARAMETERS_TO_SPECTRUM);

		// update progress info
		updateTitle("Assigning mission parameters...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create statement
				try (Statement statement = connection.createStatement()) {
					assignParameters(statement, connection);
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}
		return null;
	}

	/**
	 * Edits input file in database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void assignParameters(Statement statement, Connection connection) throws Exception {

		// update info
		updateMessage("Assigning mission parameters to '" + spectrumItem_.getName() + "'...");

		// setup table and column names
		String tableName = null, colName = null;
		if (spectrumItem_ instanceof Spectrum) {
			tableName = "cdf_mission_parameters";
			colName = "cdf_id";
		}
		else if (spectrumItem_ instanceof STFFile) {
			tableName = "stf_mission_parameters";
			colName = "stf_id";
		}
		else if (spectrumItem_ instanceof ExternalStressSequence) {
			tableName = "ext_sth_mission_parameters";
			colName = "sth_id";
		}

		// remove all mission parameters
		String sql = "delete from " + tableName + " where " + colName + " = " + spectrumItem_.getID();
		statement.executeUpdate(sql);

		// there are no mission parameters
		if (missionParameters_.length == 0)
			return;

		// insert mission parameters
		sql = "insert into " + tableName + "(" + colName + ", name, val) values(?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// loop over mission parameters
			for (MissionParameter mp : missionParameters_) {

				// execute update
				update.setInt(1, spectrumItem_.getID()); // spectrum item ID
				update.setString(2, mp.getName()); // parameter name
				update.setDouble(3, mp.getValue()); // parameter value
				update.executeUpdate();
			}
		}
	}
}
