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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.data.AnalysisEngine;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.data.SearchInput;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.plugin.FileType;
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

		// add stress sequence
		if (equinoxInput.getChild("addStressSequence") != null) {
			if (!checkAddStressSequence(equinoxInput))
				return false;
		}

		// generate stress sequence
		if (equinoxInput.getChild("generateStressSequence") != null) {
			if (!checkGenerateStressSequence(equinoxInput))
				return false;
		}

		// TODO check next instructions

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

			// check CML path
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
	 * Returns true if all <code>addStressSequence</code> elements pass checks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @return True if all <code>addStressSequence</code> elements pass checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkAddStressSequence(Element equinoxInput) throws Exception {

		// read input file
		updateMessage("Checking addStressSequence elements...");

		// loop over add stress sequence elements
		for (Element addStressSequence : equinoxInput.getChildren("addStressSequence")) {

			// no id
			if (!XMLUtilities.checkElementId(this, inputFile, equinoxInput, addStressSequence))
				return false;

			// from SIGMA file
			if (addStressSequence.getChild("sigmaPath") != null) {
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addStressSequence, "sigmaPath", false, FileType.SIGMA))
					return false;
			}

			// from STH file
			else {
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addStressSequence, "sthPath", false, FileType.STH))
					return false;
				if (!XMLUtilities.checkInputPathValue(this, inputFile, addStressSequence, "flsPath", false, FileType.FLS))
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
				if (!XMLUtilities.checkOutputPathValue(this, inputFile, pilotPointImage, "imagePath", false, overwriteFiles, FileType.PNG))
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

			// check spectrum id
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
				if (!XMLUtilities.checkDoubleValue(this, inputFile, missionParameter, "value", false))
					return false;
			}
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

			// nothing given
			else {
				addWarning("Cannot locate element 'stfPath' or 'searchEntry' under " + XMLUtilities.getFamilyTree(addStf) + " in instruction set '" + inputFile.toString() + "'. At least one of the mentioned elements is obligatory. Check failed.");
				return false;
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
				if (!XMLUtilities.checkDoubleValue(this, inputFile, missionParameter, "value", false))
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

		// check run silent
		if (!XMLUtilities.checkBooleanValue(this, inputFile, settings, "runSilent", true))
			return false;

		// check overwrite files
		if (!XMLUtilities.checkBooleanValue(this, inputFile, settings, "overwriteFiles", true))
			return false;
		if (settings.getChild("overwriteFiles") != null) {
			overwriteFiles = Boolean.parseBoolean(settings.getChild("overwriteFiles").getTextNormalize());
		}

		// check analysis engine
		if (settings.getChild("analysisEngine") != null) {

			// get element
			Element analysisEngine = settings.getChild("analysisEngine");

			// engine
			if (!XMLUtilities.checkStringValue(this, inputFile, analysisEngine, "engine", true, XMLUtilities.getStringArray(AnalysisEngine.values())))
				return false;

			// version
			if (!XMLUtilities.checkStringValue(this, inputFile, analysisEngine, "version", true, XMLUtilities.getStringArray(IsamiVersion.values())))
				return false;

			// sub version
			if (!XMLUtilities.checkStringValue(this, inputFile, analysisEngine, "subVersion", true, XMLUtilities.getStringArray(IsamiSubVersion.values())))
				return false;

			// keep output file
			if (!XMLUtilities.checkBooleanValue(this, inputFile, analysisEngine, "keepOutputFile", true))
				return false;

			// perform detailed analysis
			if (!XMLUtilities.checkBooleanValue(this, inputFile, analysisEngine, "performDetailedAnalysis", true))
				return false;

			// apply compression for propagation
			if (!XMLUtilities.checkBooleanValue(this, inputFile, analysisEngine, "applyCompressionForPropagation", true))
				return false;

			// fallback to inbuilt
			if (!XMLUtilities.checkBooleanValue(this, inputFile, analysisEngine, "fallbackToInbuilt", true))
				return false;
		}

		// check search engine
		if (settings.getChild("searchEngine") != null) {

			// get element
			Element searchEngine = settings.getChild("searchEngine");

			// logical operator
			if (!XMLUtilities.checkStringValue(this, inputFile, searchEngine, "logicalOperator", true, "and", "or"))
				return false;

			// ignore case
			if (!XMLUtilities.checkBooleanValue(this, inputFile, searchEngine, "ignoreCase", true))
				return false;

			// maximum hits
			if (!XMLUtilities.checkIntegerValue(this, inputFile, searchEngine, "maxHits", true))
				return false;

			// order results by
			if (!XMLUtilities.checkStringValue(this, inputFile, searchEngine, "orderResultsBy", true, SearchInput.NAME, SearchInput.PROGRAM, SearchInput.SECTION, SearchInput.MISSION, SearchInput.DELIVERY))
				return false;

			// results order
			if (!XMLUtilities.checkStringValue(this, inputFile, searchEngine, "resultsOrder", true, "Ascending", "Descending"))
				return false;
		}

		// check passed
		return true;
	}
}