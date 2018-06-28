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
import java.sql.Connection;

import equinox.Equinox;
import equinox.data.fileType.ExternalStressSequence;
import equinox.process.SaveExternalSTH;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for save external stress sequence as STH task.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 11:26:53 AM
 */
public class SaveExternalStressSequenceAsSTH extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Process of this task. */
	private final SaveExternalSTH process_;

	/**
	 * Creates save external stress sequence as STH task.
	 *
	 * @param sequence
	 *            Stress sequence to save.
	 * @param output
	 *            Output file.
	 */
	public SaveExternalStressSequenceAsSTH(ExternalStressSequence sequence, File output) {
		process_ = new SaveExternalSTH(this, sequence, output);
	}

	@Override
	public String getTaskTitle() {
		return "Save external stress sequence to '" + process_.getOutputFile().getName() + "'";
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
		updateTitle("Saving external stress sequence to '" + process_.getOutputFile().getName() + "'");

		// start process
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			process_.start(connection);
		}

		// return
		return null;
	}
}
