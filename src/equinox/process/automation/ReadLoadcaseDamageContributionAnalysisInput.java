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
package equinox.process.automation;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import equinox.data.input.LoadcaseDamageContributionInput;
import equinox.process.EquinoxProcess;
import equinox.task.TemporaryFileCreatingTask;

/**
 * Class for read loadcase damage contribution analysis input process.
 * 
 * @author Murat Artim
 * @date 13 Sep 2018
 * @time 14:06:24
 */
public class ReadLoadcaseDamageContributionAnalysisInput implements EquinoxProcess<LoadcaseDamageContributionInput> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task;

	/** Input file. */
	private Path inputFile;

	/**
	 * Creates read generate stress sequence input process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input XML/JSON file.
	 */
	public ReadLoadcaseDamageContributionAnalysisInput(TemporaryFileCreatingTask<?> task, Path inputFile) {
		this.task = task;
		this.inputFile = inputFile;
	}

	@Override
	public LoadcaseDamageContributionInput start(Connection connection, PreparedStatement... preparedStatements) throws Exception {
		// FIXME
		return null;
	}
}