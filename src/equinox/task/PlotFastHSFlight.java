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

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.ImageViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.SpectrumItem;
import equinox.data.ui.NotificationPanel;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinoxServer.remote.data.Permission;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

/**
 * Class for plot typical flight with highest total stress from fast equivalent stress task.
 *
 * @author Murat Artim
 * @date Jul 10, 2016
 * @time 2:21:56 PM
 */
public class PlotFastHSFlight extends InternalEquinoxTask<Image> implements ShortRunningTask {

	/** Equivalent stress. */
	private final SpectrumItem eqStress_;

	/**
	 * Creates plot typical flight with highest total stress from fast equivalent stress task.
	 *
	 * @param eqStress
	 *            Equivalent stress.
	 */
	public PlotFastHSFlight(SpectrumItem eqStress) {
		eqStress_ = eqStress;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Plot typical flight with highest total stress";
	}

	@Override
	protected Image call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT);

		// update progress info
		updateTitle("Plotting typical flight with highest total stress...");

		// initialize image
		Image image = null;

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get pilot point id
				int id = ((STFFile) eqStress_.getParentItem()).getID();

				// get data
				String sql = "select image from pilot_point_tf_hs where id = " + id;
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						Blob blob = resultSet.getBlob("image");
						if (blob != null) {
							image = new Image(new ByteArrayInputStream(blob.getBytes(1L, (int) blob.length())));
							blob.free();
						}
					}
				}
			}
		}

		// return image
		return image;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get image
			Image image = get();

			// no image found (ask for generation)
			if (image == null) {

				// set title and message
				String title = "Generate Typical Flight with Highest Total Stress";
				String message = "Typical flight with highest total stress plot is not available. Do you want to generate the plot?";

				// show notification
				NotificationPanel np = taskPanel_.getOwner().getOwner().getNotificationPane();
				np.showQuestion(title, message, "Yes", "No",

						// yes
						new EventHandler<ActionEvent>() {

							@Override
							public void handle(ActionEvent event) {
								np.hide();
								taskPanel_.getOwner().runTaskInParallel(new GenerateHSFlightPlot(eqStress_, true));
							}
						},

						// no
						new EventHandler<ActionEvent>() {

							@Override
							public void handle(ActionEvent event) {
								np.hide();
							}
						});
			}

			// image found (set to image panel)
			else {
				ImageViewPanel panel = (ImageViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.IMAGE_VIEW);
				panel.setView(image);
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.IMAGE_VIEW);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
