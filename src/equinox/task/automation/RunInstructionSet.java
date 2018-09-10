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

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.Equinox;
import equinox.controller.InputPanel;
import equinox.controller.SearchEngineSettingsPanel;
import equinox.data.AnalysisEngine;
import equinox.data.InstructedTask;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.MissionParameter;
import equinox.data.Pair;
import equinox.data.Settings;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.data.input.EquivalentStressComparisonInput;
import equinox.data.input.EquivalentStressInput;
import equinox.data.input.EquivalentStressRatioComparisonInput;
import equinox.data.input.ExternalFlightPlotInput;
import equinox.data.input.ExternalStatisticsInput;
import equinox.data.input.ExternalStatisticsInput.ExternalStatistic;
import equinox.data.input.FlightComparisonInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.data.input.LevelCrossingInput;
import equinox.data.input.LifeFactorComparisonInput;
import equinox.data.input.StressSequenceComparisonInput;
import equinox.data.input.StressSequenceComparisonInput.ComparisonCriteria;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.data.PilotPointSearchInput;
import equinox.dataServer.remote.data.SearchItem;
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.dataServer.remote.data.SpectrumSearchInput;
import equinox.plugin.FileType;
import equinox.process.automation.ReadEquivalentStressAnalysisInput;
import equinox.process.automation.ReadGenerateStressSequenceInput;
import equinox.task.AddSTFFiles;
import equinox.task.AddSpectrum;
import equinox.task.AddStressSequence;
import equinox.task.AdvancedPilotPointSearch;
import equinox.task.AdvancedSpectrumSearch;
import equinox.task.AssignMissionParameters;
import equinox.task.CompareEquivalentStresses;
import equinox.task.CompareEquivalentStressesWithMissionParameters;
import equinox.task.CompareFlights;
import equinox.task.CompareStressSequences;
import equinox.task.CreateDummySTFFile;
import equinox.task.DownloadPilotPoint;
import equinox.task.DownloadSpectrum;
import equinox.task.EquivalentStressAnalysis;
import equinox.task.ExportSTF;
import equinox.task.ExportSpectrum;
import equinox.task.GenerateExternalStatistics;
import equinox.task.GenerateLFsWithMissionParameters;
import equinox.task.GenerateLifeFactors;
import equinox.task.GenerateStressRatios;
import equinox.task.GenerateStressRatiosWithMissionParameters;
import equinox.task.GenerateStressSequence;
import equinox.task.GetSTFInfo2;
import equinox.task.GetSTFInfo3;
import equinox.task.GetSpectrumEditInfo;
import equinox.task.GetTypicalFlight;
import equinox.task.InternalEquinoxTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.PlotExternalTypicalFlights;
import equinox.task.PlotLevelCrossing;
import equinox.task.SavableTask;
import equinox.task.SaveANA;
import equinox.task.SaveCVT;
import equinox.task.SaveCategoryDataset;
import equinox.task.SaveChart;
import equinox.task.SaveConversionTable;
import equinox.task.SaveEquivalentStressPlotToFile;
import equinox.task.SaveEquivalentStressRatios;
import equinox.task.SaveEquivalentStresses;
import equinox.task.SaveExternalStressSequenceAsSIGMA;
import equinox.task.SaveExternalStressSequenceAsSTH;
import equinox.task.SaveFLS;
import equinox.task.SaveLifeFactors;
import equinox.task.SaveMissionParameterPlot;
import equinox.task.SaveMissionProfile;
import equinox.task.SaveOutputFile;
import equinox.task.SaveRainflow;
import equinox.task.SaveSTF;
import equinox.task.SaveSpectrum;
import equinox.task.SaveStressSequenceAsSIGMA;
import equinox.task.SaveStressSequenceAsSTH;
import equinox.task.SaveStressSequenceInfo;
import equinox.task.SaveStressSequencePlotToFile;
import equinox.task.SaveTXT;
import equinox.task.SaveTask;
import equinox.task.SaveXYDataset;
import equinox.task.SaveXYSeriesCollection;
import equinox.task.SelectAllExternalFlights;
import equinox.task.SelectExternalFlight;
import equinox.task.SetSTFMission;
import equinox.task.ShareGeneratedItem;
import equinox.task.ShareSTF;
import equinox.task.ShareSpectrum;
import equinox.task.ShareSpectrumFile;
import equinox.task.UploadPilotPoints;
import equinox.task.UploadSpectra;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;

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
		}

		// download spectrum
		if (equinoxInput.getChild("downloadSpectrum") != null) {
			downloadSpectrum(equinoxInput, tasks);
		}

		// add spectrum
		if (equinoxInput.getChild("addSpectrum") != null) {
			addSpectrum(equinoxInput, tasks);
		}

		// assign mission parameters to spectrum
		if (equinoxInput.getChild("assignMissionParametersToSpectrum") != null) {
			assignMissionParametersToSpectrum(equinoxInput, tasks);
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

		// upload spectrum
		if (equinoxInput.getChild("uploadSpectrum") != null) {
			uploadSpectrum(equinoxInput, tasks);
		}

		// download STF
		if (equinoxInput.getChild("downloadStf") != null) {
			downloadStf(equinoxInput, tasks);
		}

		// add STF
		if (equinoxInput.getChild("addStf") != null) {
			addStf(equinoxInput, tasks);
		}

		// override fatigue mission
		if (equinoxInput.getChild("overrideFatigueMission") != null) {
			overrideFatigueMission(equinoxInput, tasks);
		}

		// assign mission parameters to STF
		if (equinoxInput.getChild("assignMissionParametersToStf") != null) {
			assignMissionParametersToStf(equinoxInput, tasks);
		}

		// save STF
		if (equinoxInput.getChild("saveStf") != null) {
			saveStf(equinoxInput, tasks);
		}

		// share STF
		if (equinoxInput.getChild("shareStf") != null) {
			shareStf(equinoxInput, tasks);
		}

		// export STF
		if (equinoxInput.getChild("exportStf") != null) {
			exportStf(equinoxInput, tasks);
		}

		// upload STF
		if (equinoxInput.getChild("uploadStf") != null) {
			uploadStf(equinoxInput, tasks);
		}

		// add headless stress sequence
		if (equinoxInput.getChild("addHeadlessStressSequence") != null) {
			addHeadlessStressSequence(equinoxInput, tasks);
		}

		// generate stress sequence
		if (equinoxInput.getChild("generateStressSequence") != null) {
			generateStressSequence(equinoxInput, tasks);
		}

		// assign mission parameters to headless stress sequence
		if (equinoxInput.getChild("assignMissionParametersToStressSequence") != null) {
			assignMissionParametersToStressSequence(equinoxInput, tasks);
		}

		// edit stress sequence info
		if (equinoxInput.getChild("editStressSequenceInfo") != null) {
			editStressSequenceInfo(equinoxInput, tasks);
		}

		// save stress sequence
		if (equinoxInput.getChild("saveStressSequence") != null) {
			saveStressSequence(equinoxInput, tasks);
		}

		// plot mission profile
		if (equinoxInput.getChild("plotMissionProfile") != null) {
			plotMissionProfile(equinoxInput, tasks);
		}

		// save mission profile info
		if (equinoxInput.getChild("saveMissionProfileInfo") != null) {
			saveMissionProfileInfo(equinoxInput, tasks);
		}

		// plot typical flight
		if (equinoxInput.getChild("plotTypicalFlight") != null) {
			plotTypicalFlight(equinoxInput, tasks, "plotTypicalFlight");
		}

		// plot typical flight statistics
		if (equinoxInput.getChild("plotTypicalFlightStatistics") != null) {
			plotTypicalFlight(equinoxInput, tasks, "plotTypicalFlightStatistics");
		}

		// equivalent stress analysis
		if (equinoxInput.getChild("equivalentStressAnalysis") != null) {
			equivalentStressAnalysis(equinoxInput, tasks);
		}

		// plot level crossing
		if (equinoxInput.getChild("plotLevelCrossing") != null) {
			plotLevelCrossing(equinoxInput, tasks);
		}

		// plot rainflow histogram
		if (equinoxInput.getChild("plotRainflowHistogram") != null) {
			plotRainflowHistogram(equinoxInput, tasks);
		}

		// save rainflow cycle info
		if (equinoxInput.getChild("saveRainflowCycleInfo") != null) {
			saveRainflowCycleInfo(equinoxInput, tasks);
		}

		// save analysis output file
		if (equinoxInput.getChild("saveAnalysisOutputFile") != null) {
			saveAnalysisOutputFile(equinoxInput, tasks);
		}

		// plot stress sequence comparison
		if (equinoxInput.getChild("plotStressSequenceComparison") != null) {
			plotStressSequenceComparison(equinoxInput, tasks);
		}

		// plot level crossing comparison
		if (equinoxInput.getChild("plotLevelCrossingComparison") != null) {
			plotLevelCrossingComparison(equinoxInput, tasks);
		}

		// plot typical flight comparison
		if (equinoxInput.getChild("plotTypicalFlightComparison") != null) {
			plotTypicalFlightComparison(equinoxInput, tasks);
		}

		// plot equivalent stress comparison
		if (equinoxInput.getChild("plotEquivalentStressComparison") != null) {
			plotEquivalentStressComparison(equinoxInput, tasks);
		}

		// plot life factors
		if (equinoxInput.getChild("plotLifeFactors") != null) {
			plotLifeFactors(equinoxInput, tasks);
		}

		// plot equivalent stress ratios
		if (equinoxInput.getChild("plotEquivalentStressRatios") != null) {
			plotEquivalentStressRatios(equinoxInput, tasks);
		}

		// save equivalent stresses
		if (equinoxInput.getChild("saveEquivalentStresses") != null) {
			saveEquivalentStresses(equinoxInput, tasks);
		}

		// save life factors
		if (equinoxInput.getChild("saveLifeFactors") != null) {
			saveLifeFactors(equinoxInput, tasks);
		}

		// save equivalent stress ratios
		if (equinoxInput.getChild("saveEquivalentStressRatios") != null) {
			saveEquivalentStressRatios(equinoxInput, tasks);
		}

		// TODO

		// share file
		if (equinoxInput.getChild("shareFile") != null) {
			shareFile(equinoxInput, tasks);
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
					taskPanel_.getOwner().runTaskInParallel(taskImpl);
				}

				// sequential
				else if (runMode.equals(SEQUENTIAL)) {
					taskPanel_.getOwner().runTaskSequentially(taskImpl);
				}

				// save
				else if (runMode.equals(SAVE)) {
					taskPanel_.getOwner().runTaskInParallel(new SaveTask((SavableTask) taskImpl, null));
				}
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Creates edit stress sequence info tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void editStressSequenceInfo(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating edit stress sequence info tasks...");

		// loop over edit stress sequence info elements
		for (Element editStressSequenceInfo : equinoxInput.getChildren("editStressSequenceInfo")) {

			// create task
			String id = editStressSequenceInfo.getChild("id").getTextNormalize();
			String headlessStressSequenceId = editStressSequenceInfo.getChild("headlessStressSequenceId").getTextNormalize();

			// initialize info array
			String[] info = new String[3];

			// aircraft program
			if (editStressSequenceInfo.getChild("aircraftProgram") != null) {
				info[SaveStressSequenceInfo.PROGRAM] = editStressSequenceInfo.getChildTextNormalize("aircraftProgram");
			}

			// aircraft section
			if (editStressSequenceInfo.getChild("aircraftSection") != null) {
				info[SaveStressSequenceInfo.SECTION] = editStressSequenceInfo.getChildTextNormalize("aircraftSection");
			}

			// fatigue mission
			if (editStressSequenceInfo.getChild("fatigueMission") != null) {
				info[SaveStressSequenceInfo.MISSION] = editStressSequenceInfo.getChildTextNormalize("fatigueMission");
			}

			// create task
			SaveStressSequenceInfo task = new SaveStressSequenceInfo(null, info);

			// add to parent task
			ParameterizedTaskOwner<ExternalStressSequence> parentTask = (ParameterizedTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
			parentTask.addParameterizedTask(id, task);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates assign mission parameters to headless stress sequence tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void assignMissionParametersToStressSequence(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating assign mission parameters to headless stress sequence tasks...");

		// loop over assign mission parameters to headless stress sequence elements
		for (Element assignMissionParametersToStressSequence : equinoxInput.getChildren("assignMissionParametersToStressSequence")) {

			// create task
			String id = assignMissionParametersToStressSequence.getChild("id").getTextNormalize();
			String headlessStressSequenceId = assignMissionParametersToStressSequence.getChild("headlessStressSequenceId").getTextNormalize();
			ArrayList<MissionParameter> parameters = new ArrayList<>();

			// loop over mission parameter elements
			for (Element missionParameter : assignMissionParametersToStressSequence.getChildren("missionParameter")) {

				// get parameter name and value
				String name = missionParameter.getChildTextNormalize("name");
				double value = Double.parseDouble(missionParameter.getChildTextNormalize("value"));

				// create and add mission parameter
				parameters.add(new MissionParameter(name, value));
			}

			// create task
			AssignMissionParameters<ExternalStressSequence> assignMissionParametersTask = new AssignMissionParameters<>(null, parameters.toArray(new MissionParameter[parameters.size()]));

			// add to parent task
			ParameterizedTaskOwner<ExternalStressSequence> parentTask = (ParameterizedTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
			parentTask.addParameterizedTask(id, assignMissionParametersTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(assignMissionParametersTask, true));
		}
	}

	/**
	 * Creates save equivalent stress ratios tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveEquivalentStressRatios(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save equivalent stress ratios tasks...");

		// loop over save equivalent stress ratios elements
		for (Element saveEquivalentStressRatios : equinoxInput.getChildren("saveEquivalentStressRatios")) {

			// get id and output path
			String id = saveEquivalentStressRatios.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveEquivalentStressRatios.getChildTextNormalize("outputPath"));
			String basisMission = saveEquivalentStressRatios.getChildTextNormalize("basisMission");

			// options
			BooleanProperty[] options = new BooleanProperty[15];
			for (int i = 0; i < options.length; i++) {
				options[i] = new SimpleBooleanProperty(false);
			}
			options[SaveEquivalentStressRatios.STRESS_RATIO].set(true);
			options[SaveEquivalentStressRatios.PP_NAME].set(true);
			options[SaveEquivalentStressRatios.MISSION].set(true);
			if (saveEquivalentStressRatios.getChild("options") != null) {

				// get element
				Element optionsElement = saveEquivalentStressRatios.getChild("options");

				// set options
				if (optionsElement.getChild("equivalentStressRatio") != null) {
					options[SaveEquivalentStressRatios.STRESS_RATIO] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("equivalentStressRatio")));
				}
				if (optionsElement.getChild("materialName") != null) {
					options[SaveEquivalentStressRatios.MAT_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialName")));
				}
				if (optionsElement.getChild("materialData") != null) {
					options[SaveEquivalentStressRatios.MAT_DATA] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialData")));
				}
				if (optionsElement.getChild("pilotPointName") != null) {
					options[SaveEquivalentStressRatios.PP_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("pilotPointName")));
				}
				if (optionsElement.getChild("elementId") != null) {
					options[SaveEquivalentStressRatios.EID] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("elementId")));
				}
				if (optionsElement.getChild("stressSequenceName") != null) {
					options[SaveEquivalentStressRatios.SEQ_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("stressSequenceName")));
				}
				if (optionsElement.getChild("spectrumName") != null) {
					options[SaveEquivalentStressRatios.SPEC_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumName")));
				}
				if (optionsElement.getChild("aircraftProgram") != null) {
					options[SaveEquivalentStressRatios.PROGRAM] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftProgram")));
				}
				if (optionsElement.getChild("aircraftSection") != null) {
					options[SaveEquivalentStressRatios.SECTION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftSection")));
				}
				if (optionsElement.getChild("fatigueMission") != null) {
					options[SaveEquivalentStressRatios.MISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("fatigueMission")));
				}
				if (optionsElement.getChild("spectrumValidity") != null) {
					options[SaveEquivalentStressRatios.VALIDITY] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumValidity")));
				}
				if (optionsElement.getChild("maximumStress") != null) {
					options[SaveEquivalentStressRatios.MAX_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("maximumStress")));
				}
				if (optionsElement.getChild("minimumStress") != null) {
					options[SaveEquivalentStressRatios.MIN_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("minimumStress")));
				}
				if (optionsElement.getChild("rRatio") != null) {
					options[SaveEquivalentStressRatios.R_RATIO] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("rRatio")));
				}
				if (optionsElement.getChild("omissionLevel") != null) {
					options[SaveEquivalentStressRatios.OMISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("omissionLevel")));
				}
			}

			// create task
			SaveEquivalentStressRatios task = new SaveEquivalentStressRatios(null, options, outputPath.toFile(), basisMission);

			// set input threshold
			List<Element> equivalentStressIds = saveEquivalentStressRatios.getChildren("equivalentStressId");
			task.setInputThreshold(equivalentStressIds.size());

			// loop over stress ids
			for (Element equivalentStressIdElement : equivalentStressIds) {

				// get stress id
				String equivalentStressId = equivalentStressIdElement.getTextNormalize();

				// connect to parent task
				ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addParameterizedTask(id, task);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates save life factors tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveLifeFactors(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save life factors tasks...");

		// loop over save life factors elements
		for (Element saveLifeFactors : equinoxInput.getChildren("saveLifeFactors")) {

			// get id and output path
			String id = saveLifeFactors.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveLifeFactors.getChildTextNormalize("outputPath"));
			String basisMission = saveLifeFactors.getChildTextNormalize("basisMission");

			// options
			BooleanProperty[] options = new BooleanProperty[15];
			for (int i = 0; i < options.length; i++) {
				options[i] = new SimpleBooleanProperty(false);
			}
			options[SaveLifeFactors.LIFE_FACTOR].set(true);
			options[SaveLifeFactors.PP_NAME].set(true);
			options[SaveLifeFactors.MISSION].set(true);
			if (saveLifeFactors.getChild("options") != null) {

				// get element
				Element optionsElement = saveLifeFactors.getChild("options");

				// set options
				if (optionsElement.getChild("lifeFactor") != null) {
					options[SaveLifeFactors.LIFE_FACTOR] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("lifeFactor")));
				}
				if (optionsElement.getChild("materialName") != null) {
					options[SaveLifeFactors.MAT_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialName")));
				}
				if (optionsElement.getChild("materialData") != null) {
					options[SaveLifeFactors.MAT_DATA] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialData")));
				}
				if (optionsElement.getChild("pilotPointName") != null) {
					options[SaveLifeFactors.PP_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("pilotPointName")));
				}
				if (optionsElement.getChild("elementId") != null) {
					options[SaveLifeFactors.EID] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("elementId")));
				}
				if (optionsElement.getChild("stressSequenceName") != null) {
					options[SaveLifeFactors.SEQ_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("stressSequenceName")));
				}
				if (optionsElement.getChild("spectrumName") != null) {
					options[SaveLifeFactors.SPEC_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumName")));
				}
				if (optionsElement.getChild("aircraftProgram") != null) {
					options[SaveLifeFactors.PROGRAM] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftProgram")));
				}
				if (optionsElement.getChild("aircraftSection") != null) {
					options[SaveLifeFactors.SECTION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftSection")));
				}
				if (optionsElement.getChild("fatigueMission") != null) {
					options[SaveLifeFactors.MISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("fatigueMission")));
				}
				if (optionsElement.getChild("spectrumValidity") != null) {
					options[SaveLifeFactors.VALIDITY] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumValidity")));
				}
				if (optionsElement.getChild("maximumStress") != null) {
					options[SaveLifeFactors.MAX_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("maximumStress")));
				}
				if (optionsElement.getChild("minimumStress") != null) {
					options[SaveLifeFactors.MIN_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("minimumStress")));
				}
				if (optionsElement.getChild("rRatio") != null) {
					options[SaveLifeFactors.R_RATIO] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("rRatio")));
				}
				if (optionsElement.getChild("omissionLevel") != null) {
					options[SaveLifeFactors.OMISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("omissionLevel")));
				}
			}

			// create task
			SaveLifeFactors task = new SaveLifeFactors(null, options, outputPath.toFile(), basisMission);

			// set input threshold
			List<Element> equivalentStressIds = saveLifeFactors.getChildren("equivalentStressId");
			task.setInputThreshold(equivalentStressIds.size());

			// loop over stress ids
			for (Element equivalentStressIdElement : equivalentStressIds) {

				// get stress id
				String equivalentStressId = equivalentStressIdElement.getTextNormalize();

				// connect to parent task
				ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addParameterizedTask(id, task);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates save equivalent stresses tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveEquivalentStresses(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save equivalent stresses tasks...");

		// loop over save equivalent stresses elements
		for (Element saveEquivalentStresses : equinoxInput.getChildren("saveEquivalentStresses")) {

			// get id and output path
			String id = saveEquivalentStresses.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveEquivalentStresses.getChildTextNormalize("outputPath"));

			// options
			BooleanProperty[] options = new BooleanProperty[15];
			for (int i = 0; i < options.length; i++) {
				options[i] = new SimpleBooleanProperty(false);
			}
			options[SaveEquivalentStresses.EQUIVALENT_STRESS] = new SimpleBooleanProperty(true);
			options[SaveEquivalentStresses.MAT_NAME] = new SimpleBooleanProperty(true);
			options[SaveEquivalentStresses.PP_NAME] = new SimpleBooleanProperty(true);
			options[SaveEquivalentStresses.MISSION] = new SimpleBooleanProperty(true);
			if (saveEquivalentStresses.getChild("options") != null) {

				// get element
				Element optionsElement = saveEquivalentStresses.getChild("options");

				// set options
				if (optionsElement.getChild("equivalentStress") != null) {
					options[SaveEquivalentStresses.EQUIVALENT_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("equivalentStress")));
				}
				if (optionsElement.getChild("materialName") != null) {
					options[SaveEquivalentStresses.MAT_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialName")));
				}
				if (optionsElement.getChild("materialData") != null) {
					options[SaveEquivalentStresses.MAT_DATA] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialData")));
				}
				if (optionsElement.getChild("pilotPointName") != null) {
					options[SaveEquivalentStresses.PP_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("pilotPointName")));
				}
				if (optionsElement.getChild("elementId") != null) {
					options[SaveEquivalentStresses.EID] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("elementId")));
				}
				if (optionsElement.getChild("stressSequenceName") != null) {
					options[SaveEquivalentStresses.SEQ_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("stressSequenceName")));
				}
				if (optionsElement.getChild("spectrumName") != null) {
					options[SaveEquivalentStresses.SPEC_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumName")));
				}
				if (optionsElement.getChild("aircraftProgram") != null) {
					options[SaveEquivalentStresses.PROGRAM] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftProgram")));
				}
				if (optionsElement.getChild("aircraftSection") != null) {
					options[SaveEquivalentStresses.SECTION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftSection")));
				}
				if (optionsElement.getChild("fatigueMission") != null) {
					options[SaveEquivalentStresses.MISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("fatigueMission")));
				}
				if (optionsElement.getChild("spectrumValidity") != null) {
					options[SaveEquivalentStresses.VALIDITY] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumValidity")));
				}
				if (optionsElement.getChild("maximumStress") != null) {
					options[SaveEquivalentStresses.MAX_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("maximumStress")));
				}
				if (optionsElement.getChild("minimumStress") != null) {
					options[SaveEquivalentStresses.MIN_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("minimumStress")));
				}
				if (optionsElement.getChild("rRatio") != null) {
					options[SaveEquivalentStresses.R_RATIO] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("rRatio")));
				}
				if (optionsElement.getChild("omissionLevel") != null) {
					options[SaveEquivalentStresses.OMISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("omissionLevel")));
				}
			}

			// create task
			SaveEquivalentStresses task = new SaveEquivalentStresses(null, options, outputPath.toFile());

			// set input threshold
			List<Element> equivalentStressIds = saveEquivalentStresses.getChildren("equivalentStressId");
			task.setInputThreshold(equivalentStressIds.size());

			// loop over stress ids
			for (Element equivalentStressIdElement : equivalentStressIds) {

				// get stress id
				String equivalentStressId = equivalentStressIdElement.getTextNormalize();

				// connect to parent task
				ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addParameterizedTask(id, task);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates plot equivalent stress ratios tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotEquivalentStressRatios(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot equivalent stress ratios tasks...");

		// loop over plot equivalent stress ratios elements
		for (Element plotEquivalentStressRatios : equinoxInput.getChildren("plotEquivalentStressRatios")) {

			// get id and output path
			String id = plotEquivalentStressRatios.getChildTextNormalize("id");
			String basisMission = plotEquivalentStressRatios.getChildTextNormalize("basisMission");
			Path outputPath = Paths.get(plotEquivalentStressRatios.getChildTextNormalize("outputPath"));

			// create input
			EquivalentStressRatioComparisonInput input = new EquivalentStressRatioComparisonInput();
			input.setBasisMission(basisMission);

			// options
			if (plotEquivalentStressRatios.getChild("options") != null) {

				// get element
				Element options = plotEquivalentStressRatios.getChild("options");

				// include basis mission
				if (options.getChild("includeBasisMission") != null) {
					input.setIncludeBasisMission(Boolean.parseBoolean(options.getChildTextNormalize("includeBasisMission")));
				}

				// show data labels
				if (options.getChild("showDataLabels") != null) {
					input.setLabelDisplay(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
				}
			}

			// set series naming
			if (plotEquivalentStressRatios.getChild("seriesNaming") != null) {
				Element seriesNaming = plotEquivalentStressRatios.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
				}
				if (seriesNaming.getChild("includeStressSequenceName") != null) {
					input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
				}
				if (seriesNaming.getChild("includeMaterialName") != null) {
					input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
				}
				if (seriesNaming.getChild("includeOmissionLevel") != null) {
					input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
				}
			}

			// mission parameter given
			if (plotEquivalentStressRatios.getChild("missionParameter") != null) {

				// get mission parameter name
				input.setMissionParameterName(plotEquivalentStressRatios.getChildTextNormalize("missionParameter"));

				// create tasks
				SaveMissionParameterPlot saveTask = new SaveMissionParameterPlot(null, null, outputPath);
				GenerateStressRatiosWithMissionParameters compareTask = new GenerateStressRatiosWithMissionParameters(input, null);
				compareTask.addParameterizedTask(id, saveTask);
				compareTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// set input threshold
				List<Element> equivalentStressIds = plotEquivalentStressRatios.getChildren("equivalentStressId");
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addParameterizedTask(id, compareTask);
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}

			// no mission parameter given
			else {

				// create tasks
				SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
				GenerateStressRatios compareTask = new GenerateStressRatios(input);
				compareTask.addParameterizedTask(id, saveDatasetTask);
				compareTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// set input threshold
				List<Element> equivalentStressIds = plotEquivalentStressRatios.getChildren("equivalentStressId");
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addParameterizedTask(id, compareTask);
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}
		}
	}

	/**
	 * Creates plot life factors tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLifeFactors(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot life factors tasks...");

		// loop over plot life factors elements
		for (Element plotLifeFactors : equinoxInput.getChildren("plotLifeFactors")) {

			// get id and output path
			String id = plotLifeFactors.getChildTextNormalize("id");
			String basisMission = plotLifeFactors.getChildTextNormalize("basisMission");
			Path outputPath = Paths.get(plotLifeFactors.getChildTextNormalize("outputPath"));

			// create input
			LifeFactorComparisonInput input = new LifeFactorComparisonInput();
			input.setBasisMission(basisMission);

			// options
			if (plotLifeFactors.getChild("options") != null) {

				// get element
				Element options = plotLifeFactors.getChild("options");

				// include basis mission
				if (options.getChild("includeBasisMission") != null) {
					input.setIncludeBasisMission(Boolean.parseBoolean(options.getChildTextNormalize("includeBasisMission")));
				}

				// show data labels
				if (options.getChild("showDataLabels") != null) {
					input.setLabelDisplay(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
				}
			}

			// set series naming
			if (plotLifeFactors.getChild("seriesNaming") != null) {
				Element seriesNaming = plotLifeFactors.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
				}
				if (seriesNaming.getChild("includeStressSequenceName") != null) {
					input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
				}
				if (seriesNaming.getChild("includeMaterialName") != null) {
					input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
				}
				if (seriesNaming.getChild("includeOmissionLevel") != null) {
					input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
				}
			}

			// mission parameter given
			if (plotLifeFactors.getChild("missionParameter") != null) {

				// get mission parameter name
				input.setMissionParameterName(plotLifeFactors.getChildTextNormalize("missionParameter"));

				// create tasks
				SaveMissionParameterPlot saveTask = new SaveMissionParameterPlot(null, null, outputPath);
				GenerateLFsWithMissionParameters compareTask = new GenerateLFsWithMissionParameters(input, null);
				compareTask.addParameterizedTask(id, saveTask);
				compareTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// set input threshold
				List<Element> equivalentStressIds = plotLifeFactors.getChildren("equivalentStressId");
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addParameterizedTask(id, compareTask);
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}

			// no mission parameter given
			else {

				// create tasks
				SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
				GenerateLifeFactors compareTask = new GenerateLifeFactors(input);
				compareTask.addParameterizedTask(id, saveDatasetTask);
				compareTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// set input threshold
				List<Element> equivalentStressIds = plotLifeFactors.getChildren("equivalentStressId");
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addParameterizedTask(id, compareTask);
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}
		}
	}

	/**
	 * Creates plot equivalent stress comparison tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotEquivalentStressComparison(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot equivalent stress comparison tasks...");

		// loop over plot equivalent stress comparison elements
		for (Element plotEquivalentStressComparison : equinoxInput.getChildren("plotEquivalentStressComparison")) {

			// get id and output path
			String id = plotEquivalentStressComparison.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotEquivalentStressComparison.getChildTextNormalize("outputPath"));

			// create comparison input
			EquivalentStressComparisonInput input = new EquivalentStressComparisonInput();

			// label display
			if (plotEquivalentStressComparison.getChild("showDataLabels") != null) {
				input.setLabelDisplay(Boolean.parseBoolean(plotEquivalentStressComparison.getChildTextNormalize("showDataLabels")));
			}

			// set series naming
			if (plotEquivalentStressComparison.getChild("seriesNaming") != null) {
				Element seriesNaming = plotEquivalentStressComparison.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
				}
				if (seriesNaming.getChild("includeStressSequenceName") != null) {
					input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
				}
				if (seriesNaming.getChild("includeMaterialName") != null) {
					input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
				}
				if (seriesNaming.getChild("includeOmissionLevel") != null) {
					input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
				}
			}

			// mission parameter given
			if (plotEquivalentStressComparison.getChild("missionParameter") != null) {

				// get mission parameter name
				input.setMissionParameterName(plotEquivalentStressComparison.getChildTextNormalize("missionParameter"));

				// create tasks
				SaveMissionParameterPlot saveTask = new SaveMissionParameterPlot(null, null, outputPath);
				CompareEquivalentStressesWithMissionParameters compareTask = new CompareEquivalentStressesWithMissionParameters(input, null);
				compareTask.addParameterizedTask(id, saveTask);
				compareTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// set input threshold
				List<Element> equivalentStressIds = plotEquivalentStressComparison.getChildren("equivalentStressId");
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addParameterizedTask(id, compareTask);
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}

			// no mission parameter given
			else {

				// create tasks
				SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
				CompareEquivalentStresses compareTask = new CompareEquivalentStresses(input);
				compareTask.addParameterizedTask(id, saveDatasetTask);
				compareTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// set input threshold
				List<Element> equivalentStressIds = plotEquivalentStressComparison.getChildren("equivalentStressId");
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addParameterizedTask(id, compareTask);
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}
		}
	}

	/**
	 * Creates plot typical flight comparison tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotTypicalFlightComparison(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating typical flight comparison tasks...");

		// loop over typical flight comparison elements
		for (Element plotTypicalFlightComparison : equinoxInput.getChildren("plotTypicalFlightComparison")) {

			// get id and output path
			String id = plotTypicalFlightComparison.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotTypicalFlightComparison.getChildTextNormalize("outputPath"));

			// create comparison input
			FlightComparisonInput input = new FlightComparisonInput(null);
			input.setShowMarkers(false);

			// stress components
			if (plotTypicalFlightComparison.getChild("stressComponents") != null) {
				Element stressComponents = plotTypicalFlightComparison.getChild("stressComponents");
				boolean[] plotComponentOptions = { true, true, true, true };
				if (stressComponents.getChild("plotIncrements") != null) {
					plotComponentOptions[FlightComparisonInput.INCREMENT_STRESS_COMP] = Boolean.parseBoolean(stressComponents.getChild("plotIncrements").getTextNormalize());
				}
				if (stressComponents.getChild("plotDp") != null) {
					plotComponentOptions[FlightComparisonInput.DP_STRESS_COMP] = Boolean.parseBoolean(stressComponents.getChild("plotDp").getTextNormalize());
				}
				if (stressComponents.getChild("plotDt") != null) {
					plotComponentOptions[FlightComparisonInput.DT_STRESS_COMP] = Boolean.parseBoolean(stressComponents.getChild("plotDt").getTextNormalize());
				}
				if (stressComponents.getChild("plot1g") != null) {
					plotComponentOptions[FlightComparisonInput.ONE_G_STRESS_COMP] = Boolean.parseBoolean(stressComponents.getChild("plot1g").getTextNormalize());
				}
				input.setPlotComponentOptions(plotComponentOptions, false);
			}

			// set series naming
			if (plotTypicalFlightComparison.getChild("seriesNaming") != null) {
				Element seriesNaming = plotTypicalFlightComparison.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
				}
				if (seriesNaming.getChild("includeStressSequenceName") != null) {
					input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
				}
				if (seriesNaming.getChild("includeTypicalFlightName") != null) {
					input.setIncludeFlightName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeTypicalFlightName")));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
				}
			}

			// create tasks
			SaveChart saveTask = new SaveChart(null, outputPath);
			CompareFlights compareTask = new CompareFlights(input);
			compareTask.addParameterizedTask(id, saveTask);
			compareTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// set input threshold
			List<Element> typicalFlights = plotTypicalFlightComparison.getChildren("typicalFlight");
			compareTask.setInputThreshold(typicalFlights.size());

			// loop over typical flights
			for (Element typicalFlight : typicalFlights) {

				// get sequence id and flight name
				String stressSequenceId = typicalFlight.getChildText("stressSequenceId");
				String flightName = typicalFlight.getChildText("typicalFlightName");

				// create get flight task
				GetTypicalFlight getFlightTask = new GetTypicalFlight(flightName);
				getFlightTask.addParameterizedTask(Integer.toString(compareTask.hashCode()), compareTask);
				getFlightTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// connect to parent task
				ParameterizedTaskOwner<StressSequence> parentTask = (ParameterizedTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
				parentTask.addParameterizedTask(Integer.toString(getFlightTask.hashCode()), getFlightTask);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(saveTask, true));
			tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
		}
	}

	/**
	 * Creates plot level crossing comparison tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLevelCrossingComparison(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating level crossing comparison tasks...");

		// loop over plot level crossing comparison elements
		for (Element plotLevelCrossingComparison : equinoxInput.getChildren("plotLevelCrossingComparison")) {

			// get id and output path
			String id = plotLevelCrossingComparison.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotLevelCrossingComparison.getChildTextNormalize("outputPath"));

			// create comparison input
			LevelCrossingInput input = new LevelCrossingInput(true, null);

			// set series naming
			if (plotLevelCrossingComparison.getChild("seriesNaming") != null) {
				Element seriesNaming = plotLevelCrossingComparison.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
				}
				if (seriesNaming.getChild("includeStressSequenceName") != null) {
					input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
				}
				if (seriesNaming.getChild("includeMaterialName") != null) {
					input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
				}
				if (seriesNaming.getChild("includeOmissionLevel") != null) {
					input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
				}
			}

			// create tasks
			SaveXYSeriesCollection saveDatasetTask = new SaveXYSeriesCollection(null, outputPath);
			PlotLevelCrossing compareTask = new PlotLevelCrossing(input);
			compareTask.addParameterizedTask(id, saveDatasetTask);
			compareTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// set input threshold
			List<Element> equivalentStressIds = plotLevelCrossingComparison.getChildren("equivalentStressId");
			compareTask.setInputThreshold(equivalentStressIds.size());

			// loop over equivalent stress ids
			for (Element equivalentStressIdElement : equivalentStressIds) {

				// get stress id
				String equivalentStressId = equivalentStressIdElement.getTextNormalize();

				// connect to parent task
				ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addParameterizedTask(id, compareTask);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(saveDatasetTask, true));
			tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
		}
	}

	/**
	 * Creates plot stress sequence comparison tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotStressSequenceComparison(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot stress sequence comparison tasks...");

		// loop over plot stress sequence comparison elements
		for (Element plotStressSequenceComparison : equinoxInput.getChildren("plotStressSequenceComparison")) {

			// get id and output path
			String id = plotStressSequenceComparison.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotStressSequenceComparison.getChildTextNormalize("outputPath"));

			// create comparison input
			StressSequenceComparisonInput comparisonInput = new StressSequenceComparisonInput();
			comparisonInput.setCriteria(ComparisonCriteria.valueOf(plotStressSequenceComparison.getChildTextNormalize("comparisonCriteria")));

			// set options
			if (plotStressSequenceComparison.getChild("options") != null) {
				Element options = plotStressSequenceComparison.getChild("options");
				if (options.getChild("resultsOrder") != null) {
					comparisonInput.setOrder(options.getChildTextNormalize("resultsOrder").equals("descending"));
				}
				if (options.getChild("showDataLabels") != null) {
					comparisonInput.setLabelDisplay(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
				}
			}

			// set series naming
			if (plotStressSequenceComparison.getChild("seriesNaming") != null) {
				Element seriesNaming = plotStressSequenceComparison.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					comparisonInput.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					comparisonInput.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					comparisonInput.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
				}
				if (seriesNaming.getChild("includeStressSequenceName") != null) {
					comparisonInput.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					comparisonInput.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					comparisonInput.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					comparisonInput.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
				}
			}

			// create tasks
			SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
			CompareStressSequences compareTask = new CompareStressSequences(comparisonInput);
			compareTask.addParameterizedTask(id, saveDatasetTask);
			compareTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// set input threshold
			List<Element> stressSequenceIds = plotStressSequenceComparison.getChildren("stressSequenceId");
			compareTask.setInputThreshold(stressSequenceIds.size());

			// loop over sequence ids
			for (Element stressSequenceIdElement : stressSequenceIds) {

				// get stress sequence id
				String stressSequenceId = stressSequenceIdElement.getTextNormalize();

				// connect to parent task
				ParameterizedTaskOwner<StressSequence> parentTask = (ParameterizedTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
				parentTask.addParameterizedTask(id, compareTask);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(saveDatasetTask, true));
			tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
		}
	}

	/**
	 * Creates share file tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void shareFile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating share file tasks...");

		// loop over save stress sequence elements
		for (Element shareFile : equinoxInput.getChildren("shareFile")) {

			// create task
			String id = shareFile.getChild("id").getTextNormalize();
			String fileId = shareFile.getChild("fileId").getTextNormalize();
			String recipient = shareFile.getChild("recipient").getTextNormalize();

			// create tasks
			ShareGeneratedItem shareTask = new ShareGeneratedItem(null, Arrays.asList(recipient));

			// add to parent task
			ParameterizedTaskOwner<Path> parentTask = (ParameterizedTaskOwner<Path>) tasks.get(fileId).getTask();
			parentTask.addParameterizedTask(id, shareTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(shareTask, true));
		}
	}

	/**
	 * Creates save analysis output file tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveAnalysisOutputFile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save analysis output file tasks...");

		// loop over save analysis output file elements
		for (Element saveAnalysisOutputFile : equinoxInput.getChildren("saveAnalysisOutputFile")) {

			// get inputs
			String id = saveAnalysisOutputFile.getChild("id").getTextNormalize();
			String equivalentStressId = saveAnalysisOutputFile.getChild("equivalentStressId").getTextNormalize();
			Path outputPath = Paths.get(saveAnalysisOutputFile.getChild("outputPath").getTextNormalize());

			// create task
			SaveOutputFile task = new SaveOutputFile(null, outputPath);

			// connect to parent task
			ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
			parentTask.addParameterizedTask(id, task);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates save rainflow cycle info tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveRainflowCycleInfo(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating rainflow cycle info tasks...");

		// loop over rainflow cycle info elements
		for (Element saveRainflowCycleInfo : equinoxInput.getChildren("saveRainflowCycleInfo")) {

			// get inputs
			String id = saveRainflowCycleInfo.getChild("id").getTextNormalize();
			String equivalentStressId = saveRainflowCycleInfo.getChild("equivalentStressId").getTextNormalize();
			Path outputPath = Paths.get(saveRainflowCycleInfo.getChild("outputPath").getTextNormalize());

			// create task
			SaveRainflow task = new SaveRainflow(null, outputPath.toFile());

			// connect to parent task
			ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
			parentTask.addParameterizedTask(id, task);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates plot rainflow histogram tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotRainflowHistogram(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot rainflow histogram tasks...");

		// loop over plot rainflow histogram elements
		for (Element plotRainflowHistogram : equinoxInput.getChildren("plotRainflowHistogram")) {

			// get inputs
			String id = plotRainflowHistogram.getChild("id").getTextNormalize();
			String equivalentStressId = plotRainflowHistogram.getChild("equivalentStressId").getTextNormalize();
			Path outputPath = Paths.get(plotRainflowHistogram.getChild("outputPath").getTextNormalize());

			// create task
			SaveEquivalentStressPlotToFile task = new SaveEquivalentStressPlotToFile(null, PilotPointImageType.RAINFLOW_HISTOGRAM, outputPath);

			// connect to parent task
			ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
			parentTask.addParameterizedTask(id, task);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates plot level crossing tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLevelCrossing(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot level crossing tasks...");

		// loop over plot level crossing elements
		for (Element plotLevelCrossing : equinoxInput.getChildren("plotLevelCrossing")) {

			// get inputs
			String id = plotLevelCrossing.getChild("id").getTextNormalize();
			Path outputPath = Paths.get(plotLevelCrossing.getChild("outputPath").getTextNormalize());

			// equivalent stress id
			if (plotLevelCrossing.getChild("equivalentStressId") != null) {

				// create task
				SaveEquivalentStressPlotToFile task = new SaveEquivalentStressPlotToFile(null, PilotPointImageType.LEVEL_CROSSING, outputPath);

				// connect to parent task
				String equivalentStressId = plotLevelCrossing.getChild("equivalentStressId").getTextNormalize();
				ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addParameterizedTask(id, task);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}

			// headless equivalent stress id
			else if (plotLevelCrossing.getChild("headlessEquivalentStressId") != null) {

				// FIXME create task
			}
		}
	}

	/**
	 * Creates equivalent stress analysis tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void equivalentStressAnalysis(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating equivalent stress analysis tasks...");

		// get analysis engine settings
		Settings settings = taskPanel_.getOwner().getOwner().getSettings();
		AnalysisEngine engine = (AnalysisEngine) settings.getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) settings.getValue(Settings.ISAMI_VERSION);
		IsamiSubVersion isamiSubVersion = (IsamiSubVersion) settings.getValue(Settings.ISAMI_SUB_VERSION);
		boolean applyCompression = (boolean) settings.getValue(Settings.APPLY_COMPRESSION);

		// input mapping
		HashMap<Path, EquivalentStressInput> inputs = new HashMap<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// loop over equivalent stress analysis elements
			for (Element equivalentStressAnalysis : equinoxInput.getChildren("equivalentStressAnalysis")) {

				// get inputs
				String id = equivalentStressAnalysis.getChild("id").getTextNormalize();
				Path xmlPath = Paths.get(equivalentStressAnalysis.getChild("xmlPath").getTextNormalize());

				// get input parameters
				EquivalentStressInput input = inputs.get(xmlPath);
				if (input == null) {
					input = new ReadEquivalentStressAnalysisInput(this, xmlPath, isamiVersion).start(connection);
					inputs.put(xmlPath, input);
				}

				// create task
				EquivalentStressAnalysis task = new EquivalentStressAnalysis(null, input, engine);
				task.setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression);

				// stress sequence
				if (equivalentStressAnalysis.getChild("stressSequenceId") != null) {
					String stressSequenceId = equivalentStressAnalysis.getChildTextNormalize("stressSequenceId");
					ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(stressSequenceId).getTask();
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
					parentTask.addParameterizedTask(id, task);
				}

				// headless stress sequence
				else if (equivalentStressAnalysis.getChild("headlessStressSequenceId") != null) {
					String headlessStressSequenceId = equivalentStressAnalysis.getChildTextNormalize("headlessStressSequenceId");
					ParameterizedTaskOwner<SpectrumItem> parentTask = (ParameterizedTaskOwner<SpectrumItem>) tasks.get(headlessStressSequenceId).getTask();
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
					parentTask.addParameterizedTask(id, task);
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}
		}
	}

	/**
	 * Creates plot typical flight tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @param elementName
	 *            Element name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotTypicalFlight(Element equinoxInput, HashMap<String, InstructedTask> tasks, String elementName) throws Exception {

		// update info
		updateMessage("Creating plot typical flight tasks...");

		// loop over plot typical flight elements
		for (Element plotTypicalFlight : equinoxInput.getChildren(elementName)) {

			// get inputs
			String id = plotTypicalFlight.getChild("id").getTextNormalize();
			Path outputPath = Paths.get(plotTypicalFlight.getChild("outputPath").getTextNormalize());
			String plotTypeName = plotTypicalFlight.getChild("plotType").getTextNormalize();

			// get plot type
			PilotPointImageType plotType = null;
			for (PilotPointImageType type : PilotPointImageType.values()) {
				if (type.toString().equals(plotTypeName)) {
					plotType = type;
					break;
				}
			}

			// stress sequence
			if (plotTypicalFlight.getChild("stressSequenceId") != null) {

				// get stress sequence id
				String stressSequenceId = plotTypicalFlight.getChild("stressSequenceId").getTextNormalize();

				// create task
				SaveStressSequencePlotToFile task = new SaveStressSequencePlotToFile(null, plotType, outputPath);

				// connect to parent task
				ParameterizedTaskOwner<StressSequence> parentTask = (ParameterizedTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
				parentTask.addParameterizedTask(id, task);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}

			// headless stress sequence
			else if (plotTypicalFlight.getChild("headlessStressSequenceId") != null) {

				// get headless stress sequence id
				String headlessStressSequenceId = plotTypicalFlight.getChild("headlessStressSequenceId").getTextNormalize();

				// plot typical flights
				if (elementName.equals("plotTypicalFlight")) {

					// get typical flight type
					int criteria = -1;
					if (plotType.equals(PilotPointImageType.LONGEST_FLIGHT)) {
						criteria = SelectExternalFlight.LONGEST_FLIGHT;
					}
					else if (plotType.equals(PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE)) {
						criteria = SelectExternalFlight.MAX_VALIDITY;
					}
					else if (plotType.equals(PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS)) {
						criteria = SelectExternalFlight.MAX_PEAK;
					}

					// create tasks
					SaveXYDataset saveTask = new SaveXYDataset(null, outputPath);
					PlotExternalTypicalFlights plotTask = new PlotExternalTypicalFlights(new ExternalFlightPlotInput());
					plotTask.addParameterizedTask(id, saveTask);
					plotTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
					plotTask.setInputThreshold(1);
					SelectExternalFlight selectTask = new SelectExternalFlight(null, criteria);
					selectTask.addParameterizedTask(id, plotTask);
					selectTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

					// connect to parent task
					ParameterizedTaskOwner<ExternalStressSequence> parentTask = (ParameterizedTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
					parentTask.addParameterizedTask(id, selectTask);
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

					// put task to tasks
					tasks.put(id, new InstructedTask(saveTask, true));
				}

				// plot typical flight statistics
				else if (elementName.equals("plotTypicalFlightStatistics")) {

					// create input
					ExternalStatisticsInput input = new ExternalStatisticsInput();

					// set statistic type
					if (plotType.equals(PilotPointImageType.NUMBER_OF_PEAKS)) {
						input.setStatistic(ExternalStatistic.NUM_PEAKS);
					}
					else if (plotType.equals(PilotPointImageType.FLIGHT_OCCURRENCE)) {
						input.setStatistic(ExternalStatistic.FLIGHT_OCCURRENCE);
					}

					// options
					if (plotTypicalFlight.getChild("options") != null) {

						// get element
						Element options = plotTypicalFlight.getChild("options");

						// data display
						if (options.getChild("showDataLabels") != null) {
							input.setLabelDisplay(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
						}

						// max flights
						if (options.getChild("maxFlights") != null) {
							input.setLimit(Integer.parseInt(options.getChildTextNormalize("maxFlights")));
						}

						// results order
						if (options.getChild("resultsOrder") != null) {
							input.setOrder(options.getChildTextNormalize("resultsOrder").equals("descending"));
						}
					}

					// create tasks
					SaveCategoryDataset saveTask = new SaveCategoryDataset(null, outputPath);
					GenerateExternalStatistics plotTask = new GenerateExternalStatistics(input);
					plotTask.addParameterizedTask(id, saveTask);
					plotTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));
					SelectAllExternalFlights selectTask = new SelectAllExternalFlights(null);
					selectTask.addParameterizedTask(id, plotTask);
					selectTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

					// connect to parent task
					ParameterizedTaskOwner<ExternalStressSequence> parentTask = (ParameterizedTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
					parentTask.addParameterizedTask(id, selectTask);
					parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

					// put task to tasks
					tasks.put(id, new InstructedTask(saveTask, true));
				}
			}
		}
	}

	/**
	 * Creates save mission profile info tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveMissionProfileInfo(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save mission profile info tasks...");

		// loop over save mission profile info elements
		for (Element saveMissionProfileInfo : equinoxInput.getChildren("saveMissionProfileInfo")) {

			// get inputs
			String id = saveMissionProfileInfo.getChild("id").getTextNormalize();
			String stressSequenceId = saveMissionProfileInfo.getChild("stressSequenceId").getTextNormalize();
			Path outputPath = Paths.get(saveMissionProfileInfo.getChild("outputPath").getTextNormalize());

			// create task
			SaveMissionProfile task = new SaveMissionProfile(null, outputPath.toFile());

			// connect to parent task
			ParameterizedTaskOwner<StressSequence> parentTask = (ParameterizedTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
			parentTask.addParameterizedTask(id, task);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates plot mission profile tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotMissionProfile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot mission profile tasks...");

		// loop over plot mission profile elements
		for (Element plotMissionProfile : equinoxInput.getChildren("plotMissionProfile")) {

			// get inputs
			String id = plotMissionProfile.getChild("id").getTextNormalize();
			String stressSequenceId = plotMissionProfile.getChild("stressSequenceId").getTextNormalize();
			Path outputPath = Paths.get(plotMissionProfile.getChild("outputPath").getTextNormalize());

			// create task
			SaveStressSequencePlotToFile task = new SaveStressSequencePlotToFile(null, PilotPointImageType.MISSION_PROFILE, outputPath);

			// connect to parent task
			ParameterizedTaskOwner<StressSequence> parentTask = (ParameterizedTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
			parentTask.addParameterizedTask(id, task);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates save stress sequence tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveStressSequence(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save stress sequence tasks...");

		// loop over save stress sequence elements
		for (Element saveStressSequence : equinoxInput.getChildren("saveStressSequence")) {

			// create task
			String id = saveStressSequence.getChild("id").getTextNormalize();
			Path outputPath = Paths.get(saveStressSequence.getChild("outputPath").getTextNormalize());

			// stress sequence
			if (saveStressSequence.getChild("stressSequenceId") != null) {

				// get stress sequence id
				String stressSequenceId = saveStressSequence.getChild("stressSequenceId").getTextNormalize();
				SingleInputTask<StressSequence> task = null;

				// get file type
				FileType fileType = FileType.getFileType(outputPath.toFile());

				// save as SIGMA
				if (fileType.equals(FileType.SIGMA)) {
					task = new SaveStressSequenceAsSIGMA(null, outputPath.toFile());
				}

				// save as STH
				else if (fileType.equals(FileType.STH)) {
					task = new SaveStressSequenceAsSTH(null, outputPath.toFile());
				}

				// add to parent task
				ParameterizedTaskOwner<StressSequence> parentTask = (ParameterizedTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
				parentTask.addParameterizedTask(id, task);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// put task to tasks
				tasks.put(id, new InstructedTask((InternalEquinoxTask<?>) task, true));
			}

			// headless stress sequence
			else if (saveStressSequence.getChild("headlessStressSequenceId") != null) {

				// get headless stress sequence id
				String headlessStressSequenceId = saveStressSequence.getChild("headlessStressSequenceId").getTextNormalize();
				SingleInputTask<ExternalStressSequence> task = null;

				// get file type
				FileType fileType = FileType.getFileType(outputPath.toFile());

				// save as SIGMA
				if (fileType.equals(FileType.SIGMA)) {
					task = new SaveExternalStressSequenceAsSIGMA(null, outputPath.toFile());
				}

				// save as STH
				else if (fileType.equals(FileType.STH)) {
					task = new SaveExternalStressSequenceAsSTH(null, outputPath.toFile());
				}

				// add to parent task
				ParameterizedTaskOwner<ExternalStressSequence> parentTask = (ParameterizedTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
				parentTask.addParameterizedTask(id, task);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// put task to tasks
				tasks.put(id, new InstructedTask((InternalEquinoxTask<?>) task, true));
			}
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

		// input mapping
		HashMap<Path, GenerateStressSequenceInput> inputs = new HashMap<>();

		// loop over generate stress sequence elements
		for (Element generateStressSequence : equinoxInput.getChildren("generateStressSequence")) {

			// parse elements
			String id = generateStressSequence.getChild("id").getTextNormalize();
			String stfId = generateStressSequence.getChild("stfId").getTextNormalize();
			Path xmlPath = Paths.get(generateStressSequence.getChild("xmlPath").getTextNormalize());

			// get input parameters
			GenerateStressSequenceInput input = inputs.get(xmlPath);
			if (input == null) {
				input = new ReadGenerateStressSequenceInput(this, xmlPath).start(null);
				inputs.put(xmlPath, input);
			}

			// create task
			GenerateStressSequence generateStressSequenceTask = new GenerateStressSequence(null, input);

			// add to parent task
			ParameterizedTaskOwner<STFFile> parentTask = (ParameterizedTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addParameterizedTask(id, generateStressSequenceTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(generateStressSequenceTask, true));
		}
	}

	/**
	 * Creates add headless stress sequence tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addHeadlessStressSequence(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating add headless stress sequence tasks...");

		// get add stress sequence elements
		for (Element addHeadlessStressSequence : equinoxInput.getChildren("addHeadlessStressSequence")) {

			// get id
			String id = addHeadlessStressSequence.getChild("id").getTextNormalize();

			// from SIGMA
			if (addHeadlessStressSequence.getChild("sigmaPath") != null) {
				Path sigmaPath = Paths.get(addHeadlessStressSequence.getChild("sigmaPath").getTextNormalize());
				tasks.put(id, new InstructedTask(new AddStressSequence(sigmaPath), false));
			}

			// from STH
			else if (addHeadlessStressSequence.getChild("sthPath") != null) {
				Path sthPath = Paths.get(addHeadlessStressSequence.getChild("sthPath").getTextNormalize());
				Path flsPath = Paths.get(addHeadlessStressSequence.getChild("flsPath").getTextNormalize());
				tasks.put(id, new InstructedTask(new AddStressSequence(sthPath, flsPath), false));
			}
		}
	}

	/**
	 * Creates upload STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void uploadStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating upload STF tasks...");

		// loop over upload STF elements
		for (Element uploadStf : equinoxInput.getChildren("uploadStf")) {

			// create task
			String id = uploadStf.getChild("id").getTextNormalize();
			String exportId = uploadStf.getChild("exportId").getTextNormalize();
			UploadPilotPoints uploadStfTask = new UploadPilotPoints(null);

			// add to parent task
			ParameterizedTaskOwner<Path> parentTask = (ParameterizedTaskOwner<Path>) tasks.get(exportId).getTask();
			parentTask.addParameterizedTask(id, uploadStfTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(uploadStfTask, true));
		}
	}

	/**
	 * Creates export STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void exportStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating export STF tasks...");

		// loop over export STF elements
		for (Element exportStf : equinoxInput.getChildren("exportStf")) {

			// create task
			String id = exportStf.getChild("id").getTextNormalize();
			String stfId = exportStf.getChild("stfId").getTextNormalize();
			Path outputPath = Paths.get(exportStf.getChild("outputPath").getTextNormalize());
			GetSTFInfo3 getSTFInfoTask = new GetSTFInfo3(null);
			ExportSTF exportSTFTask = new ExportSTF(outputPath.toFile());

			// create info array
			String[] info = new String[12];

			// loop over pilot point info elements
			for (Element pilotPointInfo : exportStf.getChildren("pilotPointInfo")) {

				// get attribute name and value
				String attributeName = pilotPointInfo.getChildTextNormalize("attributeName");
				String attributeValue = pilotPointInfo.getChildTextNormalize("attributeValue");

				// fatigue mission
				if (attributeName.equals("fatigueMission")) {
					exportSTFTask.setMission(attributeValue);
				}

				// description
				else if (attributeName.equals("description")) {
					info[GetSTFInfo2.DESCRIPTION] = attributeValue;
				}

				// data source
				else if (attributeName.equals("dataSource")) {
					info[GetSTFInfo2.DATA_SOURCE] = attributeValue;
				}

				// generation source
				else if (attributeName.equals("generationSource")) {
					info[GetSTFInfo2.GEN_SOURCE] = attributeValue;
				}

				// delivery reference
				else if (attributeName.equals("deliveryReference")) {
					info[GetSTFInfo2.DELIVERY_REF] = attributeValue;
				}

				// issue
				else if (attributeName.equals("issue")) {
					info[GetSTFInfo2.ISSUE] = attributeValue;
				}

				// eid
				else if (attributeName.equals("eid")) {
					info[GetSTFInfo2.EID] = attributeValue;
				}

				// element type
				else if (attributeName.equals("elementType")) {
					info[GetSTFInfo2.ELEMENT_TYPE] = attributeValue;
				}

				// frame position
				else if (attributeName.equals("framePosition")) {
					info[GetSTFInfo2.FRAME_RIB_POS] = attributeValue;
				}

				// stringer position
				else if (attributeName.equals("stringerPosition")) {
					info[GetSTFInfo2.STRINGER_POS] = attributeValue;
				}

				// fatigue material
				else if (attributeName.equals("fatigueMaterial")) {
					info[GetSTFInfo2.FATIGUE_MATERIAL] = attributeValue;
				}

				// preffas material
				else if (attributeName.equals("preffasMaterial")) {
					info[GetSTFInfo2.PREFFAS_MATERIAL] = attributeValue;
				}

				// linear material
				else if (attributeName.equals("linearMaterial")) {
					info[GetSTFInfo2.LINEAR_MATERIAL] = attributeValue;
				}
			}

			// set info
			exportSTFTask.setInfo(info);

			// create image mapping
			HashMap<PilotPointImageType, Image> images = new HashMap<>();

			// loop over pilot point image elements
			for (Element pilotPointImage : exportStf.getChildren("pilotPointImage")) {

				// get attribute name and value
				String imageTypeName = pilotPointImage.getChildTextNormalize("imageType");
				Path imagePath = Paths.get(pilotPointImage.getChildTextNormalize("imagePath"));

				// get image type
				PilotPointImageType imageType = null;
				for (PilotPointImageType type : PilotPointImageType.values()) {
					if (type.toString().equals(imageTypeName)) {
						imageType = type;
						break;
					}
				}

				// get image bytes
				Image image = null;
				byte[] imageBytes = new byte[(int) imagePath.toFile().length()];
				try (ImageInputStream imgStream = ImageIO.createImageInputStream(imagePath.toFile())) {
					imgStream.read(imageBytes);
					try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
						image = new Image(inputStream);
					}
				}

				// add to mapping
				images.put(imageType, image);
			}

			// set images
			if (!images.isEmpty()) {
				exportSTFTask.setImages(images);
			}

			// add to first task
			getSTFInfoTask.addParameterizedTask(id, exportSTFTask);
			getSTFInfoTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// add to parent task
			ParameterizedTaskOwner<STFFile> parentTask = (ParameterizedTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addParameterizedTask(id, getSTFInfoTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(exportSTFTask, true));
		}
	}

	/**
	 * Creates share STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void shareStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating share STF tasks...");

		// loop over share STF elements
		for (Element shareStf : equinoxInput.getChildren("shareStf")) {

			// create task
			String id = shareStf.getChild("id").getTextNormalize();
			String stfId = shareStf.getChild("stfId").getTextNormalize();
			String recipient = shareStf.getChild("recipient").getTextNormalize();
			ShareSTF shareSTFTask = new ShareSTF(null, Arrays.asList(recipient));

			// add to parent task
			ParameterizedTaskOwner<STFFile> parentTask = (ParameterizedTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addParameterizedTask(id, shareSTFTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(shareSTFTask, true));
		}
	}

	/**
	 * Creates save STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save STF tasks...");

		// loop over save STF elements
		for (Element saveStf : equinoxInput.getChildren("saveStf")) {

			// create task
			String id = saveStf.getChild("id").getTextNormalize();
			String stfId = saveStf.getChild("stfId").getTextNormalize();
			Path outputPath = Paths.get(saveStf.getChild("outputPath").getTextNormalize());
			SaveSTF saveSTFTask = new SaveSTF(null, outputPath.toFile(), FileType.getFileType(outputPath.toFile()));

			// add to parent task
			ParameterizedTaskOwner<STFFile> parentTask = (ParameterizedTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addParameterizedTask(id, saveSTFTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(saveSTFTask, true));
		}
	}

	/**
	 * Creates override mission parameters to STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void assignMissionParametersToStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating override mission parameters to STF tasks...");

		// loop over override mission parameters elements
		for (Element assignMissionParametersToStf : equinoxInput.getChildren("assignMissionParametersToStf")) {

			// create task
			String id = assignMissionParametersToStf.getChild("id").getTextNormalize();
			String stfId = assignMissionParametersToStf.getChild("stfId").getTextNormalize();
			ArrayList<MissionParameter> parameters = new ArrayList<>();

			// loop over mission parameter elements
			for (Element missionParameter : assignMissionParametersToStf.getChildren("missionParameter")) {

				// get parameter name and value
				String name = missionParameter.getChildTextNormalize("name");
				double value = Double.parseDouble(missionParameter.getChildTextNormalize("value"));

				// create and add mission parameter
				parameters.add(new MissionParameter(name, value));
			}

			// create task
			AssignMissionParameters<STFFile> assignMissionParametersTask = new AssignMissionParameters<>(null, parameters.toArray(new MissionParameter[parameters.size()]));

			// add to parent task
			ParameterizedTaskOwner<STFFile> parentTask = (ParameterizedTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addParameterizedTask(id, assignMissionParametersTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(assignMissionParametersTask, true));
		}
	}

	/**
	 * Creates override fatigue mission tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void overrideFatigueMission(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating override fatigue mission tasks...");

		// loop over override fatigue mission elements
		for (Element overrideFatigueMission : equinoxInput.getChildren("overrideFatigueMission")) {

			// create task
			String id = overrideFatigueMission.getChild("id").getTextNormalize();
			String stfId = overrideFatigueMission.getChild("stfId").getTextNormalize();
			String fatigueMission = overrideFatigueMission.getChild("fatigueMission").getTextNormalize();
			SetSTFMission setStfMissionTask = new SetSTFMission(null, fatigueMission);

			// add to parent task
			ParameterizedTaskOwner<STFFile> parentTask = (ParameterizedTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addParameterizedTask(id, setStfMissionTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(setStfMissionTask, true));
		}
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
				ParameterizedTaskOwner<Spectrum> parentTask = (ParameterizedTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
				parentTask.addParameterizedTask(id, addSTFFilesTask);
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
				SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
				panel.setEngineSettings(input);

				// create tasks
				AdvancedPilotPointSearch searchPilotPointTask = new AdvancedPilotPointSearch(input);
				DownloadPilotPoint downloadPilotPointTask = new DownloadPilotPoint(null, null, null);

				// add download task to search
				searchPilotPointTask.addParameterizedTask(id, downloadPilotPointTask);
				searchPilotPointTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// add search to add spectrum
				ParameterizedTaskOwner<Spectrum> parentTask = (ParameterizedTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
				parentTask.addParameterizedTask(id, searchPilotPointTask);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// add to tasks
				tasks.put(id, new InstructedTask(downloadPilotPointTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(searchPilotPointTask, true));
			}

			// create dummy STF
			else {

				// create task
				String fileName = addStf.getChild("stfName").getTextNormalize();
				boolean is1D = addStf.getChild("stressState").getTextNormalize().equals("1d");
				CreateDummySTFFile createDummySTFTask = new CreateDummySTFFile(null, fileName, is1D);

				// get 1g stresses
				if (addStf.getChild("onegStresses") != null) {
					Element onegStresses = addStf.getChild("onegStresses");
					double sx = Double.parseDouble(onegStresses.getChildTextNormalize("sx"));
					double sy = Double.parseDouble(onegStresses.getChildTextNormalize("sy"));
					double sxy = Double.parseDouble(onegStresses.getChildTextNormalize("sxy"));
					createDummySTFTask.setOneGStresses(sx, sy, sxy);
				}

				// get increment stresses
				if (addStf.getChild("incrementStresses") != null) {
					Element incrementStresses = addStf.getChild("incrementStresses");
					double sx = Double.parseDouble(incrementStresses.getChildTextNormalize("sx"));
					double sy = Double.parseDouble(incrementStresses.getChildTextNormalize("sy"));
					double sxy = Double.parseDouble(incrementStresses.getChildTextNormalize("sxy"));
					createDummySTFTask.setIncrementStresses(sx, sy, sxy);
				}

				// get delta-p stresses
				if (addStf.getChild("dpStresses") != null) {
					Element dpStresses = addStf.getChild("dpStresses");
					String dpLoadcase = dpStresses.getChildTextNormalize("dpLoadcase");
					double sx = Double.parseDouble(dpStresses.getChildTextNormalize("sx"));
					double sy = Double.parseDouble(dpStresses.getChildTextNormalize("sy"));
					double sxy = Double.parseDouble(dpStresses.getChildTextNormalize("sxy"));
					createDummySTFTask.setDeltaPStresses(dpLoadcase, sx, sy, sxy);
				}

				// get delta-t stresses
				if (addStf.getChild("dtStresses") != null) {

					// get element
					Element dtStresses = addStf.getChild("dtStresses");

					// superior
					if (dtStresses.getChild("superior") != null) {
						Element superior = addStf.getChild("superior");
						String loadcase = superior.getChildTextNormalize("loadcase");
						double sx = Double.parseDouble(superior.getChildTextNormalize("sx"));
						double sy = Double.parseDouble(superior.getChildTextNormalize("sy"));
						double sxy = Double.parseDouble(superior.getChildTextNormalize("sxy"));
						createDummySTFTask.setDeltaTSupStresses(loadcase, sx, sy, sxy);
					}

					// inferior
					if (dtStresses.getChild("inferior") != null) {
						Element inferior = addStf.getChild("inferior");
						String loadcase = inferior.getChildTextNormalize("loadcase");
						double sx = Double.parseDouble(inferior.getChildTextNormalize("sx"));
						double sy = Double.parseDouble(inferior.getChildTextNormalize("sy"));
						double sxy = Double.parseDouble(inferior.getChildTextNormalize("sxy"));
						createDummySTFTask.setDeltaTInfStresses(loadcase, sx, sy, sxy);
					}
				}

				// add to parent task
				ParameterizedTaskOwner<Spectrum> parentTask = (ParameterizedTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
				parentTask.addParameterizedTask(id, createDummySTFTask);
				parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

				// put task to tasks
				tasks.put(id, new InstructedTask(createDummySTFTask, true));
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
			SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
			panel.setEngineSettings(input);

			// create tasks
			AdvancedPilotPointSearch searchPilotPointTask = new AdvancedPilotPointSearch(input);
			DownloadPilotPoint downloadPilotPointTask = new DownloadPilotPoint(null, outputFile, null);
			searchPilotPointTask.addParameterizedTask(id, downloadPilotPointTask);
			searchPilotPointTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// add to tasks
			tasks.put(id, new InstructedTask(downloadPilotPointTask, true));
			tasks.put("ownerOf_" + id, new InstructedTask(searchPilotPointTask, false));
		}
	}

	/**
	 * Creates upload spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void uploadSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating upload spectrum tasks...");

		// loop over upload spectrum elements
		for (Element uploadSpectrum : equinoxInput.getChildren("uploadSpectrum")) {

			// create task
			String id = uploadSpectrum.getChild("id").getTextNormalize();
			String exportId = uploadSpectrum.getChild("exportId").getTextNormalize();
			UploadSpectra uploadSectrumTask = new UploadSpectra(null);

			// add to parent task
			ParameterizedTaskOwner<Path> parentTask = (ParameterizedTaskOwner<Path>) tasks.get(exportId).getTask();
			parentTask.addParameterizedTask(id, uploadSectrumTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(uploadSectrumTask, true));
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
			getSpectrumInfoTask.addParameterizedTask(id, exportSpectrumTask);
			getSpectrumInfoTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// add to parent task
			ParameterizedTaskOwner<Spectrum> parentTask = (ParameterizedTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addParameterizedTask(id, getSpectrumInfoTask);
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
			ParameterizedTaskOwner<Spectrum> parentTask = (ParameterizedTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addParameterizedTask(id, shareSpectrumFileTask);
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
			ParameterizedTaskOwner<Spectrum> parentTask = (ParameterizedTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addParameterizedTask(id, shareSpectrumTask);
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
			SingleInputTask<Spectrum> saveSpectrumFileTask = null;

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
			ParameterizedTaskOwner<Spectrum> parentTask = (ParameterizedTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addParameterizedTask(id, saveSpectrumFileTask);
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
			ParameterizedTaskOwner<Spectrum> parentTask = (ParameterizedTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addParameterizedTask(id, saveSpectrumTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(saveSpectrumTask, true));
		}
	}

	/**
	 * Creates assign mission parameters to spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void assignMissionParametersToSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating assign mission parameters to spectrum tasks...");

		// loop over assign mission parameters elements
		for (Element assignMissionParametersToSpectrum : equinoxInput.getChildren("assignMissionParametersToSpectrum")) {

			// create task
			String id = assignMissionParametersToSpectrum.getChild("id").getTextNormalize();
			String spectrumId = assignMissionParametersToSpectrum.getChild("spectrumId").getTextNormalize();
			ArrayList<MissionParameter> parameters = new ArrayList<>();

			// loop over mission parameter elements
			for (Element missionParameter : assignMissionParametersToSpectrum.getChildren("missionParameter")) {

				// get parameter name and value
				String name = missionParameter.getChildTextNormalize("name");
				double value = Double.parseDouble(missionParameter.getChildTextNormalize("value"));

				// create and add mission parameter
				parameters.add(new MissionParameter(name, value));
			}

			// create task
			AssignMissionParameters<Spectrum> assignMissionParametersTask = new AssignMissionParameters<>(null, parameters.toArray(new MissionParameter[parameters.size()]));

			// add to parent task
			ParameterizedTaskOwner<Spectrum> parentTask = (ParameterizedTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addParameterizedTask(id, assignMissionParametersTask);
			parentTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// put task to tasks
			tasks.put(id, new InstructedTask(assignMissionParametersTask, true));
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
				ParameterizedTaskOwner<Pair<Path, SpectrumInfo>> parentTask = (ParameterizedTaskOwner<Pair<Path, SpectrumInfo>>) tasks.get(downloadId).getTask();
				parentTask.addParameterizedTask(id, addSpectrumTask);
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
			SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
			panel.setEngineSettings(input);

			// create tasks
			AdvancedSpectrumSearch searchSpectrumTask = new AdvancedSpectrumSearch(input);
			DownloadSpectrum downloadSpectrumTask = new DownloadSpectrum(null, outputFile, false);
			searchSpectrumTask.addParameterizedTask(id, downloadSpectrumTask);
			searchSpectrumTask.setAutomaticTaskExecutionMode(runMode.equals(PARALLEL));

			// add to tasks
			tasks.put(id, new InstructedTask(downloadSpectrumTask, true));
			tasks.put("ownerOf_" + id, new InstructedTask(searchSpectrumTask, false));
		}
	}
}