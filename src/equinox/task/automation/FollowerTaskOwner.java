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

import java.util.List;

/**
 * Interface for follower task owners.
 *
 * @author Murat Artim
 * @date 28 Aug 2018
 * @time 09:43:06
 */
public interface FollowerTaskOwner extends AutomaticTaskOwner {

	/**
	 * Adds follower task.
	 *
	 * @param task
	 *            Task to add.
	 */
	void addFollowerTask(FollowerTask task);

	/**
	 * Returns a list containing the follower tasks or null if no follower tasks are defined.
	 *
	 * @return List containing the follower tasks or null if no follower tasks are defined.
	 */
	List<FollowerTask> getFollowerTasks();

	default void
}