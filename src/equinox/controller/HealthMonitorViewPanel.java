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
import eu.hansolo.tilesfx.addons.Indicator;
import eu.hansolo.tilesfx.chart.ChartDataBuilder;
import eu.hansolo.tilesfx.chart.TilesFXSeries;
import eu.hansolo.tilesfx.skins.BarChartItem;
import eu.hansolo.tilesfx.skins.LeaderBoardItem;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import eu.hansolo.tilesfx.tools.Location;
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
	public static final int ONLINE_USERS = 0, DATA_QUERIES = 1, ANALYSIS_REQUESTS = 2, COLLABORATION_REQUESTS = 3, POPULAR_SEARCH_HITS = 4, POPULAR_DOWNLOADS = 5, SPECTRUM_COUNT = 6, PP_COUNT = 7, BUG_REPORTS = 8, USER_WISHES = 9, USER_LOCATIONS = 10;

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

		// @formatter:off
		// create dashboard tiles
		tiles_ = new Tile[11];

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

		// create data queries tile
		XYChart.Series<String, Number> dataQueriesSeries = new XYChart.Series<>();
		dataQueriesSeries.setName("Data Queries");
		XYChart.Series<String, Number> failedQueriesSeries = new XYChart.Series<>();
		failedQueriesSeries.setName("Failed Queries");
		TilesFXSeries<String, Number> dataQueriesFXSeries = new TilesFXSeries<>(dataQueriesSeries, Tile.BLUE, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Tile.BLUE), new Stop(1, Color.TRANSPARENT)));
		TilesFXSeries<String, Number> failedQueriesFXSeries = new TilesFXSeries<>(failedQueriesSeries, Tile.ORANGE, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Tile.ORANGE), new Stop(1, Color.TRANSPARENT)));
		tiles_[DATA_QUERIES] = TileBuilder.create()
		       .skinType(SkinType.SMOOTHED_CHART)
		       .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
		       .title("Data Queries")
		       .chartType(ChartType.AREA)
		       //.animated(true)
		       .smoothing(true)
		       .tilesFxSeries(dataQueriesFXSeries, failedQueriesFXSeries)
		       .build();
		tiles_[DATA_QUERIES].getXAxis().setTickLabelsVisible(false);

		// create analysis requests tile
		XYChart.Series<String, Number> analysisRequestsSeries = new XYChart.Series<>();
		analysisRequestsSeries.setName("Analysis Requests");
		XYChart.Series<String, Number> failedAnalysesSeries = new XYChart.Series<>();
		failedAnalysesSeries.setName("Failed Analyses");
		TilesFXSeries<String, Number> analysisRequestsFXSeries = new TilesFXSeries<>(analysisRequestsSeries, Tile.BLUE, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Tile.BLUE), new Stop(1, Color.TRANSPARENT)));
		TilesFXSeries<String, Number> failedAnalysesFXSeries = new TilesFXSeries<>(failedAnalysesSeries, Tile.ORANGE, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Tile.ORANGE), new Stop(1, Color.TRANSPARENT)));
		tiles_[ANALYSIS_REQUESTS] = TileBuilder.create()
                .skinType(SkinType.SMOOTHED_CHART)
                .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
                .title("Analysis Requests")
                .chartType(ChartType.AREA)
                //.animated(true)
                .smoothing(true)
                .tilesFxSeries(analysisRequestsFXSeries, failedAnalysesFXSeries)
                .build();
		tiles_[ANALYSIS_REQUESTS].getXAxis().setTickLabelsVisible(false);

		// create collaboration requests tile
		XYChart.Series<String, Number> shareRequestsSeries = new XYChart.Series<>();
		shareRequestsSeries.setName("Share Requests");
		XYChart.Series<String, Number> failedSharesSeries = new XYChart.Series<>();
		failedSharesSeries.setName("Failed Shares");
		TilesFXSeries<String, Number> shareRequestsFXSeries = new TilesFXSeries<>(shareRequestsSeries, Tile.BLUE, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Tile.BLUE), new Stop(1, Color.TRANSPARENT)));
		TilesFXSeries<String, Number> failedSharesFXSeries = new TilesFXSeries<>(failedSharesSeries, Tile.ORANGE, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Tile.ORANGE), new Stop(1, Color.TRANSPARENT)));
		tiles_[COLLABORATION_REQUESTS] = TileBuilder.create()
		       .skinType(SkinType.SMOOTHED_CHART)
		       .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
		       .title("Collaboration Requests")
		       .chartType(ChartType.AREA)
		       //.animated(true)
		       .smoothing(true)
		       .tilesFxSeries(shareRequestsFXSeries, failedSharesFXSeries)
		       .build();
		tiles_[COLLABORATION_REQUESTS].getXAxis().setTickLabelsVisible(false);

		// create popular search hits tile
		LeaderBoardItem leaderBoardItem1 = new LeaderBoardItem("Gerrit", 47);
		LeaderBoardItem leaderBoardItem2 = new LeaderBoardItem("Sandra", 43);
		LeaderBoardItem leaderBoardItem3 = new LeaderBoardItem("Lilli", 12);
		LeaderBoardItem leaderBoardItem4 = new LeaderBoardItem("Anton", 8);
		tiles_[POPULAR_SEARCH_HITS] = TileBuilder.create()
                .skinType(SkinType.LEADER_BOARD)
                .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
                .title("Popular Search Hits")
                .leaderBoardItems(leaderBoardItem1, leaderBoardItem2, leaderBoardItem3, leaderBoardItem4)
                .build();

		// create popular downloads tile
		tiles_[POPULAR_DOWNLOADS] = TileBuilder.create()
		        .skinType(SkinType.LEADER_BOARD)
		        .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
		        .title("Popular Downloads")
		        .leaderBoardItems(leaderBoardItem1, leaderBoardItem2, leaderBoardItem3, leaderBoardItem4)
		        .build();

				// create spectrum count tile
				BarChartItem barChartItem1 = new BarChartItem("Gerrit", 47, Tile.BLUE);
				BarChartItem barChartItem2 = new BarChartItem("Sandra", 43, Tile.RED);
				BarChartItem barChartItem3 = new BarChartItem("Lilli", 12, Tile.GREEN);
				BarChartItem barChartItem4 = new BarChartItem("Anton", 8, Tile.ORANGE);
				tiles_[SPECTRUM_COUNT] = TileBuilder.create()
                        .skinType(SkinType.BAR_CHART)
                        .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
                        .title("Spectrum Count")
                        .barChartItems(barChartItem1, barChartItem2, barChartItem3, barChartItem4)
                        .decimals(0)
                        .build();

				// create pilot point count tile
				BarChartItem barChartItem5 = new BarChartItem("Gerrit", 47, Tile.BLUE);
				BarChartItem barChartItem6 = new BarChartItem("Sandra", 43, Tile.RED);
				BarChartItem barChartItem7 = new BarChartItem("Lilli", 12, Tile.GREEN);
				BarChartItem barChartItem8 = new BarChartItem("Anton", 8, Tile.ORANGE);
				tiles_[PP_COUNT] = TileBuilder.create()
                        .skinType(SkinType.BAR_CHART)
                        .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
                        .title("Pilot Point Count")
                        .barChartItems(barChartItem5, barChartItem6, barChartItem7, barChartItem8)
                        .decimals(0)
                        .build();


				// create bug reports tile
		Indicator bugOpen = new Indicator(Tile.RED);
        bugOpen.setOn(true);
        Indicator bugProgress = new Indicator(Tile.YELLOW);
        bugProgress.setOn(true);
        Indicator bugClosed = new Indicator(Tile.GREEN);
        bugClosed.setOn(true);
        tiles_[BUG_REPORTS] = TileBuilder.create()
                .skinType(SkinType.STATUS)
                .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
                .title("Bug Reports")
                .description("Status")
                .leftText("OPEN")
                .middleText("PROGRESS")
                .rightText("CLOSED")
                .leftGraphics(bugOpen)
                .middleGraphics(bugProgress)
                .rightGraphics(bugClosed)
                .build();

        // create user wishes tile
        Indicator wishOpen = new Indicator(Tile.RED);
        wishOpen.setOn(true);
        Indicator wishProgress = new Indicator(Tile.YELLOW);
        wishProgress.setOn(true);
        Indicator wishClosed = new Indicator(Tile.GREEN);
        wishClosed.setOn(true);
        tiles_[USER_WISHES] = TileBuilder.create()
                .skinType(SkinType.STATUS)
                .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
                .title("User Wishes")
                .description("Status")
                .leftText("OPEN")
                .middleText("PROGRESS")
                .rightText("CLOSED")
                .leftGraphics(wishOpen)
                .middleGraphics(wishProgress)
                .rightGraphics(wishClosed)
                .build();

        // create connection map tile
        Location SanFranciso = new Location(37.7576171, -122.5776844, "San Francisco", Color.MAGENTA);
        Location NewYork     = new Location(40.7157216,-74.3036411, "New York", Color.MAGENTA);
        Location Chicago     = new Location(41.8333908,-88.0128341, "Chicago", Color.MAGENTA);
        Location Home        = new Location(51.9065938,7.6352688, "Hause", Color.CRIMSON);
        Location Moscow      = new Location(55.751042, 37.619060, "Moscow", Color.MAGENTA);
        tiles_[USER_LOCATIONS] = TileBuilder.create()
                               .skinType(SkinType.WORLDMAP)
                               .title("User Locations")
                               .textVisible(false)
                               .maxSize(Double.MAX_VALUE, Double.MAX_VALUE)
                               .pointsOfInterest(SanFranciso, Chicago, NewYork, Moscow)
                               .chartData(ChartDataBuilder.create()
                                                          .name("Home")
                                                          .fillColor(Color.RED)
                                                          .value(20)
                                                          .location(Home)
                                                          .build())
                               .build();
        FlowGridPane.setColumnSpan(tiles_[USER_LOCATIONS], 2);

		// @formatter:on

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

			// data service
			if (dataServerStats != null) {
				tiles_[ONLINE_USERS].getTilesFXSeries().get(0).getSeries().getData().clear();
				tiles_[DATA_QUERIES].getTilesFXSeries().get(0).getSeries().getData().clear();
				tiles_[DATA_QUERIES].getTilesFXSeries().get(1).getSeries().getData().clear();
				int k = 0;
				for (DataServerStatistic stat : dataServerStats) {

					tiles_[ONLINE_USERS].getTilesFXSeries().get(0).getSeries().getData().add(new XYChart.Data<>(stat.getRecorded().toString(), stat.getClients()));
					tiles_[DATA_QUERIES].getTilesFXSeries().get(0).getSeries().getData().add(new XYChart.Data<>(stat.getRecorded().toString(), stat.getQueries()));
					tiles_[DATA_QUERIES].getTilesFXSeries().get(1).getSeries().getData().add(new XYChart.Data<>(stat.getRecorded().toString(), stat.getFailedQueries()));
					k++;
					if (k > 23) {
						break;
					}
				}
			}

			// analysis server statistics
			if (analysisServerStats != null) {
				tiles_[ANALYSIS_REQUESTS].getTilesFXSeries().get(0).getSeries().getData().clear();
				tiles_[ANALYSIS_REQUESTS].getTilesFXSeries().get(1).getSeries().getData().clear();
				for (AnalysisServerStatistic stat : analysisServerStats) {
					tiles_[ANALYSIS_REQUESTS].getTilesFXSeries().get(0).getSeries().getData().add(new XYChart.Data<>(stat.getRecorded().toString(), stat.getAnalysisRequests()));
					tiles_[ANALYSIS_REQUESTS].getTilesFXSeries().get(1).getSeries().getData().add(new XYChart.Data<>(stat.getRecorded().toString(), stat.getFailedAnalyses()));
				}
			}

			// exchange server statistics
			if (exchangeServerStats != null) {
				tiles_[COLLABORATION_REQUESTS].getTilesFXSeries().get(0).getSeries().getData().clear();
				tiles_[COLLABORATION_REQUESTS].getTilesFXSeries().get(1).getSeries().getData().clear();
				for (ExchangeServerStatistic stat : exchangeServerStats) {
					tiles_[COLLABORATION_REQUESTS].getTilesFXSeries().get(0).getSeries().getData().add(new XYChart.Data<>(stat.getRecorded().toString(), stat.getShareRequests()));
					tiles_[COLLABORATION_REQUESTS].getTilesFXSeries().get(1).getSeries().getData().add(new XYChart.Data<>(stat.getRecorded().toString(), stat.getFailedShares()));
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