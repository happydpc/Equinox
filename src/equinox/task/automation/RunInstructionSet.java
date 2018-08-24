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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.data.InstructedTask;
import equinox.data.Pair;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.data.PilotPointSearchInput;
import equinox.dataServer.remote.data.SearchItem;
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.dataServer.remote.data.SpectrumSearchInput;
import equinox.plugin.FileType;
import equinox.process.automation.ReadGenerateStressSequenceInput;
import equinox.task.AddSTFFiles;
import equinox.task.AddSpectrum;
import equinox.task.AddStressSequence;
import equinox.task.AdvancedPilotPointSearch;
import equinox.task.AdvancedSpectrumSearch;
import equinox.task.DeleteFiles;
import equinox.task.DownloadPilotPoint;
import equinox.task.DownloadSpectrum;
import equinox.task.ExportSpectrum;
import equinox.task.GenerateStressSequence;
import equinox.task.GetSpectrumEditInfo;
import equinox.task.InternalEquinoxTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.SavableTask;
import equinox.task.SaveANA;
import equinox.task.SaveCVT;
import equinox.task.SaveConversionTable;
import equinox.task.SaveFLS;
import equinox.task.SaveSpectrum;
import equinox.task.SaveTXT;
import equinox.task.SaveTask;
import equinox.task.ShareSpectrum;
import equinox.task.ShareSpectrumFile;
import equinox.utility.Utility;
import equinox.utility.XMLUtilities;

/**
 * Class for run instruction set task.
 *
 * @author Murat Artim
 * @date 17 Aug 2018
 * @time 15:12:11
 */
