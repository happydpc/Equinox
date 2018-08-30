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
import java.util.ArrayList;
import java.util.List;

import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.SharedFileInfo;
import equinox.task.InternalEquinoxTask.FileSharingTask;
import equinox.task.automation.SingleInputTask;
import equinox.task.automation.FollowerTask;
import equinox.task.automation.FollowerTaskOwner;
import equinox.task.serializableTask.SerializableShareGeneratedItem;
import equinox.utility.Utility;

/**
 * Class for share generated item task.
 *
 * @author Murat Artim
 * @date Sep 23, 2014
 * @time 6:19:36 PM
 */
public class ShareGeneratedItem extends TemporaryFileCreatingTask<Void> implements SavableTask, FileSharingTask, SingleInputTask<Path>, FollowerTaskOwner {

	/** File to share. */
	private Path file_;

	/** Recipients. */
	private final List<String> recipients_;

	/** Automatic tasks. */
	private List<FollowerTask> followerTasks_ = null;

	/** Automatic task execution mode. */
	private boolean executeAutomaticTasksInParallel_ = true;

	/**
	 * Creates share generated item task.
	 *
	 * @param file
	 *            File to share. Can be null for automatic execution.
	 * @param recipients
	 *            Recipients.
	 */
	public ShareGeneratedItem(Path file, List<String> recipients) {
		file_ = file;
		recipients_ = recipients;
	}

	@Override
	public void setAutomaticTaskExecutionMode(boolean isParallel) {
		executeAutomaticTasksInParallel_ = isParallel;
	}

	@Override
	public void addFollowerTask(FollowerTask task) {
		if (followerTasks_ == null) {
			followerTasks_ = new ArrayList<>();
		}
		followerTasks_.add(task);
	}

	@Override
	public List<FollowerTask> getFollowerTasks() {
		return followerTasks_;
	}

	@Override
	public void setAutomaticInput(Path input) {
		file_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Share file";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableShareGeneratedItem(file_, recipients_);
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SHARE_FILE);

		// update progress info
		updateTitle("Sharing file...");

		// zip file
		String name = FileType.getNameWithoutExtension(file_);
		Path output = getWorkingDirectory().resolve(FileType.appendExtension(name, FileType.ZIP));
		Utility.zipFile(file_, output.toFile(), this);

		// upload file to filer
		shareFile(output, recipients_, SharedFileInfo.FILE);
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// execute follower tasks
		if (followerTasks_ != null) {
			for (FollowerTask task : followerTasks_) {
				if (executeAutomaticTasksInParallel_) {
					taskPanel_.getOwner().runTaskInParallel((InternalEquinoxTask<?>) task);
				}
				else {
					taskPanel_.getOwner().runTaskSequentially((InternalEquinoxTask<?>) task);
				}
			}
		}
	}
}
