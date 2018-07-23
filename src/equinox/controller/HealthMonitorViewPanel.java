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
package equinox.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import equinox.analysisServer.remote.data.AnalysisServerStatistic;
import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.DataServerStatistic;
import equinox.exchangeServer.remote.data.ExchangeServerStatistic;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.GetServerDiagnostics;
import equinox.task.SaveImage;
import equinox.utility.Animator;
import equinox.utility.Utility;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.ChartType;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.chart.TilesFXSeries;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.FileChooser;

/**
 * Class for platform health monitoring view panel controller.
 *
 * @author Murat Artim
 * @date 18 Jul 2018
 * @time 23:35:08
 */
public class HealthMonitorViewPanel implements InternalViewSubPanel {

	/** Tile index. */
	public static final int ONLINE_USERS = 0, ANALYSIS_REQUESTS = 1, SUCCESSFUL_ANALYSES = 2, FAILED_ANALYSES = 3, DATA_QUERIES = 4, SUCCESSFUL_QUERIES = 5, FAILED_QUERIES = 6, SHARE_REQUESTS = 7, SUCCESSFUL_SHARES = 8, FAILED_SHARES = 9;

	/** The owner panel. */
	private ViewPanel owner_;

	/** Dashboard tiles. */
	private Tile[] tiles_;

	/** Tile grid pane. */
	private FlowGridPane pane_;

	/** Control panel. */
	private HealthMonitorViewControls controls_;

	@FXML
	private VBox root_;

	@FXML
	private HBox header_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create controls
		controls_ = HealthMonitorViewControls.load(this);

		// create dashboard tiles
		// @formatter:off
		tiles_ = new Tile[10];

        // create online users tile
        XYChart.Series<String, Number> onlieUsersSeries = new XYChart.Series<>();
        onlieUsersSeries.setName("Online Users");
		TilesFXSeries<String, Number> onlineUsersTilesFXSeries = new TilesFXSeries<>(onlieUsersSeries, Tile.BLUE, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Tile.BLUE), new Stop(1, Color.TRANSPARENT)));
		tiles_[ONLINE_USERS] = TileBuilder.create()
                .skinType(SkinType.SMOOTHED_CHART)
                .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
                .title("Online Users")
                .chartType(ChartType.AREA)
                //.animated(true)
                .smoothing(true)
                .tilesFxSeries(onlineUsersTilesFXSeries)
                .build();
		tiles_[ONLINE_USERS].getXAxis().setTickLabelsVisible(false);

		tiles_[ANALYSIS_REQUESTS] = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
				.title("Analysis Requests")
				.unit("analyses")
				.smoothing(true)
				.build();
		tiles_[SUCCESSFUL_ANALYSES] = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
				.title("Successful Analyses")
				.unit("analyses")
				.smoothing(true)
				.build();
		tiles_[FAILED_ANALYSES] = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
				.title("Failed Analyses")
				.unit("analyses")
				.smoothing(true)
				.build();
		tiles_[DATA_QUERIES] = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
				.title("Data Queries")
				.unit("queries")
				.smoothing(true)
				.build();
		tiles_[SUCCESSFUL_QUERIES] = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
				.title("Successful Queries")
				.unit("queries")
				.smoothing(true)
				.build();
		tiles_[FAILED_QUERIES] = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
				.title("Failed Queries")
				.unit("queries")
				.smoothing(true)
				.build();
		tiles_[SHARE_REQUESTS] = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
				.title("Share Requests")
				.unit("shares")
				.smoothing(true)
				.build();
		tiles_[SUCCESSFUL_SHARES] = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
				.title("Successful Shares")
				.unit("shares")
				.smoothing(true)
				.build();
		tiles_[FAILED_SHARES] = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
				.title("Failed Shares")
				.unit("shares")
				.smoothing(true)
				.build();

