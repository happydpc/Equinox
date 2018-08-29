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
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.STFFile;
import equinox.plugin.FileType;
import equinox.process.SaveSTFFile;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.utility.Utility;

/**
 * Class for save STF task.
 *
 * @author Murat Artim
 * @date Feb 12, 2014
 * @time 12:27:42 PM
 */
public class SaveSTF extends TemporaryFileCreatingTask<Path> implements LongRunningTask, AutomaticTask<STFFile>, AutomaticTaskOwner<Path> {

	/** File item to save. */
	private STFFile file_;

	/** Output file. */
	private final File output_;

	/** Output file type. */
	private final FileType type_;

	/** Automatic tasks. */
	private HashMap<String, AutomaticTask<Path>> automaticTasks_ = null;

	/** Automatic task execution mode. */
	private boolean executeAutomaticTasksInParallel_ = true;

	/**
	 * Creates save STF task.
	 *
	 * @param file
	 *            File item to save. Can be null for automatic execution.
	 * @param output
	 *            Output file.
	 * @param type
	 *            Output file type.
	 */
	public SaveSTF(STFFile file, File output, FileType type) {
		file_ = file;
		output_ = output;
		type_ = type;
	}

	@Override
	public void setAutomaticTaskExecutionMode(boolean isParallel) {
		executeAutomaticTasksInParallel_ = isParallel;
	}

	@Override
	public void addAutomaticTask(String taskID, AutomaticTask<Path> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, AutomaticTask<Path>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Save STF file to '" + output_.getName() + "'";
	}

	@Override
	public void setAutomaticInput(STFFile input) {
		file_ = input;
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving STF file to '" + output_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// STF file format
			if (type_.equals(FileType.STF)) {
				new SaveSTFFile(this, file_, output_.toPath()).start(connection);
			}

			// ZIP format
			else if (type_.equals(FileType.ZIP)) {
				Path stfFile = getWorkingDirectory().resolve(FileType.appendExtension(file_.getName(), FileType.STF));
				new SaveSTFFile(this, file_, stfFile).start(connection);
				Utility.zipFile(stfFile, output_, this);
			}

			// GZIP file format
			else if (type_.equals(FileType.GZ)) {
				Path stfFile = getWorkingDirectory().resolve(FileType.appendExtension(file_.getName(), FileType.STF));
				new SaveSTFFile(this, file_, stfFile).start(connection);
				Utility.gzipFile(stfFile.toFile(), output_);
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
				for (AutomaticTask<Path> task : automaticTasks_.values()) {
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
