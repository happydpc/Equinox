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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.SingleInputTask;
import equinox.task.automation.ParameterizedTask;
import equinox.task.automation.ParameterizedTaskOwner;
import equinox.utility.Utility;

/**
 * Class for save FLS task.
 *
 * @author Murat Artim
 * @date Apr 25, 2014
 * @time 2:47:08 PM
 */
public class SaveFLS extends TemporaryFileCreatingTask<Path> implements LongRunningTask, SingleInputTask<Spectrum>, ParameterizedTaskOwner<Path> {

	/** ID of file item to save. */
	private Integer fileID_ = null;

	/** Output file. */
	private final File output_;

	/** Output file type. */
	private final FileType type_;

	/** Automatic tasks. */
	private HashMap<String, SingleInputTask<Path>> automaticTasks_ = null;

	/** Automatic task execution mode. */
	private boolean executeAutomaticTasksInParallel_ = true;

	/**
	 * Creates save FLS task.
	 *
	 * @param fileID
	 *            ID of file item to save. This can be null for automatic execution.
	 * @param output
	 *            Output file.
	 * @param type
	 *            Output file type.
	 */
	public SaveFLS(Integer fileID, File output, FileType type) {
		fileID_ = fileID;
		output_ = output;
		type_ = type;
	}

	@Override
	public void setAutomaticTaskExecutionMode(boolean isParallel) {
		executeAutomaticTasksInParallel_ = isParallel;
	}

	@Override
	public void addParameterizedTask(String taskID, ParameterizedTask<V> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, SingleInputTask<Path>> getParameterizedTasks() {
		return automaticTasks_;
	}

	@Override
	public String getTaskTitle() {
		return "Save FLS file to '" + output_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public void setAutomaticInput(Spectrum spectrum) {
		fileID_ = spectrum.getFLSFileID();
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving FLS file to '" + output_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// update progress info
				updateMessage("Retrieving FLS file from database...");

				// execute query
				try (ResultSet resultSet = statement.executeQuery("select name, data from fls_files where file_id = " + fileID_)) {

					// get data
					if (resultSet.next()) {

						// get file name
						String name = resultSet.getString("name");

						// get blob
						Blob blob = resultSet.getBlob("data");

						// FLS file format
						if (type_.equals(FileType.FLS)) {
							Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
							Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
							Path flsFile = Utility.extractFileFromZIP(zipFile, this, FileType.FLS, null);
							Files.copy(flsFile, output_.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}

						// ZIP file format
						else if (type_.equals(FileType.ZIP)) {
							Files.copy(blob.getBinaryStream(), output_.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
						else if (type_.equals(FileType.GZ)) {
							Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
							Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
							Path flsFile = Utility.extractFileFromZIP(zipFile, this, FileType.FLS, null);
							Utility.gzipFile(flsFile.toFile(), output_);
						}

						// free blob
						blob.free();
					}
				}
			}
		}

		// return
		return output_.toPath();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get output file
			Path file = get();

			// execute automatic tasks
			if (automaticTasks_ != null) {
				for (SingleInputTask<Path> task : automaticTasks_.values()) {
					task.setAutomaticInput(file);
					if (executeAutomaticTasksInParallel_) {
						taskPanel_.getOwner().runTaskInParallel((InternalEquinoxTask<?>) task);
					}
					else {
						taskPanel_.getOwner().runTaskSequentially((InternalEquinoxTask<?>) task);
					}
				}
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
