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
package equinox.data.ui;

import java.util.Date;

/**
 * Class for saved instruction set item.
 *
 * @author Murat Artim
 * @date 12 Sep 2018
 * @time 15:10:05
 */
public class SavedInstructionSetItem implements Comparable<SavedInstructionSetItem> {

	/** Instruction set ID. */
	private final int instructionSetId_;

	/** Instruction set file name. */
	private final String fileName_;

	/** Schedule date. */
	private Date date_;

	/**
	 * Creates saved instruction set item.
	 *
	 * @param instructionSetId
	 *            Instruction set ID.
	 * @param fileName
	 *            Instruction set file name.
	 * @param date
	 *            Schedule date (can be null).
	 */
	public SavedInstructionSetItem(int instructionSetId, String fileName, Date date) {
		instructionSetId_ = instructionSetId;
		fileName_ = fileName;
		date_ = date;
	}

	/**
	 * Sets schedule date.
	 *
	 * @param date
	 *            Schedule date.
	 */
	public void setDate(Date date) {
		date_ = date;
	}

	/**
	 * Returns instruction set ID.
	 *
	 * @return Instruction set ID.
	 */
	public int getInstructionSetID() {
		return this.instructionSetId_;
	}

	/**
	 * Returns file name of instruction set.
	 *
	 * @return File name of instruction set.
	 */
	public String getFileName() {
		return fileName_;
	}

	/**
	 * Returns schedule date of task.
	 *
	 * @return Schedule date of task.
	 */
	public Date getDate() {
		return date_;
	}

	@Override
	public String toString() {
		return fileName_ + (date_ == null ? "" : " (" + date_.toString() + ")");
	}

	@Override
	public int compareTo(SavedInstructionSetItem o) {
		return this.date_.compareTo(o.getDate());
	}
}