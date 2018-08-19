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

import equinox.data.input.GenerateStressSequenceInput;
import equinox.process.ReadGenerateStressSequenceInput;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;

/**
 * Class for run instruction set task.
 *
 * @author Murat Artim
 * @date 17 Aug 2018
 * @time 15:12:11
 */
public class RunInstructionSet extends InternalEquinoxTask<HashMap<String, InternalEquinoxTask<?>>> implements LongRunningTask {

	/** Run mode constant. */
	public static final String PARALLEL = "parallel", SEQUENTIAL = "sequential", SAVE = "save";

	/** True if tasks should be executed in parallel mode. */
	private String runMode = PARALLEL;

	/** True if all notifications should be suppressed. */
	private boolean runSilent = false;

	/** Input XML file. */
	private final Path inputFile;

	/**
	 * Creates submit instruction set task.
	 *
	 * @param inputFile
	 *            Input XML file.
	 */
	public RunInstructionSet(Path inputFile) {
		this.inputFile = inputFile;
	}

	@Override
	public String getTaskTitle() {
		return "Run instruction set";
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

		// get run mode (if given)
		if (equinoxInput.getChild("settings") != null) {
			if (equinoxInput.getChild("settings").getChild("runMode") != null) {
				runMode = equinoxInput.getChild("settings").getChild("runMode").getTextNormalize();
			}
		}

		// get run silent (if given)
		if (equinoxInput.getChild("settings") != null) {
			if (equinoxInput.getChild("settings").getChild("runSilent") != null) {
				runSilent = Boolean.parseBoolean(equinoxInput.getChild("settings").getChild("runSilent").getTextNormalize());
			}
		}

		// add spectrum
		if (equinoxInput.getChild("addSpectrum") != null) {
			addSpectrum(equinoxInput, tasks);
		}

		// add STF
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
				if (runSilent) {
					get().values().forEach(task -> taskPanel_.getOwner().runTaskSilently(task, false));
				}
				else {
					get().values().forEach(task -> taskPanel_.getOwner().runTaskInParallel(task));
				}
			}

			// sequential
			else if (runMode.equals(SEQUENTIAL)) {
				if (runSilent) {
					get().values().forEach(task -> taskPanel_.getOwner().runTaskSilently(task, true));
				}
				else {
					get().values().forEach(task -> taskPanel_.getOwner().runTaskSequentially(task));
				}
			}

			// save
			else if (runMode.equals(SAVE)) {
				if (runSilent) {
					get().values().forEach(task -> taskPanel_.getOwner().runTaskSilently(new SaveTask((SavableTask) task, null), false));
				}
				else {
					get().values().forEach(task -> taskPanel_.getOwner().runTaskInParallel(new SaveTask((SavableTask) task, null)));
				}
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Creates generate stress sequence tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void generateStressSequence(Element equinoxInput, HashMap<String, InternalEquinoxTask<?>> tasks) throws Exception {

		// update info
		updateMessage("Creating generate stress sequence tasks...");

		// get add stress sequence elements
		equinoxInput.getChildren("generateStressSequence").forEach(Utility.exceptionThrowingLambda(generateStressSequence -> {

			// parse elements
			String id = generateStressSequence.getChild("id").getTextNormalize();
			String stfId = generateStressSequence.getChild("stfId").getTextNormalize();
			Path inputPath = Paths.get(generateStressSequence.getChild("inputPath").getTextNormalize());

			// read input parameters
			GenerateStressSequenceInput input = new ReadGenerateStressSequenceInput(this, inputPath).start(null);

			// create task
			GenerateStressSequence task = new GenerateStressSequence(null, input);

			// add to parent task
			((AddSTFFiles) tasks.get(stfId)).addAutomaticTask(id, task);
		}));
	}

	/**
	 * Creates add stress sequence tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addStressSequence(Element equinoxInput, HashMap<String, InternalEquinoxTask<?>> tasks) throws Exception {

		// update info
		updateMessage("Creating add stress sequence tasks...");

		// get add stress sequence elements
		equinoxInput.getChildren("addStressSequence").forEach(Utility.exceptionThrowingLambda(addStressSequence -> {

			// get id
			String id = addStressSequence.getChild("id").getTextNormalize();

			// from SIGMA
			if (addStressSequence.getChild("sigmaPath") != null) {
				Path sigmaPath = Paths.get(addStressSequence.getChild("sigmaPath").getTextNormalize());
				tasks.put(id, new AddStressSequence(sigmaPath));
			}

			// from STH
			else {
				Path sthPath = Paths.get(addStressSequence.getChild("sthPath").getTextNormalize());
				Path flsPath = Paths.get(addStressSequence.getChild("flsPath").getTextNormalize());
				tasks.put(id, new AddStressSequence(sthPath, flsPath));
			}
		}));
	}

	/**
	 * Creates add STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addStf(Element equinoxInput, HashMap<String, InternalEquinoxTask<?>> tasks) throws Exception {

		// update info
		updateMessage("Creating add STF tasks...");

		// get add STF elements
		equinoxInput.getChildren("addStf").forEach(Utility.exceptionThrowingLambda(addStf -> {

			// create task
			String id = addStf.getChild("id").getTextNormalize();
			String spectrumId = addStf.getChild("spectrumId").getTextNormalize();
			Path stfPath = Paths.get(addStf.getChild("stfPath").getTextNormalize());
			AddSTFFiles addSTFFiles = new AddSTFFiles(Arrays.asList(stfPath.toFile()), null, null);

			// add to parent task
			((AddSpectrum) tasks.get(spectrumId)).addAutomaticTask(id, addSTFFiles);
		}));
	}

	/**
	 * Creates add spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addSpectrum(Element equinoxInput, HashMap<String, InternalEquinoxTask<?>> tasks) throws Exception {

		// update info
		updateMessage("Creating add spectrum tasks...");

		// get add spectrum elements
		equinoxInput.getChildren("addSpectrum").forEach(Utility.exceptionThrowingLambda(addSpectrum -> {

			// get id
			String id = addSpectrum.getChild("id").getTextNormalize();

			// SPEC bundle
			if (addSpectrum.getChild("specPath") != null) {
				Path specPath = Paths.get(addSpectrum.getChild("specPath").getTextNormalize());
				tasks.put(id, new AddSpectrum(specPath));
			}

			// CDF set files
			else {
				Path anaFile = Paths.get(addSpectrum.getChild("anaPath").getTextNormalize());
				Path cvtFile = Paths.get(addSpectrum.getChild("cvtPath").getTextNormalize());
				Path flsFile = Paths.get(addSpectrum.getChild("flsPath").getTextNormalize());
				Path conversionTable = Paths.get(addSpectrum.getChild("xlsPath").getTextNormalize());
				String sheet = addSpectrum.getChild("convSheet").getTextNormalize();
				Path txtFile = null;
				if (addSpectrum.getChild("txtPath") != null) {
					txtFile = Paths.get(addSpectrum.getChild("txtPath").getTextNormalize());
				}
				tasks.put(id, new AddSpectrum(anaFile, txtFile, cvtFile, flsFile, conversionTable, sheet, null));
			}
		}));
	}
}