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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for add batch tasks task.
 *
 * @author Murat Artim
 * @date 17 Aug 2018
 * @time 15:12:11
 */
public class AddBatchTasks extends InternalEquinoxTask<HashMap<String, InternalEquinoxTask<?>>> implements LongRunningTask {

	/** Run mode constant. */
	private static final String PARALLEL = "parallel", SEQUENTIAL = "sequential", SAVE = "save";

	/** True if tasks should be executed in parallel mode. */
	private String runMode;

	/** Input XML file. */
	private final Path inputFile;

	/**
	 * Creates add batch tasks task.
	 *
	 * @param inputFile
	 *            Input XML file.
	 */
	public AddBatchTasks(Path inputFile) {
		this.inputFile = inputFile;
	}

	@Override
	public String getTaskTitle() {
		return "Add batch tasks";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected HashMap<String, InternalEquinoxTask<?>> call() throws Exception {

		// create list of tasks to be executed
		HashMap<String, InternalEquinoxTask<?>> tasks = new HashMap<>();

		// read input file
		updateMessage("Reading input XML file...");

		// get root input element
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(inputFile.toUri().toString());
		Element equinoxInput = document.getRootElement();

		// get run mode
		runMode = equinoxInput.getChild("settings").getChild("runMode").getValue();

		// add spectra
		if (equinoxInput.getChild("addSpectrum") != null) {
			addSpectrum(equinoxInput, tasks);
		}

		// add STFs
		if (equinoxInput.getChild("addStf") != null) {
			addStf(equinoxInput, tasks);
		}

		// add stress sequence
		if (equinoxInput.getChild("addStressSequence") != null) {
			addStressSequence(equinoxInput, tasks);
		}

		// generate stress sequence
		if (equinoxInput.getChild("generateStressSequence") != null) {
			generateStressSequence(equinoxInput, tasks);
		}

		// TODO

		// return tasks to be executed
		return tasks;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// execute tasks
		try {

			// parallel
			if (runMode.equals(PARALLEL)) {
				get().values().forEach(task -> taskPanel_.getOwner().runTaskInParallel(task));
			}

			// sequential
			else if (runMode.equals(SEQUENTIAL)) {
				get().values().forEach(task -> taskPanel_.getOwner().runTaskSequentially(task));
			}

			// save
			else if (runMode.equals(SAVE)) {
				get().values().forEach(task -> taskPanel_.getOwner().runTaskInParallel(new SaveTask((SavableTask) task, null)));
			}

		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 *
	 * @param equinoxInput
	 * @param tasks
	 * @throws Exception
	 */
	private void generateStressSequence(Element equinoxInput, HashMap<String, InternalEquinoxTask<?>> tasks) throws Exception {

		// update info
		updateMessage("Creating 'generateStressSequence' tasks...");

		// get add stress sequence elements
		equinoxInput.getChildren("generateStressSequence").forEach(generateStressSequence -> {

			// get id
			String id = generateStressSequence.getChild("id").getValue();
			String stfId = generateStressSequence.getChild("stfId").getValue();
			Path inputPath = Paths.get(generateStressSequence.getChild("inputPath").getValue().trim());

			GenerateStressSequence task = new GenerateStressSequence(null, input)
		});
	}

	/**
	 *
	 * @param equinoxInput
	 * @param tasks
	 * @throws Exception
	 */
	private void addStressSequence(Element equinoxInput, HashMap<String, InternalEquinoxTask<?>> tasks) throws Exception {

		// update info
		updateMessage("Creating 'addStressSequence' tasks...");

		// get add stress sequence elements
		equinoxInput.getChildren("addStressSequence").forEach(addStressSequence -> {

			// get id
			String id = addStressSequence.getChild("id").getValue();

			// from sigma
			if (addStressSequence.getChild("sigmaPath") != null) {
				Path sigmaPath = Paths.get(addStressSequence.getChild("sigmaPath").getValue().trim());
				tasks.put(id, new AddStressSequence(sigmaPath));
			}

			// from sth
			else {
				Path sthPath = Paths.get(addStressSequence.getChild("sthPath").getValue().trim());
				Path flsPath = Paths.get(addStressSequence.getChild("flsPath").getValue().trim());
				tasks.put(id, new AddStressSequence(sthPath, flsPath));
			}
		});
	}

	/**
	 *
	 * @param equinoxInput
	 * @param tasks
	 * @throws Exception
	 */
	private void addStf(Element equinoxInput, HashMap<String, InternalEquinoxTask<?>> tasks) throws Exception {

		// update info
		updateMessage("Creating 'addStf' tasks...");

		// get add stf elements
		equinoxInput.getChildren("addStf").forEach(addStf -> {

			// create task
			String id = addStf.getChild("id").getValue();
			String spectrumId = addStf.getChild("spectrumId").getValue();
			Path stfPath = Paths.get(addStf.getChild("stfPath").getValue().trim());
			AddSTFFiles addSTFFiles = new AddSTFFiles(Arrays.asList(stfPath.toFile()), null, null);

			// add to parent task
			((AddSpectrum) tasks.get(spectrumId)).addAutomaticTask(id, addSTFFiles);
		});
	}

	/**
	 *
	 * @param equinoxInput
	 * @param tasks
	 * @throws Exception
	 */
	private void addSpectrum(Element equinoxInput, HashMap<String, InternalEquinoxTask<?>> tasks) throws Exception {

		// update info
		updateMessage("Creating 'addSpectrum' tasks...");

		// get add spectrum elements
		equinoxInput.getChildren("addSpectrum").forEach(addSpectrum -> {

			// get id
			String id = addSpectrum.getChild("id").getValue();

			// spec bundle
			if (addSpectrum.getChild("specPath") != null) {
				Path specPath = Paths.get(addSpectrum.getChild("specPath").getValue().trim());
				tasks.put(id, new AddSpectrum(specPath));
			}

			// CDF set files
			else {
				Path anaFile = Paths.get(addSpectrum.getChild("anaPath").getValue().trim());
				Path cvtFile = Paths.get(addSpectrum.getChild("cvtPath").getValue().trim());
				Path flsFile = Paths.get(addSpectrum.getChild("flsPath").getValue().trim());
				Path conversionTable = Paths.get(addSpectrum.getChild("xlsPath").getValue().trim());
				String sheet = addSpectrum.getChild("convSheet").getValue().trim();
				Path txtFile = null;
				if (addSpectrum.getChild("txtPath") != null) {
					if (addSpectrum.getChild("txtPath").getValue() != null) {
						if (!addSpectrum.getChild("txtPath").getValue().trim().isEmpty()) {
							txtFile = Paths.get(addSpectrum.getChild("txtPath").getValue().trim());
						}
					}
				}
				tasks.put(id, new AddSpectrum(anaFile, txtFile, cvtFile, flsFile, conversionTable, sheet, null));
			}
		});
	}
}