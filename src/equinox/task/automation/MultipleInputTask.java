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

import equinox.task.InternalEquinoxTask;

/**
 *
 * Interface for multiple input automatic task. Multiple input tasks collect inputs from multiple sources and <u>execute themselves</u> once all inputs are collected. It is up to the task to decide whether it will execute itself if not all inputs arrive.
 *
 * @author Murat Artim
 * @date 30 Aug 2018
 * @time 13:35:28
 * @param <V>
 *            Input class.
 */
public interface MultipleInputTask<V> extends AutomaticTask<V> {

	/**
	 * Sets input threshold. Once the threshold is reached, this task will execute itself.
	 *
	 * @param inputThreshold
	 *            Threshold to set.
	 */
	void setInputThreshold(int inputThreshold);

	/**
	 * Adds automatic task input. Note that, once the number of inputs reaches the input threshold, this task will execute itself.
	 *
	 * @param task
	 *            Source task (i.e. the caller).
	 * @param input
	 *            Input.
	 * @param executeInParallel
	 *            True to execute this task in parallel mode (if the above mentioned condition is met).
	 */
	void addAutomaticInput(AutomaticTaskOwner<V> task, V input, boolean executeInParallel);

	/**
	 * Notifies this task that an input failed to arrive. Source tasks would normally call this method from their <code>failed</code> or <code>canceled</code> methods. The implementation could do one of the following:
	 * <UL>
	 * <LI>Decrement the input threshold (i.e. this task will be executed with less inputs than initially intended) and execute the task if the number of inputs is equal to the input threshold or,
	 * <LI>Keep input threshold unchanged (i.e. this task would never execute).
	 * </UL>
	 * The decision depends on the nature of this task; whether all inputs should be available for running the task or not.
	 *
	 * @param task
	 *            Source task (i.e. the caller).
	 * @param executeInParallel
	 *            True to execute this task in parallel mode (if the above mentioned condition is met).
	 */
	void inputFailed(AutomaticTaskOwner<V> task, boolean executeInParallel);

	/**
	 * Called by implementing classes as a default implementation of <code>addAutomaticInput</code> method.
	 *
	 * @param task
	 *            Source task.
	 * @param input
	 *            Input.
	 * @param executeInParallel
	 *            True to execute this task in parallel mode.
	 * @param inputList
	 *            List of inputs to add the given input.
	 * @param inputThreshold
	 *            Input threshold.
	 */
	default void automaticInputAdded(AutomaticTaskOwner<V> task, V input, boolean executeInParallel, List<V> inputList, int inputThreshold) {

		// synchronize on input list
		synchronized (inputList) {

			// add input
			inputList.add(input);

			// input threshold reached
			if (inputList.size() == inputThreshold) {

				// execute in parallel
				if (executeInParallel) {
					((InternalEquinoxTask<?>) task).getTaskPanel().getOwner().runTaskInParallel((InternalEquinoxTask<?>) this);
				}

				// execute sequentially
				else {
					((InternalEquinoxTask<?>) task).getTaskPanel().getOwner().runTaskSequentially((InternalEquinoxTask<?>) this);
				}
			}
		}
	}

	/**
	 * Called by implementing classes as a default implementation of <code>inputFailed</code> method.
	 * 
	 * @param task
	 *            Source task.
	 * @param executeInParallel
	 *            True to execute this task in parallel mode.
	 * @param inputList
	 *            List of inputs to add the given input.
	 * @param inputThreshold
	 *            Input threshold.
	 * @return The updated input threshold.
	 */
	default int automaticInputFailed(AutomaticTaskOwner<V> task, boolean executeInParallel, List<V> inputList, int inputThreshold) {

		// decrement threshold
		inputThreshold--;

		// input threshold reached
		if (inputList.size() == inputThreshold && inputThreshold >= 2) {

			// execute in parallel
			if (executeInParallel) {
				((InternalEquinoxTask<?>) task).getTaskPanel().getOwner().runTaskInParallel((InternalEquinoxTask<?>) this);
			}

			// execute sequentially
			else {
				((InternalEquinoxTask<?>) task).getTaskPanel().getOwner().runTaskSequentially((InternalEquinoxTask<?>) this);
			}
		}

		// return input threshold
		return inputThreshold;
	}
}