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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.data.AnalysisEngine;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.plugin.FileType;
import equinox.process.CheckGenerateStressSequenceInput;
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

		// add spectrum
		if (equinoxInput.getChild("addSpectrum") != null) {
			if (!checkAddSpectrum(equinoxInput))
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

		// add STF
		if (equinoxInput.getChild("addStf") != null) {
			if (!checkAddStf(equinoxInput))
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

			// check STF path
			if (!XMLUtilities.checkInputPathValue(this, inputFile, addStf, "stfPath", false, FileType.STF))
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

		// check passed
		return true;
	}
}