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

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save chart task.
 *
 * @author Murat Artim
 * @date 5 Sep 2018
 * @time 21:53:20
 */
public class SaveChart extends InternalEquinoxTask<Path> implements ShortRunningTask, SingleInputTask<JFreeChart> {

	/** Chart. */
	private JFreeChart chart;

	/** Path to output file. */
	private final Path output;

	/**
	 * Creates save chart task.
	 *
	 * @param chart
	 *            Chart to save. Can be null for automatic execution.
	 * @param output
	 *            Path to output file.
	 */
	public SaveChart(JFreeChart chart, Path output) {
		this.chart = chart;
		this.output = output;
	}

	@Override
	public void setAutomaticInput(JFreeChart input) {
		this.chart = input;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save chart";
	}

	@Override
	protected Path call() throws Exception {

		// update info
		updateMessage("Saving chart...");

		// setup chart dimensions
		int width = 658;
		int height = 597;

		// plot
		ChartUtilities.saveChartAsPNG(output.toFile(), chart, width, height);

		// return output path
		return output;
	}
}