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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.data.AnalysisEngine;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.task.InternalEquinoxTask.LongRunningTask;

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
			addWarning("Cannot locate root input element 'equinoxInput' in instruction set. This element is obligatory. Check failed.");
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
			if (!checkElementId(equinoxInput, generateStressSequence))
				return false;

			// check STF id
			if (!checkDependency(equinoxInput, generateStressSequence, "stfId", "addStf"))
				return false;

			// check input path
			if (!checkPathValue(generateStressSequence, "inputPath", false))
				return false;

			// get input path
			String inputPath = generateStressSequence.getChild("inputPath").getTextNormalize();

			// already checked
			if (checkedInputs.contains(inputPath)) {
				continue;
			}

			// TODO check generate stress sequence input
			// if(!new CheckGenerateStressSequenceInput(Paths.get(inputPath)).start(null))
			// return false;

			// add to checked inputs
			checkedInputs.add(inputPath);
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
			if (!checkElementId(equinoxInput, addStressSequence))
				return false;

			// from SIGMA file
			if (addStressSequence.getChild("sigmaPath") != null) {
				if (!checkPathValue(addStressSequence, "sigmaPath", false))
					return false;
			}

			// from STH file
			else {
				if (!checkPathValue(addStressSequence, "sthPath", false))
					return false;
				if (!checkPathValue(addStressSequence, "flsPath", false))
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
			if (!checkElementId(equinoxInput, addStf))
				return false;

			// check spectrum id
			if (!checkDependency(equinoxInput, addStf, "spectrumId", "addSpectrum"))
				return false;

			// check STF path
			if (!checkPathValue(addStf, "stfPath", false))
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
			if (!checkElementId(equinoxInput, addSpectrum))
				return false;

			// SPEC bundle
			if (addSpectrum.getChild("specPath") != null) {
				if (!checkPathValue(addSpectrum, "specPath", false))
					return false;
			}

			// CDF set files
			else {
				if (!checkPathValue(addSpectrum, "anaPath", false))
					return false;
				if (!checkPathValue(addSpectrum, "cvtPath", false))
					return false;
				if (!checkPathValue(addSpectrum, "flsPath", false))
					return false;
				if (!checkPathValue(addSpectrum, "xlsPath", false))
					return false;
				if (!checkPathValue(addSpectrum, "txtPath", true))
					return false;
				if (!checkStringValue(addSpectrum, "convSheet", false))
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
		if (!checkStringValue(settings, "runMode", true, RunInstructionSet.PARALLEL, RunInstructionSet.SEQUENTIAL, RunInstructionSet.SAVE))
			return false;

		// check run silent
		if (!checkBooleanValue(settings, "runSilent", true))
			return false;

		// check analysis engine
		if (settings.getChild("analysisEngine") != null) {

			// get element
			Element analysisEngine = settings.getChild("analysisEngine");

			// engine
			if (!checkStringValue(analysisEngine, "engine", true, getStringArray(AnalysisEngine.values())))
				return false;

			// version
			if (!checkStringValue(analysisEngine, "version", true, getStringArray(IsamiVersion.values())))
				return false;

			// sub version
			if (!checkStringValue(analysisEngine, "subVersion", true, getStringArray(IsamiSubVersion.values())))
				return false;

			// keep output file
			if (!checkBooleanValue(analysisEngine, "keepOutputFile", true))
				return false;

			// perform detailed analysis
			if (!checkBooleanValue(analysisEngine, "performDetailedAnalysis", true))
				return false;

			// apply compression for propagation
			if (!checkBooleanValue(analysisEngine, "applyCompressionForPropagation", true))
				return false;

			// fallback to inbuilt
			if (!checkBooleanValue(analysisEngine, "fallbackToInbuilt", true))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if the dependency of the given element is satisfied.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param element
	 *            Element to check its dependency.
	 * @param dependencyName
	 *            Dependency name.
	 * @param targetElementName
	 *            Target element name.
	 * @return True if the dependency of the given element is satisfied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkDependency(Element equinoxInput, Element element, String dependencyName, String targetElementName) throws Exception {

		// get source element
		Element sourceElement = element.getChild(dependencyName);

		// source element not found
		if (sourceElement == null) {
			addWarning("Cannot locate element '" + dependencyName + "' under " + getFamilyTree(element) + " in instruction set. This element is obligatory. Check failed.");
			return false;
		}

		// get source id
		String sourceId = sourceElement.getTextNormalize();

		// empty id
		if (sourceId.isEmpty()) {
			addWarning("Empty value supplied for " + getFamilyTree(sourceElement) + ". Check failed.");
			return false;
		}

		// search for referenced element
		for (Element targetElement : equinoxInput.getChildren(targetElementName)) {
			Element id = targetElement.getChild("id");
			if (id != null) {
				if (sourceId.equals(id.getTextNormalize()))
					return true;
			}
		}

		// referenced element not found
		addWarning("Cannot find element with task id '" + sourceId + "' which appears to be a dependency of " + getFamilyTree(sourceElement) + ". Check failed.");
		return false;
	}

	/**
	 * Returns true if the given element has a valid task id.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param element
	 *            Element to be checked.
	 * @return True if the given element has a valid task id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkElementId(Element equinoxInput, Element element) throws Exception {

		// no task id
		if (element.getChild("id") == null) {
			addWarning("No task id specified for " + getFamilyTree(element) + ". Check failed.");
			return false;
		}

		// get id
		String id = element.getChild("id").getTextNormalize();

		// invalid id
		if (id.isEmpty()) {
			addWarning("Invalid id specified for " + getFamilyTree(element) + ". Check failed.");
			return false;
		}

		// loop over elements
		for (Element c : equinoxInput.getChildren()) {

			// pivot
			if (c.equals(element)) {
				continue;
			}

			// no id
			if (c.getChild("id") == null) {
				continue;
			}

			// same id
			if (c.getChild("id").getTextNormalize().equals(id)) {
				addWarning("Non-unique id specified for " + getFamilyTree(element) + ". Check failed.");
				return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>Path</code> value.
	 *
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if given element has a valid <code>Path</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPathValue(Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			addWarning("Cannot locate element '" + elementName + "' under " + getFamilyTree(parentElement) + " in instruction set. This element is obligatory. Check failed.");
			return false;
		}

		// invalid value
		if (!Files.exists(Paths.get(element.getTextNormalize()))) {
			addWarning("Invalid file path supplied for " + getFamilyTree(element) + ". Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>String</code> value.
	 *
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @param validValues
	 *            List of valid values. Can be <code>null</code> for checking only against empty values.
	 * @return True if given element has a valid <code>String</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkStringValue(Element parentElement, String elementName, boolean isOptionalElement, String... validValues) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			addWarning("Cannot locate element '" + elementName + "' under " + getFamilyTree(parentElement) + " in instruction set. This element is obligatory. Check failed.");
			return false;
		}

		// get value of element
		String value = element.getTextNormalize();

		// empty value
		if (value.isEmpty()) {
			addWarning("Empty value supplied for " + getFamilyTree(element) + ". Check failed.");
			return false;
		}

		// invalid value
		if (validValues != null && !Arrays.asList(validValues).contains(value)) {
			addWarning("Invalid value supplied for " + getFamilyTree(element) + ". Valid values are: " + Arrays.toString(validValues) + ". Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>boolean</code> value.
	 *
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if given element has a valid <code>boolean</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkBooleanValue(Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			addWarning("Cannot locate element '" + elementName + "' under " + getFamilyTree(parentElement) + " in instruction set. This element is obligatory. Check failed.");
			return false;
		}

		// get value of element
		String value = element.getTextNormalize();

		// invalid value
		if (!value.equals("true") && !value.equals("false")) {
			addWarning("Invalid value supplied for " + getFamilyTree(element) + ". Valid values are 'true' or 'false'. Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns <code>String</code> representation of the family tree of the given element.
	 *
	 * @param element
	 *            Target element.
	 * @return <code>String</code> representation of the family tree of the given element.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getFamilyTree(Element element) throws Exception {
		String tree = element.getName();
		Element p = element.getParentElement();
		while (p != null) {
			tree = p.getName() + "." + tree;
			p = p.getParentElement();
		}
		return tree;
	}

	/**
	 * Converts given object array to string array.
	 *
	 * @param objects
	 *            Array of objects.
	 * @return Array of strings.
	 */
	private static String[] getStringArray(Object[] objects) {
		String[] strings = new String[objects.length];
		for (int i = 0; i < objects.length; i++) {
			strings[i] = objects[i].toString();
		}
		return strings;
	}
}