@SuppressWarnings("unchecked")
public class RunInstructionSet extends InternalEquinoxTask<HashMap<String, InstructedTask>> implements LongRunningTask {

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
	protected HashMap<String, InstructedTask> call() throws Exception {

		// create list of tasks to be executed
		HashMap<String, InstructedTask> tasks = new HashMap<>();

		// read input file
		updateMessage("Reading input XML file...");

		// get root input element
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(inputFile.toUri().toString());
		Element equinoxInput = document.getRootElement();

		// get settings (if given)
		if (equinoxInput.getChild("settings") != null) {

			// get run mode (if given)
			if (equinoxInput.getChild("settings").getChild("runMode") != null) {
				runMode = equinoxInput.getChild("settings").getChild("runMode").getTextNormalize();
			}

			// get run silent (if given)
			if (equinoxInput.getChild("settings").getChild("runSilent") != null) {
				runSilent = Boolean.parseBoolean(equinoxInput.getChild("settings").getChild("runSilent").getTextNormalize());
			}
		}

		// download spectrum
		if (equinoxInput.getChild("downloadSpectrum") != null) {
			downloadSpectrum(equinoxInput, tasks);
		}

		// add spectrum
		if (equinoxInput.getChild("addSpectrum") != null) {
			addSpectrum(equinoxInput, tasks);
		}

		// save spectrum
		if (equinoxInput.getChild("saveSpectrum") != null) {
			saveSpectrum(equinoxInput, tasks);
		}

		// save spectrum file
		if (equinoxInput.getChild("saveSpectrumFile") != null) {
			saveSpectrumFile(equinoxInput, tasks);
		}

		// share spectrum
		if (equinoxInput.getChild("shareSpectrum") != null) {
			shareSpectrum(equinoxInput, tasks);
		}

		// share spectrum file
		if (equinoxInput.getChild("shareSpectrumFile") != null) {
			shareSpectrumFile(equinoxInput, tasks);
		}

		// export spectrum
		if (equinoxInput.getChild("exportSpectrum") != null) {
			exportSpectrum(equinoxInput, tasks);
		}

		// download STF
		if (equinoxInput.getChild("downloadStf") != null) {
			downloadStf(equinoxInput, tasks);
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

		// delete spectrum
		if (equinoxInput.getChild("deleteSpectrum") != null) {
			deleteSpectrum(equinoxInput, tasks);
		}

		// return tasks to be executed
		return tasks;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// execute tasks
		try {

			// get tasks
			HashMap<String, InstructedTask> tasks = get();

			// no tasks found
			if (tasks == null)
				return;

			// loop over tasks
			Iterator<Entry<String, InstructedTask>> iterator = tasks.entrySet().iterator();
			while (iterator.hasNext()) {

				// get task
				InstructedTask task = iterator.next().getValue();

				// embedded
				if (task.isEmbedded()) {
					continue;
				}

				// get task implementation
				InternalEquinoxTask<?> taskImpl = task.getTask();

				// parallel
				if (runMode.equals(PARALLEL)) {
					if (runSilent) {
						taskPanel_.getOwner().runTaskSilently(taskImpl, false);
					}
					else {
						taskPanel_.getOwner().runTaskInParallel(taskImpl);
					}
				}

				// sequential
				else if (runMode.equals(SEQUENTIAL)) {
					if (runSilent) {
						taskPanel_.getOwner().runTaskSilently(taskImpl, true);
					}
					else {
						taskPanel_.getOwner().runTaskSequentially(taskImpl);
					}
				}

				// save
				else if (runMode.equals(SAVE)) {
					if (runSilent) {
						taskPanel_.getOwner().runTaskSilently(new SaveTask((SavableTask) taskImpl, null), false);
					}
					else {
						taskPanel_.getOwner().runTaskInParallel(new SaveTask((SavableTask) taskImpl, null));
					}
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
	private void generateStressSequence(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating generate stress sequence tasks...");

		// loop over add stress sequence elements
		for (Element generateStressSequence : equinoxInput.getChildren("generateStressSequence")) {

			// parse elements
			String id = generateStressSequence.getChild("id").getTextNormalize();
			String stfId = generateStressSequence.getChild("stfId").getTextNormalize();
			Path xmlPath = Paths.get(generateStressSequence.getChild("xmlPath").getTextNormalize());

			// read input parameters
			GenerateStressSequenceInput input = new ReadGenerateStressSequenceInput(this, xmlPath).start(null);

			// create task
			GenerateStressSequence generateStressSequenceTask = new GenerateStressSequence(null, input);

			// add to parent task
			AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addAutomaticTask(id, generateStressSequenceTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(generateStressSequenceTask, true));
		}
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
	private void addStressSequence(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating add stress sequence tasks...");

		// get add stress sequence elements
		equinoxInput.getChildren("addStressSequence").forEach(Utility.exceptionThrowingLambda(addStressSequence -> {

			// get id
			String id = addStressSequence.getChild("id").getTextNormalize();

			// from SIGMA
			if (addStressSequence.getChild("sigmaPath") != null) {
				Path sigmaPath = Paths.get(addStressSequence.getChild("sigmaPath").getTextNormalize());
				tasks.put(id, new InstructedTask(new AddStressSequence(sigmaPath), false));
			}

			// from STH
			else {
				Path sthPath = Paths.get(addStressSequence.getChild("sthPath").getTextNormalize());
				Path flsPath = Paths.get(addStressSequence.getChild("flsPath").getTextNormalize());
				tasks.put(id, new InstructedTask(new AddStressSequence(sthPath, flsPath), false));
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
	private void addStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating add STF tasks...");

		// loop over add STF elements
		for (Element addStf : equinoxInput.getChildren("addStf")) {

			// get task and spectrum ids
			String id = addStf.getChild("id").getTextNormalize();
			String spectrumId = addStf.getChild("spectrumId").getTextNormalize();

			// STF path given
			if (addStf.getChild("stfPath") != null) {

				// create task
				Path stfPath = Paths.get(addStf.getChild("stfPath").getTextNormalize());
				AddSTFFiles addSTFFilesTask = new AddSTFFiles(Arrays.asList(stfPath.toFile()), null, null);

				// add to parent task
				AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
				parentTask.addAutomaticTask(id, addSTFFilesTask);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// put task to tasks
				tasks.put(id, new InstructedTask(addSTFFilesTask, true));
			}

			// search entry given
			else if (addStf.getChild("searchEntry") != null) {

				// create search input
				PilotPointSearchInput input = new PilotPointSearchInput();

				// loop over search entries
				for (Element searchEntry : addStf.getChildren("searchEntry")) {

					// get sub elements
					String attributeName = searchEntry.getChild("attributeName").getTextNormalize();
					String keyword = searchEntry.getChild("keyword").getTextNormalize();

					// get criteria
					int criteriaIndex = 0;
					if (searchEntry.getChild("criteria") != null) {
						String criteria = searchEntry.getChild("criteria").getTextNormalize();
						criteriaIndex = Arrays.asList("Contains", "Equals", "Starts with", "Ends with").indexOf(criteria);
					}

					// add input
					PilotPointInfoType infoType = PilotPointInfoType.valueOf(attributeName.toUpperCase());
					input.addInput(infoType, new SearchItem(keyword, criteriaIndex));
				}

				// set search engine settings
				XMLUtilities.setSearchEngineSettings(equinoxInput, input);

				// create tasks
				AdvancedPilotPointSearch searchPilotPointTask = new AdvancedPilotPointSearch(input);
				DownloadPilotPoint downloadPilotPointTask = new DownloadPilotPoint(null, null, null);

				// add download task to search
				searchPilotPointTask.addAutomaticTask(id, downloadPilotPointTask);
				searchPilotPointTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// add search to add spectrum
				AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
				parentTask.addAutomaticTask(id, searchPilotPointTask);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// add to tasks
				tasks.put(id, new InstructedTask(downloadPilotPointTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(searchPilotPointTask, true));
			}
		}
	}

	/**
	 * Creates download STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void downloadStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating download STF tasks...");

		// loop over add STF elements
		for (Element downloadStf : equinoxInput.getChildren("downloadStf")) {

			// get sub elements
			String id = downloadStf.getChild("id").getTextNormalize();
			Path outputFile = Paths.get(downloadStf.getChild("outputPath").getTextNormalize());

			// create search input
			PilotPointSearchInput input = new PilotPointSearchInput();

			// loop over search entries
			for (Element searchEntry : downloadStf.getChildren("searchEntry")) {

				// get sub elements
				String attributeName = searchEntry.getChild("attributeName").getTextNormalize();
				String keyword = searchEntry.getChild("keyword").getTextNormalize();

				// get criteria
				int criteriaIndex = 0;
				if (searchEntry.getChild("criteria") != null) {
					String criteria = searchEntry.getChild("criteria").getTextNormalize();
					criteriaIndex = Arrays.asList("Contains", "Equals", "Starts with", "Ends with").indexOf(criteria);
				}

				// add input
				PilotPointInfoType infoType = PilotPointInfoType.valueOf(attributeName.toUpperCase());
				input.addInput(infoType, new SearchItem(keyword, criteriaIndex));
			}

			// set search engine settings
			XMLUtilities.setSearchEngineSettings(equinoxInput, input);

			// create tasks
			AdvancedPilotPointSearch searchPilotPointTask = new AdvancedPilotPointSearch(input);
			DownloadPilotPoint downloadPilotPointTask = new DownloadPilotPoint(null, outputFile, null);
			searchPilotPointTask.addAutomaticTask(id, downloadPilotPointTask);
			searchPilotPointTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// add to tasks
			tasks.put(id, new InstructedTask(downloadPilotPointTask, true));
			tasks.put("ownerOf_" + id, new InstructedTask(searchPilotPointTask, false));
		}
	}

	/**
	 * Creates delete spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void deleteSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating delete spectrum tasks...");

		// loop over save spectrum elements
		for (Element saveSpectrum : equinoxInput.getChildren("deleteSpectrum")) {

			// create task
			String id = saveSpectrum.getChild("id").getTextNormalize();
			String spectrumId = saveSpectrum.getChild("spectrumId").getTextNormalize();
			DeleteFiles deleteFilesTask = new DeleteFiles(null);

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, deleteFilesTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(deleteFilesTask, true));
		}
	}

	/**
	 * Creates export spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void exportSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating export spectrum tasks...");

		// loop over export spectrum elements
		for (Element exportSpectrum : equinoxInput.getChildren("exportSpectrum")) {

			// create task
			String id = exportSpectrum.getChild("id").getTextNormalize();
			String spectrumId = exportSpectrum.getChild("spectrumId").getTextNormalize();
			Path outputPath = Paths.get(exportSpectrum.getChild("outputPath").getTextNormalize());
			GetSpectrumEditInfo getSpectrumInfoTask = new GetSpectrumEditInfo(null);
			ExportSpectrum exportSpectrumTask = new ExportSpectrum(null, null, outputPath.toFile());

			// set optional parameters
			if (exportSpectrum.getChild("deliveryReference") != null) {
				exportSpectrumTask.setDeliveryReference(exportSpectrum.getChild("deliveryReference").getTextNormalize());
			}
			if (exportSpectrum.getChild("description") != null) {
				exportSpectrumTask.setDescription(exportSpectrum.getChild("description").getTextNormalize());
			}

			// add to first task
			getSpectrumInfoTask.addAutomaticTask(id, exportSpectrumTask);
			getSpectrumInfoTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, getSpectrumInfoTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(exportSpectrumTask, true));
		}
	}

	/**
	 * Creates share spectrum file tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void shareSpectrumFile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating share spectrum file tasks...");

		// loop over share spectrum file elements
		for (Element shareSpectrumFile : equinoxInput.getChildren("shareSpectrumFile")) {

			// create task
			String id = shareSpectrumFile.getChild("id").getTextNormalize();
			String spectrumId = shareSpectrumFile.getChild("spectrumId").getTextNormalize();
			FileType fileType = FileType.getFileTypeForExtension("." + shareSpectrumFile.getChild("fileType").getTextNormalize());
			String recipient = shareSpectrumFile.getChild("recipient").getTextNormalize();
			ShareSpectrumFile shareSpectrumFileTask = new ShareSpectrumFile(null, fileType, Arrays.asList(recipient));

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, shareSpectrumFileTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(shareSpectrumFileTask, true));
		}
	}

	/**
	 * Creates share spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void shareSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating share spectrum tasks...");

		// loop over share spectrum elements
		for (Element shareSpectrum : equinoxInput.getChildren("shareSpectrum")) {

			// create task
			String id = shareSpectrum.getChild("id").getTextNormalize();
			String spectrumId = shareSpectrum.getChild("spectrumId").getTextNormalize();
			String recipient = shareSpectrum.getChild("recipient").getTextNormalize();
			ShareSpectrum shareSpectrumTask = new ShareSpectrum(null, Arrays.asList(recipient));

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, shareSpectrumTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(shareSpectrumTask, true));
		}
	}

	/**
	 * Creates save spectrum file tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveSpectrumFile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save spectrum file tasks...");

		// loop over save spectrum file elements
		for (Element saveSpectrumFile : equinoxInput.getChildren("saveSpectrumFile")) {

			// create task
			String id = saveSpectrumFile.getChild("id").getTextNormalize();
			String spectrumId = saveSpectrumFile.getChild("spectrumId").getTextNormalize();
			Path outputPath = Paths.get(saveSpectrumFile.getChild("outputPath").getTextNormalize());
			AutomaticTask<Spectrum> saveSpectrumFileTask = null;

			// get file type
			FileType fileType = FileType.getFileType(outputPath.toFile());

			// ANA
			if (fileType.equals(FileType.ANA)) {
				saveSpectrumFileTask = new SaveANA(null, outputPath.toFile(), FileType.ANA);
			}

			// CVT
			else if (fileType.equals(FileType.CVT)) {
				saveSpectrumFileTask = new SaveCVT(null, outputPath.toFile(), FileType.CVT);
			}

			// TXT
			else if (fileType.equals(FileType.TXT)) {
				saveSpectrumFileTask = new SaveTXT(null, outputPath.toFile(), FileType.TXT);
			}

			// FLS
			else if (fileType.equals(FileType.FLS)) {
				saveSpectrumFileTask = new SaveFLS(null, outputPath.toFile(), FileType.FLS);
			}

			// XLS
			else if (fileType.equals(FileType.XLS)) {
				saveSpectrumFileTask = new SaveConversionTable(null, outputPath.toFile(), FileType.XLS);
			}

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, saveSpectrumFileTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask((InternalEquinoxTask<?>) saveSpectrumFileTask, true));
		}
	}

	/**
	 * Creates save spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save spectrum tasks...");

		// loop over save spectrum elements
		for (Element saveSpectrum : equinoxInput.getChildren("saveSpectrum")) {

			// create task
			String id = saveSpectrum.getChild("id").getTextNormalize();
			String spectrumId = saveSpectrum.getChild("spectrumId").getTextNormalize();
			Path outputPath = Paths.get(saveSpectrum.getChild("outputPath").getTextNormalize());
			SaveSpectrum saveSpectrumTask = new SaveSpectrum(null, outputPath.toFile());

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, saveSpectrumTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(saveSpectrumTask, true));
		}
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
	private void addSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating add spectrum tasks...");

		// loop over add spectrum elements
		for (Element addSpectrum : equinoxInput.getChildren("addSpectrum")) {

			// get id
			String id = addSpectrum.getChild("id").getTextNormalize();

			// from SPEC bundle
			if (addSpectrum.getChild("specPath") != null) {
				Path specPath = Paths.get(addSpectrum.getChild("specPath").getTextNormalize());
				tasks.put(id, new InstructedTask(new AddSpectrum(specPath), false));
			}

			// from central database
			else if (addSpectrum.getChild("downloadId") != null) {

				// get download id
				String downloadId = addSpectrum.getChild("downloadId").getTextNormalize();

				// create task
				AddSpectrum addSpectrumTask = new AddSpectrum(null, null);

				// add to parent task
				AutomaticTaskOwner<Pair<Path, SpectrumInfo>> parentTask = (AutomaticTaskOwner<Pair<Path, SpectrumInfo>>) tasks.get(downloadId).getTask();
				parentTask.addAutomaticTask(id, addSpectrumTask);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// put task to tasks
				tasks.put(id, new InstructedTask(addSpectrumTask, true));
			}

			// from CDF set files
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
				tasks.put(id, new InstructedTask(new AddSpectrum(anaFile, txtFile, cvtFile, flsFile, conversionTable, sheet, null), false));
			}
		}
	}

	/**
	 * Creates download spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void downloadSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating download spectrum tasks...");

		// loop over download spectrum elements
		for (Element downloadSpectrum : equinoxInput.getChildren("downloadSpectrum")) {

			// get sub elements
			String id = downloadSpectrum.getChild("id").getTextNormalize();
			Path outputFile = Paths.get(downloadSpectrum.getChild("outputPath").getTextNormalize());

			// create search input
			SpectrumSearchInput input = new SpectrumSearchInput();

			// loop over search entries
			for (Element searchEntry : downloadSpectrum.getChildren("searchEntry")) {

				// get sub elements
				String attributeName = searchEntry.getChild("attributeName").getTextNormalize();
				String keyword = searchEntry.getChild("keyword").getTextNormalize();

				// get criteria
				int criteriaIndex = 0;
				if (searchEntry.getChild("criteria") != null) {
					String criteria = searchEntry.getChild("criteria").getTextNormalize();
					criteriaIndex = Arrays.asList("Contains", "Equals", "Starts with", "Ends with").indexOf(criteria);
				}

				// add input
				SpectrumInfoType infoType = SpectrumInfoType.valueOf(attributeName.toUpperCase());
				input.addInput(infoType, new SearchItem(keyword, criteriaIndex));
			}

			// set search engine settings
			XMLUtilities.setSearchEngineSettings(equinoxInput, input);

			// create tasks
			AdvancedSpectrumSearch searchSpectrumTask = new AdvancedSpectrumSearch(input);
			DownloadSpectrum downloadSpectrumTask = new DownloadSpectrum(null, outputFile, false);
			searchSpectrumTask.addAutomaticTask(id, downloadSpectrumTask);
			searchSpectrumTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// add to tasks
			tasks.put(id, new InstructedTask(downloadSpectrumTask, true));
			tasks.put("ownerOf_" + id, new InstructedTask(searchSpectrumTask, false));
		}
	}
}