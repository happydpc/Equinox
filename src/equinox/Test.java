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
package equinox;

import java.io.IOException;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Test extends Application {
	private GridPane pane;
	private Gauge bugReports;
	private Gauge roadmap;
	private Gauge accessRequests;
	private Gauge dataService;
	private Gauge analysisService;
	private Gauge collaborationService;

	@Override
	public void init() {
		GaugeBuilder<?> builder = GaugeBuilder.create().skinType(SkinType.SLIM);
		bugReports = builder.decimals(0).maxValue(14).unit("OPEN").build();
		roadmap = builder.decimals(0).maxValue(5).unit("OPEN").build();
		accessRequests = builder.decimals(0).maxValue(0).unit("OPEN").build();
		dataService = builder.decimals(0).maxValue(100).unit("% SUCCESS").build();
		analysisService = builder.decimals(0).maxValue(100).unit("% SUCCESS").build();
		collaborationService = builder.decimals(0).maxValue(100).unit("% SUCCESS").build();

		VBox bugsBox = getTopicBox("BUG REPORTS", Color.rgb(77, 208, 225), bugReports);
		VBox roadmapBox = getTopicBox("USER WISHES", Color.rgb(255, 183, 77), roadmap);
		VBox requestsBox = getTopicBox("ACCESS REQUESTS", Color.rgb(229, 115, 115), accessRequests);
		VBox dataBox = getTopicBox("DATA QUERIES", Color.rgb(129, 199, 132), dataService);
		VBox analysisBox = getTopicBox("ANALYSIS REQUESTS", Color.rgb(149, 117, 205), analysisService);
		VBox collaborationBox = getTopicBox("SHARE REQUESTS", Color.rgb(186, 104, 200), collaborationService);

		pane = new GridPane();
		pane.setPadding(new Insets(20));
		pane.setHgap(10);
		pane.setVgap(15);
		pane.setBackground(new Background(new BackgroundFill(Color.rgb(39, 44, 50), CornerRadii.EMPTY, Insets.EMPTY)));
		pane.add(bugsBox, 0, 0);
		pane.add(roadmapBox, 1, 0);
		pane.add(requestsBox, 0, 1);
		pane.add(dataBox, 1, 1);
		pane.add(analysisBox, 0, 2);
		pane.add(collaborationBox, 1, 2);

		Button b = new Button("Start");
		b.setOnAction(x -> {
			bugReports.setValue(13);
			roadmap.setValue(3);
			accessRequests.setValue(0);
			dataService.setValue(98);
			analysisService.setValue(86);
			collaborationService.setValue(100);
		});
		GridPane.setColumnSpan(b, 2);

		pane.add(b, 0, 3);
	}

	@Override
	public void start(Stage stage) {
		Scene scene = new Scene(pane);

		stage.setTitle("Medusa Dashboard");
		stage.setScene(scene);
		stage.show();

	}

	@Override
	public void stop() {
		System.exit(0);
	}

	private static VBox getTopicBox(final String TEXT, final Color COLOR, final Gauge GAUGE) {
		Rectangle bar = new Rectangle(200, 3);
		bar.setArcWidth(6);
		bar.setArcHeight(6);
		bar.setFill(COLOR);

		Label label = new Label(TEXT);
		label.setTextFill(COLOR);
		label.setAlignment(Pos.CENTER);
		label.setPadding(new Insets(0, 0, 10, 0));

		GAUGE.setBarColor(COLOR);
		GAUGE.setBarBackgroundColor(Color.rgb(39, 44, 50));
		GAUGE.setAnimated(true);

		VBox vBox = new VBox(bar, label, GAUGE);
		vBox.setSpacing(3);
		vBox.setAlignment(Pos.CENTER);
		return vBox;
	}

	public static void main(String[] args) throws IOException {
		launch(args);
	}
}