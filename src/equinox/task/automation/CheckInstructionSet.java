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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.Equinox;
import equinox.data.IsamiVersion;
import equinox.data.Settings;
import equinox.data.input.StressSequenceComparisonInput.ComparisonCriteria;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.plugin.FileType;
import equinox.process.automation.CheckEquivalentStressAnalysisInput;
import equinox.process.automation.CheckGenerateStressSequenceInput;
import equinox.task.InternalEquinoxTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.XMLUtilities;

/**
 * Class for check instruction set task.
 *
 * @author Murat Artim
 * @date 18 Aug 2018
 * @time 14:56:00
 */
public class CheckInstructionSet extends InternalEquinoxTask<Boolean> implements LongRunningTask {

	/** Input XML file. */
	private final Path inputFile;

	/** True if the instruction set should be run if it passes the check. */
	private final boolean run;

	/** True to overwrite existing files. */
	private boolean overwriteFiles = true;

	/**
	 * Creates check instruction set task.
	 *
	 * @param inputFile
	 *            Input XML file.
	 * @param run
	 *            True if the instruction set should be run if it passes the check.
	 */
	public CheckInstructionSet(Path inputFile, boolean run) {
		this.inputFile = inputFile;
		this.run = run;
	}

	@Override
	public String getTaskTitle() {
		return "Check instruction set";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Boolean call() throws Exception {

		// read input file
		updateMessage("Reading input XML file...");

		// get root input element
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(inputFile.toUri().toString());
		Element equinoxInput = document.getRootElement();

		// cannot find root input element
		if (equinoxInput == null) {
			addWarning("Cannot locate root input element 'equinoxInput' in instruction set '" + inputFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// settings
		if (equinoxInput.getChild("settings") != null) {
			if (!checkSettings(equinoxInput))
				return false;
		}

		// download spectrum
		if (equinoxInput.getChild("downloadSpectrum") != null) {
			if (!checkDownloadSpectrum(equinoxInput))
				return false;
		}

		// add spectrum
		if (equinoxInput.getChild("addSpectrum") != null) {
			if (!checkAddSpectrum(equinoxInput))
				return false;
		}

		// assign mission parameters to spectrum
		if (equinoxInput.getChild("assignMissionParametersToSpectrum") != null) {
			if (!checkAssignMissionParametersToSpectrum(equinoxInput))
				return false;
		}

		// save spectrum
		if (equinoxInput.getChild("saveSpectrum") != null) {
			if (!checkSaveSpectrum(equinoxInput))
				return false;
		}

		// save spectrum file
		if (equinoxInput.getChild("saveSpectrumFile") != null) {
			if (!checkSaveSpectrumFile(equinoxInput))
				return false;
		}

		// share spectrum
		if (equinoxInput.getChild("shareSpectrum") != null) {
			if (!checkShareSpectrum(equinoxInput))
				return false;
		}

		// share spectrum file
		if (equinoxInput.getChild("shareSpectrumFile") != null) {
			if (!checkShareSpectrumFile(equinoxInput))
				return false;
		}

		// export spectrum
		if (equinoxInput.getChild("exportSpectrum") != null) {
			if (!checkExportSpectrum(equinoxInput))
				return false;
		}

		// upload spectrum
		if (equinoxInput.getChild("uploadSpectrum") != null) {
			if (!checkUploadSpectrum(equinoxInput))
				return false;
		}

		// download STF
		if (equinoxInput.getChild("downloadStf") != null) {
			if (!checkDownloadStf(equinoxInput))
				return false;
		}

		// add STF
		if (equinoxInput.getChild("addStf") != null) {
			if (!checkAddStf(equinoxInput))
				return false;
		}

		// override fatigue mission
		if (equinoxInput.getChild("overrideFatigueMission") != null) {
			if (!checkOverrideFatigueMission(equinoxInput))
				return false;
		}

		// assign mission parameters to STF
		if (equinoxInput.getChild("assignMissionParametersToStf") != null) {
			if (!checkAssignMissionParametersToStf(equinoxInput))
				return false;
		}

		// save STF
		if (equinoxInput.getChild("saveStf") != null) {
			if (!checkSaveStf(equinoxInput))
				return false;
		}

		// share STF
		if (equinoxInput.getChild("shareStf") != null) {
			if (!checkShareStf(equinoxInput))
				return false;
		}

		// export STF
		if (equinoxInput.getChild("exportStf") != null) {
			if (!checkExportStf(equinoxInput))
				return false;
		}

		// upload STF
		if (equinoxInput.getChild("uploadStf") != null) {
			if (!checkUploadStf(equinoxInput))
				return false;
		}

		// add headless stress sequence
		if (equinoxInput.getChild("addHeadlessStressSequence") != null) {
			if (!checkAddHeadlessStressSequence(equinoxInput))
				return false;
		}

		// generate stress sequence
		if (equinoxInput.getChild("generateStressSequence") != null) {
			if (!checkGenerateStressSequence(equinoxInput))
				return false;
		}

		// assign mission parameters to headless stress sequence
		if (equinoxInput.getChild("assignMissionParametersToStressSequence") != null) {
			if (!checkAssignMissionParametersToStressSequence(equinoxInput))
				return false;
		}

		// edit stress sequence info
		if (equinoxInput.getChild("editStressSequenceInfo") != null) {
			if (!checkEditStressSequenceInfo(equinoxInput))
				return false;
		}

		// save stress sequence
		if (equinoxInput.getChild("saveStressSequence") != null) {
			if (!checkSaveStressSequence(equinoxInput))
				return false;
		}

		// plot mission profile
		if (equinoxInput.getChild("plotMissionProfile") != null) {
			if (!checkPlotMissionProfile(equinoxInput))
				return false;
		}

		// save mission profile info
		if (equinoxInput.getChild("saveMissionProfileInfo") != null) {
			if (!checkSaveMissionProfileInfo(equinoxInput))
				return false;
		}

		// plot typical flight
		if (equinoxInput.getChild("plotTypicalFlight") != null) {
			if (!checkPlotTypicalFlight(equinoxInput, "plotTypicalFlight"))
				return false;
		}

		// plot typical flight statistics
		if (equinoxInput.getChild("plotTypicalFlightStatistics") != null) {
			if (!checkPlotTypicalFlight(equinoxInput, "plotTypicalFlightStatistics"))
				return false;
		}

		// equivalent stress analysis
		if (equinoxInput.getChild("equivalentStressAnalysis") != null) {
			if (!checkEquivalentStressAnalysis(equinoxInput))
				return false;
		}

		// plot level crossing
		if (equinoxInput.getChild("plotLevelCrossing") != null) {
			if (!checkPlotLevelCrossing(equinoxInput))
				return false;
		}

		// plot rainflow histogram
		if (equinoxInput.getChild("plotRainflowHistogram") != null) {
			if (!checkPlotRainflowHistogram(equinoxInput))
				return false;
		}

		// save rainflow cycle info
		if (equinoxInput.getChild("saveRainflowCycleInfo") != null) {
			if (!checkSaveRainflowCycleInfo(equinoxInput))
				return false;
		}

		// save analysis output file
		if (equinoxInput.getChild("saveAnalysisOutputFile") != null) {
			if (!checkSaveAnalysisOutputFile(equinoxInput))
				return false;
		}

		// plot stress sequence comparison
		if (equinoxInput.getChild("plotStressSequenceComparison") != null) {
			if (!checkPlotStressSequenceComparison(equinoxInput))
				return false;
		}

		// plot level crossing comparison
		if (equinoxInput.getChild("plotLevelCrossingComparison") != null) {
			if (!checkPlotLevelCrossingComparison(equinoxInput))
				return false;
		}

		// plot typical flight comparison
		if (equinoxInput.getChild("plotTypicalFlightComparison") != null) {
			if (!checkPlotTypicalFlightComparison(equinoxInput))
				return false;
		}

		// plot equivalent stress comparison
		if (equinoxInput.getChild("plotEquivalentStressComparison") != null) {
			if (!checkPlotEquivalentStressComparison(equinoxInput))
				return false;
		}

		// plot life factors
		if (equinoxInput.getChild("plotLifeFactors") != null) {
			if (!checkPlotLifeFactors(equinoxInput))
				return false;
		}

		// plot equivalent stress ratios
		if (equinoxInput.getChild("plotEquivalentStressRatios") != null) {
			if (!checkPlotEquivalentStressRatios(equinoxInput))
				return false;
		}

		// save equivalent stresses
		if (equinoxInput.getChild("saveEquivalentStresses") != null) {
			if (!checkSaveEquivalentStresses(equinoxInput))
				return false;
		}

		// save life factors
		if (equinoxInput.getChild("saveLifeFactors") != null) {
			if (!checkSaveLifeFactors(equinoxInput))
				return false;
		}

		// save equivalent stress ratios
		if (equinoxInput.getChild("saveEquivalentStressRatios") != null) {
			if (!checkSaveEquivalentStressRatios(equinoxInput))
				return false;
		}

		// TODO check next instructions

		// share file
		if (equinoxInput.getChild("shareFile") != null) {
			if (!checkShareFile(equinoxInput))
				return false;
		}

		// check passed
		return true;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// no run
		if (!run)
			return;

		// execute tasks
		try {

			// couldn't pass check
			if (!get())
				return;

			// run instruction set
			taskPanel_.getOwner().runTaskInParallel(new RunInstructionSet(inputFile));
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Returns true if all <code>editStressSequenceInfo</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>editStressSequenceInfo</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkEditStressSequenceInfo(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking editStressSequenceInfo elements...");

		// loop over edit stress sequence info elements
		for (Element editStressSequenceInfo : equinoxInput.getChildren("editStressSequenceInfo")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, editStressSequenceInfo))
				return false;

			// check headless stress sequence id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, editStressSequenceInfo, "headlessStressSequenceId", "addHeadlessStressSequence"))
				return false;

			// check aircraft program
			if (!XMLUtilities.checkStringValue(this, inputFile, editStressSequenceInfo, "aircraftProgram", true))
				return false;

			// check aircraft section
			if (!XMLUtilities.checkStringValue(this, inputFile, editStressSequenceInfo, "aircraftSection", true))
				return false;

			// check fatigue mission
			if (!XMLUtilities.checkStringValue(this, inputFile, editStressSequenceInfo, "fatigueMission", true))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>assignMissionParametersToStressSequence</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>assignMissionParametersToStressSequence</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkAssignMissionParametersToStressSequence(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking assignMissionParametersToStressSequence elements...");

		// loop over assign mission parameters to headless stress sequence elements
		for (Element assignMissionParametersToStressSequence : equinoxInput.getChildren("assignMissionParametersToStressSequence")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, assignMissionParametersToStressSequence))
				return false;

			// check headless stress sequence id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, assignMissionParametersToStressSequence, "headlessStressSequenceId", "addHeadlessStressSequence"))
				return false;

			// no mission parameter element found
			if (assignMissionParametersToStressSequence.getChild("missionParameter") == null) {
				addWarning("Cannot locate element 'missionParameter' under " + XMLUtilities.getFamilyTree(assignMissionParametersToStressSequence) + " in instruction set '" + inputFile.toString() + "'. At least 1 of this element is obligatory. Check failed.");
				return false;
			}

			// loop over mission parameter elements
			for (Element missionParameter : assignMissionParametersToStressSequence.getChildren("missionParameter")) {

				// check parameter name
				if (!XMLUtilities.checkStringValue(this, inputFile, missionParameter, "name", false))
					return false;

				// check parameter value
				if (!XMLUtilities.checkDoubleValue(this, inputFile, missionParameter, "value", false, null, null))
					return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveEquivalentStressRatios</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveEquivalentStressRatios</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveEquivalentStressRatios(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveEquivalentStressRatios elements...");

		// loop over save equivalent stress ratios elements
		for (Element saveEquivalentStressRatios : equinoxInput.getChildren("saveEquivalentStressRatios")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveEquivalentStressRatios))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveEquivalentStressRatios, "outputPath", false, overwriteFiles, FileType.XLS))
				return false;

			// check options
			if (saveEquivalentStressRatios.getChild("options") != null) {

				// get element
				Element options = saveEquivalentStressRatios.getChild("options");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "equivalentStressRatio", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "materialName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "materialData", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "pilotPointName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "elementId", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "stressSequenceName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "spectrumName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "aircraftProgram", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "aircraftSection", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "fatigueMission", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "spectrumValidity", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "maximumStress", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "minimumStress", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "rRatio", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "omissionLevel", true))
					return false;
			}

			// check basis mission
			if (!XMLUtilities.checkStringValue(this, inputFile, saveEquivalentStressRatios, "basisMission", false))
				return false;

			// check equivalent stress ids
			if (!XMLUtilities.checkDependencies(this, inputFile, equinoxInput, saveEquivalentStressRatios, "equivalentStressId", "equivalentStressAnalysis", 1))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveLifeFactors</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveLifeFactors</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveLifeFactors(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveLifeFactors elements...");

		// loop over save life factors elements
		for (Element saveLifeFactors : equinoxInput.getChildren("saveLifeFactors")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveLifeFactors))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveLifeFactors, "outputPath", false, overwriteFiles, FileType.XLS))
				return false;

