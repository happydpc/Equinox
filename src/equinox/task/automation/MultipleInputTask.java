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
public interface MultipleInputTask<V> extends AutomaticTask {

	/**
	 * Sets input threshold. Once the threshold is reached, this task will execute itself.
	 *
	 * @param inputThreshold
	 *            Threshold to set.
	 */
	void setInputThreshold(int inputThreshold);

	/**
	 * Adds automatic task input.
	 *
	 * @param input
	 *            Input.
	 */
	void addAutomaticInput(V input);

	/**
	 * Notifies this task that an input failed to arrive. This would decrement the input threshold.
	 */
	void inputFailed();
}