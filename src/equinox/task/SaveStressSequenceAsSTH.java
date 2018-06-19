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
import equinox.data.fileType.StressSequence;
import equinox.process.SaveSTH;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinoxServer.remote.utility.Permission;

/**
 * Class for save spectrum as STH task.
 *
 * @author Murat Artim
 * @date Jan 7, 2014
 * @time 2:13:14 PM
 */
public class SaveStressSequenceAsSTH extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Process of this task. */
	private final SaveSTH process_;

	/**
	 * Creates save spectrum as STH task.
	 *
	 * @param stressSequence
	 *            Stress sequence to save.
	 * @param output
	 *            Output file.
	 */
	public SaveStressSequenceAsSTH(StressSequence stressSequence, File output) {
		process_ = new SaveSTH(this, stressSequence, output);
	}

	@Override
	public String getTaskTitle() {
		return "Save stress sequence to '" + process_.getOutputFile().getName() + "'";
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
		updateTitle("Saving stress sequence to '" + process_.getOutputFile().getName() + "'");

		// start process
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			process_.start(connection);
		}

		// return
		return null;
	}
}
