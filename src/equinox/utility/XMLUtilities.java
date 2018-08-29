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
package equinox.utility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.jdom2.Element;

import equinox.Equinox;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask;

/**
 * Class for supplying utility methods for checking instruction sets.
 *
 * @author Murat Artim
 * @date 21 Aug 2018
 * @time 09:58:49
 */
public class XMLUtilities {

	/**
	 * Extracts and returns search keywords.
	 *
	 * @param keywords
	 *            User input.
	 * @return List of keywords.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static ArrayList<String> extractSearchKeywords(String keywords) throws Exception {

		// create input list
		ArrayList<String> inputs = new ArrayList<>();

		// multiple keywords
		if (keywords.contains(",")) {
			StringTokenizer st = new StringTokenizer(keywords, ",");
			while (st.hasMoreTokens()) {
				String word = st.nextToken().trim();
				if (!word.isEmpty()) {
					inputs.add(word);
				}
			}
		}

		// single keyword
		else {
			inputs.add(keywords);
		}

		// return list
		return inputs;
	}

	/**
	 * Returns true if given element has valid search entries.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param validAttributeNames
	 *            List of valid attribute names.
	 * @return True if given element has valid search entries.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkSearchEntries(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String... validAttributeNames) throws Exception {

		// no search entry found
		if (parentElement.getChild("searchEntry") == null) {
			task.addWarning("Cannot locate element 'searchEntry' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. At least one of this element is obligatory. Check failed.");
			return false;
		}

		// loop over search entries
		for (Element searchEntry : parentElement.getChildren("searchEntry")) {

			// check attribute name
			if (!checkStringValue(task, xmlFile, searchEntry, "attributeName", false, validAttributeNames))
				return false;

			// check keyword
			if (!checkStringValue(task, xmlFile, searchEntry, "keyword", false))
				return false;

			// check criteria
			if (!checkStringValue(task, xmlFile, searchEntry, "criteria", true, "Contains", "Equals", "Starts with", "Ends with"))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if given element has an online username.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if if given element has an online username.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkRecipient(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get recipient
		String recipient = element.getTextNormalize();

		// null or empty value
		if (recipient == null || recipient.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// this user
		if (recipient.equals(Equinox.USER.getUsername())) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// recipient is offline
		if (!task.getTaskPanel().getOwner().getOwner().isUserAvailable(recipient)) {
			task.addWarning("Recipient of " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "' is not online. Check failed.");
			return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if the dependency of the given element is satisfied.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param root
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
	public static boolean checkDependency(InternalEquinoxTask<?> task, Path xmlFile, Element root, Element element, String dependencyName, String targetElementName) throws Exception {

		// get source element
		Element sourceElement = element.getChild(dependencyName);

		// source element not found
		if (sourceElement == null) {
			task.addWarning("Cannot locate element '" + dependencyName + "' under " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get source id
		String sourceId = sourceElement.getTextNormalize();

		// empty id
		if (sourceId.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// search for referenced element
		for (Element targetElement : root.getChildren(targetElementName)) {
			Element id = targetElement.getChild("id");
			if (id != null) {
				if (sourceId.equals(id.getTextNormalize()))
					return true;
			}
		}

		// referenced element not found
		task.addWarning("Cannot find element with task id '" + sourceId + "' which appears to be a dependency of " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
		return false;
	}

	/**
	 * Returns true if the dependency of the given element is satisfied.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param root
	 *            Root input element.
	 * @param element
	 *            Element to check its dependency.
	 * @param dependencyName
	 *            Dependency name.
	 * @param targetElementNamePrefixes
	 *            Target element name prefixes.
	 * @return True if the dependency of the given element is satisfied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkDependencyStartingWith(InternalEquinoxTask<?> task, Path xmlFile, Element root, Element element, String dependencyName, String... targetElementNamePrefixes) throws Exception {

		// get source element
		Element sourceElement = element.getChild(dependencyName);

		// source element not found
		if (sourceElement == null) {
			task.addWarning("Cannot locate element '" + dependencyName + "' under " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get source id
		String sourceId = sourceElement.getTextNormalize();

		// empty id
		if (sourceId.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// search for referenced element
		for (Element targetElement : root.getChildren()) {
			for (String prefix : targetElementNamePrefixes) {
				if (targetElement.getName().startsWith(prefix)) {
					Element id = targetElement.getChild("id");
					if (id != null) {
						if (sourceId.equals(id.getTextNormalize()))
							return true;
					}
				}
			}
		}

		// referenced element not found
		task.addWarning("Cannot find element with task id '" + sourceId + "' which appears to be a dependency of " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
		return false;
	}

	/**
	 * Returns true if given element has at least 1 valid search keyword value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if given element has at least 1 valid search keyword value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkSearchKeywords(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get value of element
		String keywords = element.getTextNormalize();

		// empty value
		if (keywords.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// multiple keywords
		if (keywords.contains(",")) {

			// create tokens
			ArrayList<String> tokens = new ArrayList<>();
			StringTokenizer st = new StringTokenizer(keywords, ",");
			while (st.hasMoreTokens()) {
				String word = st.nextToken().trim();
				if (!word.isEmpty()) {
					tokens.add(word);
				}
			}

			// no keywords found
			if (tokens.isEmpty()) {
				task.addWarning("no valid keyword supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
				return false;
			}
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if the given element has a valid task id.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param root
	 *            Root input element.
	 * @param element
	 *            Element to be checked.
	 * @return True if the given element has a valid task id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkElementId(InternalEquinoxTask<?> task, Path xmlFile, Element root, Element element) throws Exception {

		// no task id
		if (element.getChild("id") == null) {
			task.addWarning("No task id specified for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// get id
		String id = element.getChild("id").getTextNormalize();

		// invalid id
		if (id.isEmpty()) {
			task.addWarning("Invalid id specified for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// loop over elements
		for (Element c : root.getChildren()) {

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
				task.addWarning("Non-unique id specified for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
				return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>Path</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @param fileTypes
	 *            List of allowed file types. Can be skipped if all file types are allowed.
	 * @return True if given element has a valid <code>Path</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkInputPathValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement, FileType... fileTypes) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get input path
		Path input = Paths.get(element.getTextNormalize());

		// invalid value
		if (!Files.exists(input)) {
			task.addWarning("Invalid file path supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// check file type
		if (fileTypes != null && fileTypes.length != 0) {
			if (!Arrays.asList(fileTypes).contains(FileType.getFileType(input.toFile()))) {
				task.addWarning("Invalid file type supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Allowed file types are:" + Arrays.toString(fileTypes) + ". Check failed.");
				return false;
			}
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>Path</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @param overwriteFiles
	 *            True to allow overwriting existing files.
	 * @param fileTypes
	 *            List of allowed file types. Can be skipped if all file types are allowed.
	 * @return True if given element has a valid <code>Path</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkOutputPathValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement, boolean overwriteFiles, FileType... fileTypes) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get path
		Path output = Paths.get(element.getTextNormalize());

		// parent directory doesn't exist
		if (!Files.exists(output.getParent())) {
			task.addWarning("Invalid file path supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// check file type
		if (fileTypes != null && fileTypes.length != 0) {
			if (!Arrays.asList(fileTypes).contains(FileType.getFileType(output.toFile()))) {
				task.addWarning("Invalid file type supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Allowed file types are:" + Arrays.toString(fileTypes) + ". Check failed.");
				return false;
			}
		}

		// overwrite is false and file already exists
		if (!overwriteFiles && Files.exists(output)) {
			task.addWarning("Output file supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "' already exists. Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>String</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @param validValues
	 *            List of valid values. Can be skipped for checking only against empty values.
	 * @return True if given element has a valid <code>String</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkStringValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement, String... validValues) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get value of element
		String value = element.getTextNormalize();

		// empty value
		if (value.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// invalid value
		if (validValues != null && validValues.length != 0 && !Arrays.asList(validValues).contains(value)) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Valid values are: " + Arrays.toString(validValues) + ". Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>boolean</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
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
	public static boolean checkBooleanValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get value of element
		String value = element.getTextNormalize();

		// invalid value
		if (!value.equals("true") && !value.equals("false")) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Valid values are 'true' or 'false'. Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>double</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if given element has a valid <code>double</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkDoubleValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// parse value
		try {
			Double.parseDouble(element.getTextNormalize());
		}

		// invalid value
		catch (NumberFormatException e) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Valid values are 'true' or 'false'. Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>int</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if given element has a valid <code>int</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkIntegerValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// parse value
		try {
			Integer.parseInt(element.getTextNormalize());
		}

		// invalid value
		catch (NumberFormatException e) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Valid values are 'true' or 'false'. Check failed.");
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
	public static String getFamilyTree(Element element) throws Exception {
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
	public static String[] getStringArray(Object[] objects) {
		String[] strings = new String[objects.length];
		for (int i = 0; i < objects.length; i++) {
			strings[i] = objects[i].toString();
		}
		return strings;
	}
}