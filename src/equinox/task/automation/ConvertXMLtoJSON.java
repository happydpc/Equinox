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

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.json.XML;

/**
 * TODO
 *
 * @author Murat Artim
 * @date 11 Sep 2018
 * @time 21:43:58
 */
public class ConvertXMLtoJSON {
	public static void main(String[] args) throws Exception {

		// convertXMLToJSON("/Users/aurora/Temporary/test/equinoxInput_test.xml", "/Users/aurora/Temporary/test/equinoxInput_test.json");

		convertJSONToXML("/Users/aurora/Temporary/test/equinoxInput_test.json", "/Users/aurora/Temporary/test/equinoxInput_test2.xml");

	}

	private static void convertJSONToXML(String jsonPath, String xmlPath) throws Exception {

		// FIXME

		String jsonStr = new String(Files.readAllBytes(Paths.get(jsonPath)));
		JSONObject json = new JSONObject(jsonStr);

		try (FileWriter fileWriter = new FileWriter(Paths.get(xmlPath).toFile())) {
			fileWriter.write(XML.toString(json));
		}
	}

	private static void convertXMLToJSON(String xmlPath, String jsonPath) throws Exception {
		int PRETTY_PRINT_INDENT_FACTOR = 4;

		String xmlString = new String(Files.readAllBytes(Paths.get(xmlPath)));
		JSONObject xmlJSONObj = XML.toJSONObject(xmlString);

		try (FileWriter fileWriter = new FileWriter(Paths.get(jsonPath).toFile())) {
			fileWriter.write(xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR));
		}
	}
}
