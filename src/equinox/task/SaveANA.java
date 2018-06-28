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

import equinox.Equinox;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;

/**
 * Class for save ANA task.
 *
 * @author Murat Artim
 * @date Jan 15, 2014
 * @time 10:37:46 AM
 */
public class SaveANA extends TemporaryFileCreatingTask<Void> implements LongRunningTask {

	/** ID of file item to save. */
	private final int fileID_;

	/** Output file. */
	private final File output_;

	/** Output file type. */
	private final FileType type_;

	/**
	 * Creates save ANA task.
	 *
	 * @param fileID
	 *            ID of file item to save.
	 * @param output
	 *            Output file.
	 * @param type
	 *            Output file type.
	 */
	public SaveANA(int fileID, File output, FileType type) {
		fileID_ = fileID;
		output_ = output;
		type_ = type;
	}

	@Override
	public String getTaskTitle() {
		return "Save ANA file to '" + output_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving ANA file to '" + output_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// update progress info
				updateMessage("Retrieving ANA file from database...");

				// execute query
				try (ResultSet resultSet = statement.executeQuery("select name, data from ana_files where file_id = " + fileID_)) {

					// get data
					if (resultSet.next()) {

						// get file name
						String name = resultSet.getString("name");

						// get blob
						Blob blob = resultSet.getBlob("data");

						// ANA file format
						if (type_.equals(FileType.ANA)) {
							Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
							Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
							Path anaFile = Utility.extractFileFromZIP(zipFile, this, FileType.ANA, null);
							Files.copy(anaFile, output_.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}

						// ZIP file format
						else if (type_.equals(FileType.ZIP)) {
							Files.copy(blob.getBinaryStream(), output_.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
						else if (type_.equals(FileType.GZ)) {
							Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
							Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
							Path anaFile = Utility.extractFileFromZIP(zipFile, this, FileType.ANA, null);
							Utility.gzipFile(anaFile.toFile(), output_);
						}

						// free blob
						blob.free();
					}
				}
			}
		}

		// return
		return null;
	}
}