			// check options
			if (saveLifeFactors.getChild("options") != null) {

				// get element
				Element options = saveLifeFactors.getChild("options");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "lifeFactor", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "materialName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "materialData", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "pilotPointName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "elementId", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "stressSequenceName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "spectrumName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "aircraftProgram", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "aircraftSection", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "fatigueMission", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "spectrumValidity", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "maximumStress", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "minimumStress", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "rRatio", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "omissionLevel", true))
					return false;
			}

			// check basis mission
			if (!XMLUtilities.checkStringValue(this, inputFile, saveLifeFactors, "basisMission", false))
				return false;

			// check equivalent stress ids
			if (!XMLUtilities.checkDependencies(this, inputFile, equinoxInput, saveLifeFactors, "equivalentStressId", "equivalentStressAnalysis", 1))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveEquivalentStresses</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveEquivalentStresses</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveEquivalentStresses(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveEquivalentStresses elements...");

		// loop over save equivalent stresses elements
		for (Element saveEquivalentStresses : equinoxInput.getChildren("saveEquivalentStresses")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveEquivalentStresses))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveEquivalentStresses, "outputPath", false, overwriteFiles, FileType.XLS))
				return false;

			// check options
			if (saveEquivalentStresses.getChild("options") != null) {

				// get element
				Element options = saveEquivalentStresses.getChild("options");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "equivalentStress", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "materialName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "materialData", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "pilotPointName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "elementId", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "stressSequenceName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "spectrumName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "aircraftProgram", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "aircraftSection", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "fatigueMission", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "spectrumValidity", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "maximumStress", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "minimumStress", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "rRatio", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "omissionLevel", true))
					return false;
			}

			// check equivalent stress ids
			if (!XMLUtilities.checkDependencies(this, inputFile, equinoxInput, saveEquivalentStresses, "equivalentStressId", "equivalentStressAnalysis", 1))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotEquivalentStressRatios</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>plotEquivalentStressRatios</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotEquivalentStressRatios(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking plotEquivalentStressRatios elements...");

		// loop over plot equivalent stress ratios elements
		for (Element plotEquivalentStressRatios : equinoxInput.getChildren("plotEquivalentStressRatios")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotEquivalentStressRatios))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotEquivalentStressRatios, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;

			// check series naming
			if (plotEquivalentStressRatios.getChild("seriesNaming") != null) {

				// get element
				Element seriesNaming = plotEquivalentStressRatios.getChild("seriesNaming");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeSpectrumName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStfName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeElementId", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStressSequenceName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeMaterialName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeOmissionLevel", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftProgram", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftSection", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeFatigueMission", true))
					return false;
			}

			// check basis mission
			if (!XMLUtilities.checkStringValue(this, inputFile, plotEquivalentStressRatios, "basisMission", false))
				return false;

			// check options
			if (plotEquivalentStressRatios.getChild("options") != null) {

				// get element
				Element options = plotEquivalentStressRatios.getChild("options");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "includeBasisMission", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "showDataLabels", true))
					return false;
			}

			// check mission parameter
			if (!XMLUtilities.checkStringValue(this, inputFile, plotEquivalentStressRatios, "missionParameter", true))
				return false;

			// check equivalent stress ids
			if (!XMLUtilities.checkDependencies(this, inputFile, equinoxInput, plotEquivalentStressRatios, "equivalentStressId", "equivalentStressAnalysis", 2))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotLifeFactors</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>plotLifeFactors</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotLifeFactors(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking plotLifeFactors elements...");

		// loop over plot life factors elements
		for (Element plotLifeFactors : equinoxInput.getChildren("plotLifeFactors")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotLifeFactors))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotLifeFactors, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;

			// check series naming
			if (plotLifeFactors.getChild("seriesNaming") != null) {

				// get element
				Element seriesNaming = plotLifeFactors.getChild("seriesNaming");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeSpectrumName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStfName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeElementId", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStressSequenceName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeMaterialName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeOmissionLevel", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftProgram", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftSection", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeFatigueMission", true))
					return false;
			}

			// check basis mission
			if (!XMLUtilities.checkStringValue(this, inputFile, plotLifeFactors, "basisMission", false))
				return false;

			// check options
			if (plotLifeFactors.getChild("options") != null) {

				// get element
				Element options = plotLifeFactors.getChild("options");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "includeBasisMission", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "showDataLabels", true))
					return false;
			}

			// check mission parameter
			if (!XMLUtilities.checkStringValue(this, inputFile, plotLifeFactors, "missionParameter", true))
				return false;

			// check equivalent stress ids
			if (!XMLUtilities.checkDependencies(this, inputFile, equinoxInput, plotLifeFactors, "equivalentStressId", "equivalentStressAnalysis", 2))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotEquivalentStressComparison</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>plotEquivalentStressComparison</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotEquivalentStressComparison(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking plotEquivalentStressComparison elements...");

		// loop over plot equivalent stress comparison elements
		for (Element plotEquivalentStressComparison : equinoxInput.getChildren("plotEquivalentStressComparison")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotEquivalentStressComparison))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotEquivalentStressComparison, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;

			// check series naming
			if (plotEquivalentStressComparison.getChild("seriesNaming") != null) {

				// get element
				Element seriesNaming = plotEquivalentStressComparison.getChild("seriesNaming");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeSpectrumName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStfName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeElementId", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStressSequenceName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeMaterialName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeOmissionLevel", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftProgram", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftSection", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeFatigueMission", true))
					return false;
			}

			// check label display
			if (!XMLUtilities.checkBooleanValue(this, inputFile, plotEquivalentStressComparison, "showDataLabels", true))
				return false;

			// check mission parameter
			if (!XMLUtilities.checkStringValue(this, inputFile, plotEquivalentStressComparison, "missionParameter", true))
				return false;

			// check equivalent stress ids
			if (!XMLUtilities.checkDependencies(this, inputFile, equinoxInput, plotEquivalentStressComparison, "equivalentStressId", "equivalentStressAnalysis", 2))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotTypicalFlightComparison</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>plotTypicalFlightComparison</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotTypicalFlightComparison(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking plotTypicalFlightComparison elements...");

		// loop over plot typical flight comparison elements
		for (Element plotTypicalFlightComparison : equinoxInput.getChildren("plotTypicalFlightComparison")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotTypicalFlightComparison))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotTypicalFlightComparison, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;

			// check series naming
			if (plotTypicalFlightComparison.getChild("seriesNaming") != null) {

				// get element
				Element seriesNaming = plotTypicalFlightComparison.getChild("seriesNaming");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeSpectrumName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStfName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeElementId", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStressSequenceName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeTypicalFlightName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftProgram", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftSection", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeFatigueMission", true))
					return false;
			}

			// check stress components
			if (plotTypicalFlightComparison.getChild("stressComponents") != null) {

				// get element
				Element stressComponents = plotTypicalFlightComparison.getChild("stressComponents");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, stressComponents, "plotIncrements", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, stressComponents, "plotDp", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, stressComponents, "plotDt", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, stressComponents, "plot1g", true))
					return false;
			}

			// check typical flights
			List<Element> typicalFlights = plotTypicalFlightComparison.getChildren("typicalFlight");
			if (typicalFlights == null || typicalFlights.size() < 2) {
				addWarning("Cannot locate element 'typicalFlight' under " + XMLUtilities.getFamilyTree(plotTypicalFlightComparison) + " in instruction set '" + inputFile.toString() + "'. Minimum 2 of this element is obligatory. Check failed.");
				return false;
			}

			// loop over typical flights
			for (Element typicalFlight : typicalFlights) {

				// check typical flight name
				if (!XMLUtilities.checkStringValue(this, inputFile, typicalFlight, "typicalFlightName", false))
					return false;

				// check stress sequence id
				if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, typicalFlight, "stressSequenceId", "generateStressSequence"))
					return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotLevelCrossingComparison</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>plotLevelCrossingComparison</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotLevelCrossingComparison(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking plotLevelCrossingComparison elements...");

		// loop over plot level crossing comparison elements
		for (Element plotLevelCrossingComparison : equinoxInput.getChildren("plotLevelCrossingComparison")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotLevelCrossingComparison))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotLevelCrossingComparison, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;

			// set series naming
			if (plotLevelCrossingComparison.getChild("seriesNaming") != null) {

				// get element
				Element seriesNaming = plotLevelCrossingComparison.getChild("seriesNaming");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeSpectrumName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStfName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeElementId", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStressSequenceName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeMaterialName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeOmissionLevel", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftProgram", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftSection", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeFatigueMission", true))
					return false;
			}

			// check stress sequence ids
			if (!XMLUtilities.checkDependencies(this, inputFile, equinoxInput, plotLevelCrossingComparison, "equivalentStressId", "equivalentStressAnalysis", 2))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotStressSequenceComparison</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>plotStressSequenceComparison</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotStressSequenceComparison(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking plotStressSequenceComparison elements...");

		// loop over plot stress sequence comparison elements
		for (Element plotStressSequenceComparison : equinoxInput.getChildren("plotStressSequenceComparison")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotStressSequenceComparison))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotStressSequenceComparison, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;

			// check comparison criteria
			if (!XMLUtilities.checkStringValue(this, inputFile, plotStressSequenceComparison, "comparisonCriteria", false, XMLUtilities.getStringArray(ComparisonCriteria.values())))
				return false;

			// check options
			if (plotStressSequenceComparison.getChild("options") != null) {

				// get element
				Element options = plotStressSequenceComparison.getChild("options");

				// check results order
				if (!XMLUtilities.checkStringValue(this, inputFile, options, "resultsOrder", true, "descending", "ascending"))
					return false;

				// check show data labels
				if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "showDataLabels", true))
					return false;
			}

			// set series naming
			if (plotStressSequenceComparison.getChild("seriesNaming") != null) {

				// get element
				Element seriesNaming = plotStressSequenceComparison.getChild("seriesNaming");

				// check
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeSpectrumName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStfName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeElementId", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeStressSequenceName", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftProgram", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeAircraftSection", true))
					return false;
				if (!XMLUtilities.checkBooleanValue(this, inputFile, seriesNaming, "includeFatigueMission", true))
					return false;
			}

			// check stress sequence ids
			if (!XMLUtilities.checkDependencies(this, inputFile, equinoxInput, plotStressSequenceComparison, "stressSequenceId", "generateStressSequence", 2))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>shareFile</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>shareFile</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkShareFile(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking shareFile elements...");

		// loop over share file elements
		for (Element shareFile : equinoxInput.getChildren("shareFile")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, shareFile))
				return false;

			// check file id
			if (!XMLUtilities.checkDependencyStartingWith(this, inputFile, equinoxInput, shareFile, "fileId", "save", "export", "plot"))
				return false;

			// check recipient
			if (!XMLUtilities.checkRecipient(this, inputFile, shareFile, "recipient", false))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveAnalysisOutputFile</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveAnalysisOutputFile</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveAnalysisOutputFile(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveAnalysisOutputFile elements...");

		// loop over save analysis output file elements
		for (Element saveAnalysisOutputFile : equinoxInput.getChildren("saveAnalysisOutputFile")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveAnalysisOutputFile))
				return false;

			// check equivalent stress id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, saveAnalysisOutputFile, "equivalentStressId", "equivalentStressAnalysis"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveAnalysisOutputFile, "outputPath", false, overwriteFiles, FileType.DOSSIER, FileType.HTML))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveRainflowCycleInfo</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveRainflowCycleInfo</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveRainflowCycleInfo(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveRainflowCycleInfo elements...");

		// loop over save rainflow cycle info elements
		for (Element saveRainflowCycleInfo : equinoxInput.getChildren("saveRainflowCycleInfo")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveRainflowCycleInfo))
				return false;

			// check equivalent stress id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, saveRainflowCycleInfo, "equivalentStressId", "equivalentStressAnalysis"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveRainflowCycleInfo, "outputPath", false, overwriteFiles, FileType.RFLOW))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotRainflowHistogram</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>plotRainflowHistogram</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotRainflowHistogram(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking plotRainflowHistogram elements...");

		// loop over plot rainflow histogram elements
		for (Element plotRainflowHistogram : equinoxInput.getChildren("plotRainflowHistogram")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotRainflowHistogram))
				return false;

			// check equivalent stress id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, plotRainflowHistogram, "equivalentStressId", "equivalentStressAnalysis"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotRainflowHistogram, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotLevelCrossing</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>plotLevelCrossing</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotLevelCrossing(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking plotLevelCrossing elements...");

		// loop over plot level crossing elements
		for (Element plotLevelCrossing : equinoxInput.getChildren("plotLevelCrossing")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotLevelCrossing))
				return false;

			// check equivalent stress id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, plotLevelCrossing, "equivalentStressId", "equivalentStressAnalysis"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotLevelCrossing, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>equivalentStressAnalysis</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>equivalentStressAnalysis</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkEquivalentStressAnalysis(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking equivalentStressAnalysis elements...");

		// get ISAMI version
		Settings settings = taskPanel_.getOwner().getOwner().getSettings();
		IsamiVersion isamiVersion = (IsamiVersion) settings.getValue(Settings.ISAMI_VERSION);

		// create list of checked inputs
		ArrayList<String> checkedInputs = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// loop over equivalent stress analysis elements
			for (Element equivalentStressAnalysis : equinoxInput.getChildren("equivalentStressAnalysis")) {

				// no id
				if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, equivalentStressAnalysis))
					return false;

				// check stress sequence id
				if (equivalentStressAnalysis.getChild("stressSequenceId") != null) {
					if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, equivalentStressAnalysis, "stressSequenceId", "generateStressSequence"))
						return false;
				}

				// check headless stress sequence id
				else if (equivalentStressAnalysis.getChild("headlessStressSequenceId") != null) {
					if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, equivalentStressAnalysis, "headlessStressSequenceId", "addHeadlessStressSequence"))
						return false;
				}

				// check XML path
				if (!XMLUtilities.checkInputPathValue(this, inputFile, equivalentStressAnalysis, "xmlPath", false, FileType.XML))
					return false;

				// get XML path
				String xmlPath = equivalentStressAnalysis.getChild("xmlPath").getTextNormalize();

				// already checked
				if (checkedInputs.contains(xmlPath)) {
					continue;
				}

				// check generate stress sequence input
				if (!new CheckEquivalentStressAnalysisInput(this, Paths.get(xmlPath), isamiVersion).start(connection))
					return false;

				// add to checked inputs
				checkedInputs.add(xmlPath);
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotTypicalFlight</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param elementName
	 *            Element name.
	 * @return True if all <code>plotTypicalFlight</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotTypicalFlight(Element equinoxInput, String elementName) throws Exception {

		// read input file
		updateMessage("Checking plotTypicalFlight elements...");

		// loop over plot typical flight elements
		for (Element plotTypicalFlight : equinoxInput.getChildren(elementName)) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotTypicalFlight))
				return false;

			// check stress sequence id
			if (plotTypicalFlight.getChild("stressSequenceId") != null) {
				if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, plotTypicalFlight, "stressSequenceId", "generateStressSequence"))
					return false;
			}

			// check headless stress sequence id
			else if (plotTypicalFlight.getChild("headlessStressSequenceId") != null) {

				// headless stress sequence id
				if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, plotTypicalFlight, "headlessStressSequenceId", "addHeadlessStressSequence"))
					return false;

				// check options
				if (plotTypicalFlight.getChild("options") != null) {

					// get element
					Element options = plotTypicalFlight.getChild("options");

					// check results order
					if (!XMLUtilities.checkStringValue(this, inputFile, options, "resultsOrder", true, "descending", "ascending"))
						return false;

					// check show data labels
					if (!XMLUtilities.checkBooleanValue(this, inputFile, options, "showDataLabels", true))
						return false;

					// check max flights
					if (!XMLUtilities.checkIntegerValue(this, inputFile, options, "maxFlights", true, 1, null))
						return false;
				}
			}

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotTypicalFlight, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;

			// check plot type
			if (!XMLUtilities.checkStringValue(this, inputFile, plotTypicalFlight, "plotType", false, XMLUtilities.getStringArray(PilotPointImageType.values())))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveMissionProfileInfo</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveMissionProfileInfo</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveMissionProfileInfo(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveMissionProfileInfo elements...");

		// loop over save mission profile info elements
		for (Element saveMissionProfileInfo : equinoxInput.getChildren("saveMissionProfileInfo")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveMissionProfileInfo))
				return false;

			// check stress sequence id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, saveMissionProfileInfo, "stressSequenceId", "generateStressSequence"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveMissionProfileInfo, "outputPath", false, overwriteFiles, FileType.XLS))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>plotMissionProfile</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>plotMissionProfile</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPlotMissionProfile(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking plotMissionProfile elements...");

		// loop over plot mission profile elements
		for (Element plotMissionProfile : equinoxInput.getChildren("plotMissionProfile")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, plotMissionProfile))
				return false;

			// check stress sequence id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, plotMissionProfile, "stressSequenceId", "generateStressSequence"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, plotMissionProfile, "outputPath", false, overwriteFiles, FileType.PNG))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveStressSequence</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveStressSequence</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveStressSequence(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveStressSequence elements...");

		// loop over save stress sequence elements
		for (Element saveStressSequence : equinoxInput.getChildren("saveStressSequence")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveStressSequence))
				return false;

			// check stress sequence id
			if (saveStressSequence.getChild("stressSequenceId") != null) {
				if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, saveStressSequence, "stressSequenceId", "generateStressSequence"))
					return false;
			}

			// check headless stress sequence id
			else if (saveStressSequence.getChild("headlessStressSequenceId") != null) {
				if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, saveStressSequence, "headlessStressSequenceId", "addHeadlessStressSequence"))
					return false;
			}

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveStressSequence, "outputPath", false, overwriteFiles, FileType.SIGMA, FileType.STH))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>generateStressSequence</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>generateStressSequence</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkGenerateStressSequence(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking generateStressSequence elements...");

		// create list of checked inputs
		ArrayList<String> checkedInputs = new ArrayList<>();

		// loop over generate stress sequence elements
		for (Element generateStressSequence : equinoxInput.getChildren("generateStressSequence")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, generateStressSequence))
				return false;

			// check STF id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, generateStressSequence, "stfId", "addStf"))
				return false;

			// check XML path
			if (!XMLUtilities.checkInputPathValue(this, inputFile, generateStressSequence, "xmlPath", false, FileType.XML))
				return false;

			// get XML path
			String xmlPath = generateStressSequence.getChild("xmlPath").getTextNormalize();

			// already checked
			if (checkedInputs.contains(xmlPath)) {
				continue;
			}

			// check generate stress sequence input
			if (!new CheckGenerateStressSequenceInput(this, Paths.get(xmlPath)).start(null))
				return false;

			// add to checked inputs
			checkedInputs.add(xmlPath);
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>addHeadlessStressSequence</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>addHeadlessStressSequence</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkAddHeadlessStressSequence(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking addHeadlessStressSequence elements...");

		// loop over add stress sequence elements
		for (Element addHeadlessStressSequence : equinoxInput.getChildren("addHeadlessStressSequence")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, addHeadlessStressSequence))
				return false;

			// from SIGMA file
			if (addHeadlessStressSequence.getChild("sigmaPath") != null) {
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addHeadlessStressSequence, "sigmaPath", false, FileType.SIGMA))
					return false;
			}

			// from STH file
			else {
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addHeadlessStressSequence, "sthPath", false, FileType.STH))
					return false;
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addHeadlessStressSequence, "flsPath", false, FileType.FLS))
					return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>uploadStf</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>uploadStf</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkUploadStf(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking uploadStf elements...");

		// loop over upload STF elements
		for (Element uploadStf : equinoxInput.getChildren("uploadStf")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, uploadStf))
				return false;

			// check export id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, uploadStf, "exportId", "exportStf"))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>exportStf</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>exportStf</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkExportStf(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking exportSpectrum elements...");

		// loop over export spectrum elements
		for (Element exportStf : equinoxInput.getChildren("exportStf")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, exportStf))
				return false;

			// check STF id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, exportStf, "stfId", "addStf"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, exportStf, "outputPath", false, overwriteFiles, FileType.ZIP))
				return false;

			// loop over pilot point info elements (if any)
			String[] attributeNames = { "fatigueMission", "description", "dataSource", "generationSource", "deliveryReference", "issue", "eid", "elementType", "framePosition", "stringerPosition", "fatigueMaterial", "preffasMaterial", "linearMaterial" };
			for (Element pilotPointInfo : exportStf.getChildren("pilotPointInfo")) {

				// check attribute name
				if (!XMLUtilities.checkStringValue(this, inputFile, pilotPointInfo, "attributeName", false, attributeNames))
					return false;

				// check attribute values
				if (!XMLUtilities.checkStringValue(this, inputFile, pilotPointInfo, "attributeValue", false))
					return false;
			}

			// loop over pilot point image elements
			for (Element pilotPointImage : exportStf.getChildren("pilotPointImage")) {

				// check image type
				if (!XMLUtilities.checkStringValue(this, inputFile, pilotPointImage, "imageType", false, XMLUtilities.getStringArray(PilotPointImageType.values())))
					return false;

				// check image path
				if (!XMLUtilities.checkInputPathValue(this, inputFile, pilotPointImage, "imagePath", false, FileType.PNG))
					return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>shareStf</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>shareStf</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkShareStf(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking shareStf elements...");

		// loop over share STF elements
		for (Element shareStf : equinoxInput.getChildren("shareStf")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, shareStf))
				return false;

			// check STF id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, shareStf, "stfId", "addStf"))
				return false;

			// check recipient
			if (!XMLUtilities.checkRecipient(this, inputFile, shareStf, "recipient", false))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveStf</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveStf</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveStf(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveStf elements...");

		// loop over save STF elements
		for (Element saveStf : equinoxInput.getChildren("saveStf")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveStf))
				return false;

			// check STF id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, saveStf, "stfId", "addStf"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveStf, "outputPath", false, overwriteFiles, FileType.STF, FileType.ZIP, FileType.GZ))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>assignMissionParametersToStf</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>assignMissionParametersToStf</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkAssignMissionParametersToStf(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking assignMissionParametersToStf elements...");

		// loop over assign mission parameters elements
		for (Element assignMissionParametersToStf : equinoxInput.getChildren("assignMissionParametersToStf")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, assignMissionParametersToStf))
				return false;

			// check STF id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, assignMissionParametersToStf, "stfId", "addStf"))
				return false;

			// no mission parameter element found
			if (assignMissionParametersToStf.getChild("missionParameter") == null) {
				addWarning("Cannot locate element 'missionParameter' under " + XMLUtilities.getFamilyTree(assignMissionParametersToStf) + " in instruction set '" + inputFile.toString() + "'. At least 1 of this element is obligatory. Check failed.");
				return false;
			}

			// loop over mission parameter elements
			for (Element missionParameter : assignMissionParametersToStf.getChildren("missionParameter")) {

				// check parameter name
				if (!XMLUtilities.checkStringValue(this, inputFile, missionParameter, "name", false))
					return false;

				// check parameter value
				if (!XMLUtilities.checkDoubleValue(this, inputFile, missionParameter, "value", false, null, null))
					return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>overrideFatigueMission</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>overrideFatigueMission</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkOverrideFatigueMission(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking overrideFatigueMission elements...");

		// loop over override fatigue mission elements
		for (Element overrideFatigueMission : equinoxInput.getChildren("overrideFatigueMission")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, overrideFatigueMission))
				return false;

			// check STF id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, overrideFatigueMission, "stfId", "addStf"))
				return false;

			// check fatigue mission
			if (!XMLUtilities.checkStringValue(this, inputFile, overrideFatigueMission, "fatigueMission", false))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>addStf</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>addStf</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkAddStf(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking addStf elements...");

		// loop over add STF elements
		for (Element addStf : equinoxInput.getChildren("addStf")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, addStf))
				return false;

			// check spectrum id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, addStf, "spectrumId", "addSpectrum"))
				return false;

			// STF path given
			if (addStf.getChild("stfPath") != null) {
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addStf, "stfPath", false, FileType.STF))
					return false;
			}

			// search entry given
			else if (addStf.getChild("searchEntry") != null) {
				if (!XMLUtilities.checkSearchEntries(this, inputFile, addStf, XMLUtilities.getStringArray(PilotPointInfoType.values())))
					return false;
			}

			// create dummy STF
			else {

				// check file name
				if (!XMLUtilities.checkStringValue(this, inputFile, addStf, "stfName", false))
					return false;

				// check stress state
				if (!XMLUtilities.checkStringValue(this, inputFile, addStf, "stressState", false, "1d", "2d"))
					return false;

				// check 1g stresses
				if (addStf.getChild("onegStresses") != null) {
					if (!XMLUtilities.checkDoubleValue(this, inputFile, addStf.getChild("onegStresses"), "sx", false, null, null))
						return false;
					if (!XMLUtilities.checkDoubleValue(this, inputFile, addStf.getChild("onegStresses"), "sy", false, null, null))
						return false;
					if (!XMLUtilities.checkDoubleValue(this, inputFile, addStf.getChild("onegStresses"), "sxy", false, null, null))
						return false;
				}

				// check increment stresses
				if (addStf.getChild("incrementStresses") != null) {
					if (!XMLUtilities.checkDoubleValue(this, inputFile, addStf.getChild("incrementStresses"), "sx", false, null, null))
						return false;
					if (!XMLUtilities.checkDoubleValue(this, inputFile, addStf.getChild("incrementStresses"), "sy", false, null, null))
						return false;
					if (!XMLUtilities.checkDoubleValue(this, inputFile, addStf.getChild("incrementStresses"), "sxy", false, null, null))
						return false;
				}

				// check delta-p stresses
				if (addStf.getChild("dpStresses") != null) {
					if (!XMLUtilities.checkStringValue(this, inputFile, addStf.getChild("dpStresses"), "dpLoadcase", false))
						return false;
					if (!XMLUtilities.checkDoubleValue(this, inputFile, addStf.getChild("dpStresses"), "sx", false, null, null))
						return false;
					if (!XMLUtilities.checkDoubleValue(this, inputFile, addStf.getChild("dpStresses"), "sy", false, null, null))
						return false;
					if (!XMLUtilities.checkDoubleValue(this, inputFile, addStf.getChild("dpStresses"), "sxy", false, null, null))
						return false;
				}

				// check delta-t stresses
				if (addStf.getChild("dtStresses") != null) {

					// get element
					Element dtStresses = addStf.getChild("dtStresses");

					// superior
					if (dtStresses.getChild("superior") != null) {
						if (!XMLUtilities.checkStringValue(this, inputFile, dtStresses.getChild("superior"), "loadcase", false))
							return false;
						if (!XMLUtilities.checkDoubleValue(this, inputFile, dtStresses.getChild("superior"), "sx", false, null, null))
							return false;
						if (!XMLUtilities.checkDoubleValue(this, inputFile, dtStresses.getChild("superior"), "sy", false, null, null))
							return false;
						if (!XMLUtilities.checkDoubleValue(this, inputFile, dtStresses.getChild("superior"), "sxy", false, null, null))
							return false;
					}

					// inferior
					if (dtStresses.getChild("inferior") != null) {
						if (!XMLUtilities.checkStringValue(this, inputFile, dtStresses.getChild("inferior"), "loadcase", false))
							return false;
						if (!XMLUtilities.checkDoubleValue(this, inputFile, dtStresses.getChild("inferior"), "sx", false, null, null))
							return false;
						if (!XMLUtilities.checkDoubleValue(this, inputFile, dtStresses.getChild("inferior"), "sy", false, null, null))
							return false;
						if (!XMLUtilities.checkDoubleValue(this, inputFile, dtStresses.getChild("inferior"), "sxy", false, null, null))
							return false;
					}
				}
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>downloadStf</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>downloadStf</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkDownloadStf(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking downloadStf elements...");

		// loop over download STF elements
		for (Element downloadStf : equinoxInput.getChildren("downloadStf")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, downloadStf))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, downloadStf, "outputPath", false, overwriteFiles, FileType.ZIP))
				return false;

			// check search entries
			if (!XMLUtilities.checkSearchEntries(this, inputFile, downloadStf, XMLUtilities.getStringArray(PilotPointInfoType.values())))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>downloadSpectrum</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>downloadSpectrum</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkDownloadSpectrum(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking downloadSpectrum elements...");

		// loop over download spectrum elements
		for (Element downloadSpectrum : equinoxInput.getChildren("downloadSpectrum")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, downloadSpectrum))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, downloadSpectrum, "outputPath", false, overwriteFiles, FileType.ZIP))
				return false;

			// check search entries
			if (!XMLUtilities.checkSearchEntries(this, inputFile, downloadSpectrum, XMLUtilities.getStringArray(SpectrumInfoType.values())))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>assignMissionParameters</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>assignMissionParameters</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkAssignMissionParametersToSpectrum(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking assignMissionParametersToSpectrum elements...");

		// loop over assign mission parameters elements
		for (Element assignMissionParametersToSpectrum : equinoxInput.getChildren("assignMissionParametersToSpectrum")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, assignMissionParametersToSpectrum))
				return false;

			// check spectrum id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, assignMissionParametersToSpectrum, "spectrumId", "addSpectrum"))
				return false;

			// no mission parameter element found
			if (assignMissionParametersToSpectrum.getChild("missionParameter") == null) {
				addWarning("Cannot locate element 'missionParameter' under " + XMLUtilities.getFamilyTree(assignMissionParametersToSpectrum) + " in instruction set '" + inputFile.toString() + "'. At least 1 of this element is obligatory. Check failed.");
				return false;
			}

			// loop over mission parameter elements
			for (Element missionParameter : assignMissionParametersToSpectrum.getChildren("missionParameter")) {

				// check parameter name
				if (!XMLUtilities.checkStringValue(this, inputFile, missionParameter, "name", false))
					return false;

				// check parameter value
				if (!XMLUtilities.checkDoubleValue(this, inputFile, missionParameter, "value", false, null, null))
					return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>uploadSpectrum</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>uploadSpectrum</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkUploadSpectrum(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking uploadSpectrum elements...");

		// loop over upload spectrum elements
		for (Element uploadSpectrum : equinoxInput.getChildren("uploadSpectrum")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, uploadSpectrum))
				return false;

			// check export id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, uploadSpectrum, "exportId", "exportSpectrum"))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>exportSpectrum</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>exportSpectrum</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkExportSpectrum(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking exportSpectrum elements...");

		// loop over export spectrum elements
		for (Element exportSpectrum : equinoxInput.getChildren("exportSpectrum")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, exportSpectrum))
				return false;

			// check spectrum id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, exportSpectrum, "spectrumId", "addSpectrum"))
				return false;

			// check delivery reference
			if (!XMLUtilities.checkStringValue(this, inputFile, exportSpectrum, "deliveryReference", true))
				return false;

			// check description
			if (!XMLUtilities.checkStringValue(this, inputFile, exportSpectrum, "description", true))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, exportSpectrum, "outputPath", false, overwriteFiles, FileType.ZIP))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>shareSpectrumFile</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>shareSpectrumFile</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkShareSpectrumFile(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking shareSpectrumFile elements...");

		// loop over share spectrum file elements
		for (Element shareSpectrum : equinoxInput.getChildren("shareSpectrumFile")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, shareSpectrum))
				return false;

			// check spectrum id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, shareSpectrum, "spectrumId", "addSpectrum"))
				return false;

			// check file type
			if (!XMLUtilities.checkStringValue(this, inputFile, shareSpectrum, "fileType", false, "ana", "cvt", "txt", "xls", "fls"))
				return false;

			// check recipient
			if (!XMLUtilities.checkRecipient(this, inputFile, shareSpectrum, "recipient", false))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>shareSpectrum</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>shareSpectrum</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkShareSpectrum(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking shareSpectrum elements...");

		// loop over share spectrum elements
		for (Element shareSpectrum : equinoxInput.getChildren("shareSpectrum")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, shareSpectrum))
				return false;

			// check spectrum id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, shareSpectrum, "spectrumId", "addSpectrum"))
				return false;

			// check recipient
			if (!XMLUtilities.checkRecipient(this, inputFile, shareSpectrum, "recipient", false))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveSpectrumFile</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveSpectrumFile</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveSpectrumFile(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveSpectrumFile elements...");

		// loop over save spectrum elements
		for (Element saveSpectrum : equinoxInput.getChildren("saveSpectrumFile")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveSpectrum))
				return false;

			// check spectrum id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, saveSpectrum, "spectrumId", "addSpectrum"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveSpectrum, "outputPath", false, overwriteFiles, FileType.ANA, FileType.TXT, FileType.CVT, FileType.FLS, FileType.XLS))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>saveSpectrum</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>saveSpectrum</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSaveSpectrum(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking saveSpectrum elements...");

		// loop over save spectrum elements
		for (Element saveSpectrum : equinoxInput.getChildren("saveSpectrum")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, saveSpectrum))
				return false;

			// check spectrum id
			if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, saveSpectrum, "spectrumId", "addSpectrum"))
				return false;

			// check output path
			if (!XMLUtilities.checkOutputPathValue(this, inputFile, saveSpectrum, "outputPath", false, overwriteFiles, FileType.ZIP, FileType.SPEC))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if all <code>addSpectrum</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>addSpectrum</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkAddSpectrum(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking addSpectrum elements...");

		// loop over add spectrum elements
		for (Element addSpectrum : equinoxInput.getChildren("addSpectrum")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, addSpectrum))
				return false;

			// SPEC bundle
			if (addSpectrum.getChild("specPath") != null) {
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addSpectrum, "specPath", false, FileType.SPEC))
					return false;
			}

			// download from central database
			else if (addSpectrum.getChild("downloadId") != null) {

				// check download id
				if (!XMLUtilities.checkDependency(this, inputFile, equinoxInput, addSpectrum, "downloadId", "downloadSpectrum"))
					return false;
			}

			// CDF set files
			else {
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addSpectrum, "anaPath", false, FileType.ANA))
					return false;
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addSpectrum, "cvtPath", false, FileType.CVT))
					return false;
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addSpectrum, "flsPath", false, FileType.FLS))
					return false;
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addSpectrum, "xlsPath", false, FileType.XLS))
					return false;
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addSpectrum, "txtPath", true, FileType.TXT))
					return false;
				if (!XMLUtilities.checkStringValue(this, inputFile, addSpectrum, "convSheet", false))
					return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if <code>settings</code> element passes checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if <code>settings</code> element passes checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkSettings(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking settings element...");

		// get element
		Element settings = equinoxInput.getChild("settings");

		// check run mode
		if (!XMLUtilities.checkStringValue(this, inputFile, settings, "runMode", true, RunInstructionSet.PARALLEL, RunInstructionSet.SEQUENTIAL, RunInstructionSet.SAVE))
			return false;

		// check overwrite files
		if (!XMLUtilities.checkBooleanValue(this, inputFile, settings, "overwriteFiles", true))
			return false;
		if (settings.getChild("overwriteFiles") != null) {
			overwriteFiles = Boolean.parseBoolean(settings.getChild("overwriteFiles").getTextNormalize());
		}

		// check passed
		return true;
	}
}