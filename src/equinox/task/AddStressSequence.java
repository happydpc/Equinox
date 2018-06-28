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

import java.nio.file.Path;
import java.sql.Connection;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.ExternalStressSequence;
import equinox.plugin.FileType;
import equinox.process.LoadSIGMAFile;
import equinox.process.LoadSTHFile;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableAddStressSequence;

/**
 * Class for add stress sequence task.
 *
 * @author Murat Artim
 * @date Mar 11, 2015
 * @time 3:00:28 PM
 */
public class AddStressSequence extends TemporaryFileCreatingTask<ExternalStressSequence> implements LongRunningTask, SavableTask {

	/** Path to input file. */
	private final Path sequenceFile_, flsFile_;

	/**
	 * Creates add stress sequence task.
	 *
	 * @param sigmaFile
	 *            Path to input SIGMA file.
	 */
	public AddStressSequence(Path sigmaFile) {
		sequenceFile_ = sigmaFile;
		flsFile_ = null;
	}

	/**
	 * Creates add stress sequence task.
	 *
	 * @param sthFile
	 *            Path to input STH file.
	 * @param flsFile
	 *            Path to input FLS file.
	 */
	public AddStressSequence(Path sthFile, Path flsFile) {
		sequenceFile_ = sthFile;
		flsFile_ = flsFile;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Add stress sequence";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableAddStressSequence(sequenceFile_, flsFile_);
	}

	@Override
	protected ExternalStressSequence call() throws Exception {

		// check permission
		checkPermission(Permission.ADD_NEW_STRESS_SEQUENCE);

		// update progress info
		updateTitle("Loading stress sequence");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// initialize sequence
				ExternalStressSequence sequence = null;

				// get file type
				FileType type = FileType.getFileType(sequenceFile_.toFile());

				// input file is STH file
				if (type.equals(FileType.STH)) {
					sequence = new LoadSTHFile(this, sequenceFile_, flsFile_).start(connection);
				}
				else if (type.equals(FileType.SIGMA)) {
					sequence = new LoadSIGMAFile(this, sequenceFile_).start(connection);
				}

				// task cancelled
				if (isCancelled() || sequence == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return
				return sequence;
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

		try {

			// add stress sequence to file tree
			taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot().getChildren().add(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
