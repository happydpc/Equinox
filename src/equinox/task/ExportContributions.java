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

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.Equinox;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.task.GetContributionNames.DamageContributionRequester;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinoxServer.remote.data.Permission;

/**
 * Class for export damage contributions task.
 *
 * @author Murat Artim
 * @date 20 Aug 2017
 * @time 22:10:53
 *
 */
public class ExportContributions extends InternalEquinoxTask<Void> implements LongRunningTask, DamageContributionRequester {

	/** Output file. */
	private final File output_;

	/** Spectrum items. This can be either damage contributions or STF file buckets. */
	private final ArrayList<SpectrumItem> items_;

	/** Get contribution names completion indicator. */
	private final AtomicBoolean getNamesCompleted_;

	/** Contribution names. */
	private final AtomicReference<ArrayList<String>> contributionNames_;

	/**
	 * Creates export damage contributions task.
	 *
	 * @param output
	 *            Output file path.
	 * @param items
	 *            Spectrum items. This can be either damage contributions or STF file buckets.
	 */
	public ExportContributions(File output, ArrayList<SpectrumItem> items) {
		output_ = output;
		items_ = items;
		getNamesCompleted_ = new AtomicBoolean();
		contributionNames_ = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Export damage contributions";
	}

	@Override
	public void setContributions(ArrayList<String> contributions) {
		contributionNames_.set(contributions);
		getNamesCompleted_.set(true);
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.EXPORT_DAMAGE_CONTRIBUTIONS);

		// update info
		updateMessage("Getting damage contribution names...");

		// get contribution names and wait
		Equinox.SUBTASK_THREADPOOL.submit(new GetContributionNames(this, items_));
		waitForTask();

		// get names
		ArrayList<String> names = contributionNames_.get();

		// couldn't get contribution names
		if ((names == null) || names.isEmpty())
			throw new Exception("Couldn't get common contribution names. Aborting export process.");

		// create options
		boolean[] options = new boolean[17];
		options[SaveDamageContributions.PERCENT] = true;
		options[SaveDamageContributions.FULL] = false;
		options[SaveDamageContributions.INC] = true;
		options[SaveDamageContributions.ONEG] = true;
		options[SaveDamageContributions.GAG] = true;
		options[SaveDamageContributions.DP] = true;
		options[SaveDamageContributions.DT] = true;
		options[SaveDamageContributions.MAT_NAME] = false;
		options[SaveDamageContributions.FAT_P] = false;
		options[SaveDamageContributions.FAT_Q] = false;
		options[SaveDamageContributions.PP_NAME] = true;
		options[SaveDamageContributions.EID] = false;
		options[SaveDamageContributions.SPEC_NAME] = true;
		options[SaveDamageContributions.PROGRAM] = true;
		options[SaveDamageContributions.SECTION] = true;
		options[SaveDamageContributions.MISSION] = true;
		options[SaveDamageContributions.OMISSION] = false;

		// initialize task
		InternalEquinoxTask<?> task = null;

		// check if bucket STF files are given as input
		boolean isBucketSTF = false;
		for (SpectrumItem item : items_) {
			if (item instanceof STFFileBucket) {
				isBucketSTF = true;
				break;
			}
		}

		// STF file bucket
		if (isBucketSTF) {

			// cast to STF file bucket array
			ArrayList<STFFileBucket> buckets = new ArrayList<>();
			for (SpectrumItem item : items_) {
				buckets.add((STFFileBucket) item);
			}

			// create save task
			task = new SaveBucketDamageContributions(buckets, names, options, output_);
		}

		// loadcase damage contributions
		else {

			// cast to loadcase damage contributions
			ArrayList<LoadcaseDamageContributions> contributions = new ArrayList<>();
			for (SpectrumItem item : items_) {
				contributions.add((LoadcaseDamageContributions) item);
			}

			// create save task
			task = new SaveDamageContributions(contributions, names, options, output_);
		}

		// update info
		updateMessage("Saving damage contributions...");

		// submit task
		Equinox.SUBTASK_THREADPOOL.submit(task).get();

		// return
		return null;
	}

	/**
	 * Waits for sub processes to complete.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void waitForTask() throws Exception {

		// loop while analysis is running
		while (!getNamesCompleted_.get()) {

			// task cancelled
			if (isCancelled())
				return;

			// sleep a bit
			try {
				Thread.sleep(500);
			}

			// task interrupted
			catch (InterruptedException e) {
				if (isCancelled())
					return;
			}
		}
	}
}
