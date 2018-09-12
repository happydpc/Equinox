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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.ui.SavedInstructionSetItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.collections.ObservableList;

/**
 * Class for delete instruction sets task.
 *
 * @author Murat Artim
 * @date 12 Sep 2018
 * @time 15:19:27
 */
public class DeleteInstructionSets extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Instruction sets to delete. */
	private final SavedInstructionSetItem[] instructionSets_;

	/** True if instruction sets are scheduled. */
	private final boolean isScheduled_;

	/**
	 * Creates delete instruction sets task.
	 *
	 * @param instructionSets
	 *            Instruction sets to delete.
	 * @param isScheduled
	 *            True if instruction sets are scheduled.
	 */
	public DeleteInstructionSets(ObservableList<SavedInstructionSetItem> instructionSets, boolean isScheduled) {
		instructionSets_ = new SavedInstructionSetItem[instructionSets.size()];
		for (int i = 0; i < instructionSets.size(); i++) {
			instructionSets_[i] = instructionSets.get(i);
		}
		isScheduled_ = isScheduled;
	}

	/**
	 * Creates delete instruction sets task.
	 *
	 * @param instructionSets
	 *            Instruction sets to delete.
	 * @param isScheduled
	 *            True if instruction sets are scheduled.
	 */
	public DeleteInstructionSets(ArrayList<SavedInstructionSetItem> instructionSets, boolean isScheduled) {
		instructionSets_ = new SavedInstructionSetItem[instructionSets.size()];
		for (int i = 0; i < instructionSets.size(); i++) {
			instructionSets_[i] = instructionSets.get(i);
		}
		isScheduled_ = isScheduled;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Delete " + (isScheduled_ ? "scheduled" : "saved") + " instruction sets";
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Deleting " + (isScheduled_ ? "scheduled" : "saved") + " instruction sets");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// prepare statement
				String sql = "delete from saved_instruction_sets where id = ?";
				try (PreparedStatement statement = connection.prepareStatement(sql)) {

					// loop over instruction sets
					for (SavedInstructionSetItem instructionSet : instructionSets_) {

						// delete
						statement.setInt(1, instructionSet.getInstructionSetID());
						statement.executeUpdate();
					}
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return
				return null;
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// saved instruction set
		if (!isScheduled_) {
			taskPanel_.getOwner().getOwner().getSavedInstructionSetsPanel().getSavedInstructionSets().getItems().removeAll(instructionSets_);
		}
		else {
			taskPanel_.getOwner().getOwner().getScheduledInstructionSetsPanel().getScheduledInstructionSets().getItems().removeAll(instructionSets_);
		}
	}
}
