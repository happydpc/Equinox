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

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;
import org.json.XML;

import equinox.plugin.FileType;
import equinox.process.EquinoxProcess;
import equinox.task.TemporaryFileCreatingTask;

/**
 * Class for convert JSON file to XML file process.
 *
 * @author Murat Artim
 * @date 12 Sep 2018
 * @time 10:08:05
 */
public class ConvertJSONtoXML implements EquinoxProcess<Path> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task;

	/** Input JSON file. */
	private final Path inputJsonFile;

	/**
	 * Creates convert JSON file to XML file process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputJsonFile
	 *            Input JSON file.
	 */
	public ConvertJSONtoXML(TemporaryFileCreatingTask<?> task, Path inputJsonFile) {
		this.task = task;
		this.inputJsonFile = inputJsonFile;
	}

	@Override
	public Path start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// create path to output XML file
		Path outputXmlFile = task.getWorkingDirectory().resolve(FileType.getNameWithoutExtension(inputJsonFile) + ".xml");

		// read input file and create JSON object
		task.updateMessage("Reading input JSON file...");
		String jsonString = new String(Files.readAllBytes(inputJsonFile));
		JSONObject jsonObject = new JSONObject(jsonString);

		// task cancelled
		if (task.isCancelled())
			return null;

		// write JSON object to XML file
		task.updateMessage("Writing to XML file...");
		try (FileWriter fileWriter = new FileWriter(outputXmlFile.toFile())) {
			fileWriter.write(XML.toString(jsonObject));
		}

		// return output XML file
		return outputXmlFile;
	}
}