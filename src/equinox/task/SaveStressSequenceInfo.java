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

import equinox.Equinox;
import equinox.data.fileType.ExternalStressSequence;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for save stress sequence info task.
 *
 * @author Murat Artim
 * @date Jun 17, 2016
 * @time 11:29:50 PM
 */
public class SaveStressSequenceInfo extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Info index. */
	public static final int PROGRAM = 0, SECTION = 1, MISSION = 2;

	/** Spectrum. */
	private final ExternalStressSequence sequence_;

	/** Info array. */
	private final String[] info_;

	/**
	 * Creates save stress sequence info task.
	 *
	 * @param sequence
	 *            Stress sequence.
	 * @param info
	 *            Info array.
	 */
	public SaveStressSequenceInfo(ExternalStressSequence sequence, String[] info) {
		sequence_ = sequence;
		info_ = info;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save stress sequence info";
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateMessage("Saving stress sequence info to database");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// update info
				updateSequenceInfo(connection);

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return
				return null;
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
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// update sequence info
		sequence_.setProgram(info_[PROGRAM]);
		sequence_.setSection(info_[SECTION]);
		sequence_.setMission(info_[MISSION]);
	}

	/**
	 * Updates stress sequence info in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateSequenceInfo(Connection connection) throws Exception {

		// prepare statement
		String sql = "update ext_sth_files set ac_program = ?, ac_section = ?, fat_mission = ? ";
		sql += "where file_id = " + sequence_.getID();
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			if ((info_[PROGRAM] == null) || info_[PROGRAM].trim().isEmpty()) {
				update.setNull(1, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(1, info_[PROGRAM].trim());
			}
			if ((info_[SECTION] == null) || info_[SECTION].trim().isEmpty()) {
				update.setNull(2, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(2, info_[SECTION].trim());
			}
			if ((info_[MISSION] == null) || info_[MISSION].trim().isEmpty()) {
				update.setNull(3, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(3, info_[MISSION].trim());
			}
			update.executeUpdate();
		}
	}
}