//		Indicator bugOpen = new Indicator(Tile.RED);
//        bugOpen.setOn(true);
//        Indicator bugProgress = new Indicator(Tile.YELLOW);
//        bugProgress.setOn(true);
//        Indicator bugClosed = new Indicator(Tile.GREEN);
//        bugClosed.setOn(true);
//        tiles_[BUG_REPORTS] = TileBuilder.create()
//                .skinType(SkinType.STATUS)
//                .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
//                .title("Bug Reports")
//                .description("Status")
//                .leftText("OPEN")
//                .middleText("PROGRESS")
//                .rightText("CLOSED")
//                .leftGraphics(bugOpen)
//                .middleGraphics(bugProgress)
//                .rightGraphics(bugClosed)
//                .build();
		// @formatter:on

		// TODO set averaging period
		// Arrays.asList(tiles_).forEach(x -> x.setAveragingPeriod(30));

		// create and setup dashboard grid pane
		pane_ = new FlowGridPane(4, 3, tiles_);
		pane_.setHgap(5);
		pane_.setVgap(5);
		pane_.setAlignment(Pos.CENTER);
		pane_.setCenterShape(true);
		pane_.setPadding(new Insets(5));
		pane_.setBackground(new Background(new BackgroundFill(Color.web("#101214"), CornerRadii.EMPTY, Insets.EMPTY)));
		VBox.setVgrow(pane_, Priority.ALWAYS);
		pane_.setMaxWidth(Double.MAX_VALUE);
		pane_.setMaxHeight(Double.MAX_VALUE);
		root_.getChildren().add(pane_);
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public HBox getControls() {
		return controls_.getRoot();
	}

	@Override
	public String getHeader() {
		return "Platform Health Monitoring";
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Platform Health Monitoring" + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = root_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	@Override
	public String getViewName() {
		return "Platform Health Monitoring";
	}

	@Override
	public WritableImage getViewImage() {
		return root_.snapshot(null, null);
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public void hiding() {
		// no implementation
	}

	/**
	 * Gets server diagnostics.
	 */
	public void getServerDiagnostics() {
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetServerDiagnostics(controls_.getPeriod()));
	}

	/**
	 * Sets data from platform services.
	 *
	 * @param dataServerStats
	 *            Data server statistics.
	 * @param analysisServerStats
	 *            Analysis server statistics.
	 * @param exchangeServerStats
	 *            Exchange server statistics.
	 */
	public void setData(DataServerStatistic[] dataServerStats, AnalysisServerStatistic[] analysisServerStats, ExchangeServerStatistic[] exchangeServerStats) {

		// create and play fade animation
		Animator.fade(true, 200, 500, event -> {

			// data server statistics
			if (dataServerStats != null) {
				tiles_[ONLINE_USERS].getTilesFXSeries().get(0).getSeries().getData().clear();
				for (DataServerStatistic stat : dataServerStats) {
					tiles_[ONLINE_USERS].getTilesFXSeries().get(0).getSeries().getData().add(new XYChart.Data<>(stat.getRecorded().toString(), stat.getClients()));
					tiles_[DATA_QUERIES].setValue(stat.getQueries());
					tiles_[SUCCESSFUL_QUERIES].setValue(stat.getSuccessfulQueries());
					tiles_[FAILED_QUERIES].setValue(stat.getFailedQueries());
				}
			}

			// analysis server statistics
			if (analysisServerStats != null) {
				for (AnalysisServerStatistic stat : analysisServerStats) {
					tiles_[ANALYSIS_REQUESTS].setValue(stat.getAnalysisRequests());
					tiles_[SUCCESSFUL_ANALYSES].setValue(stat.getSuccessfulAnalyses());
					tiles_[FAILED_ANALYSES].setValue(stat.getFailedAnalyses());
				}
			}

			// exchange server statistics
			if (exchangeServerStats != null) {
				for (ExchangeServerStatistic stat : exchangeServerStats) {
					tiles_[SHARE_REQUESTS].setValue(stat.getShareRequests());
					tiles_[SUCCESSFUL_SHARES].setValue(stat.getSuccessfulShares());
					tiles_[FAILED_SHARES].setValue(stat.getFailedShares());
				}
			}
		}, header_, pane_).play();
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static HealthMonitorViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("HealthMonitorViewPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			HealthMonitorViewPanel controller = (HealthMonitorViewPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}