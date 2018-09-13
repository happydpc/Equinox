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
package equinox.task.automation;

import java.nio.file.Path;

import equinox.process.automation.ConvertXMLtoJSON;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.TemporaryFileCreatingTask;

/**
 * Class for convert XML to JSON file task.
 *
 * @author Murat Artim
 * @date 12 Sep 2018
 * @time 23:26:32
 */
public class ConvertXMLFiletoJSONFile extends TemporaryFileCreatingTask<Path> implements ShortRunningTask {

	/** Input and output files. */
	private final Path inputXmlFile, outputJsonFile;

	/**
	 * Creates convert JSON file to XML file task.
	 *
	 * @param inputXmlFile
	 *            Input XML file.
	 * @param outputJsonFile
	 *            Output JSON file.
	 */
	public ConvertXMLFiletoJSONFile(Path inputXmlFile, Path outputJsonFile) {
		this.inputXmlFile = inputXmlFile;
		this.outputJsonFile = outputJsonFile;
	}

	@Override
	public String getTaskTitle() {
		return "Convert XML file to JSON file";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Path call() throws Exception {
		return new ConvertXMLtoJSON(this, inputXmlFile, outputJsonFile).start(null);
	}
}