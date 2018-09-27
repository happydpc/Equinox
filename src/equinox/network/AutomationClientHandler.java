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
package equinox.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.plugin.FileType;
import equinox.task.automation.CheckInstructionSet;
import javafx.application.Platform;

/**
 * Class for automation server client handler.
 *
 * @author Murat Artim
 * @date 23 Sep 2018
 * @time 23:34:08
 */
public class AutomationClientHandler implements Runnable {

	/** Automation server. */
	private final AutomationServer server;

	/** Client socket to respond to. */
	private final Socket socket;

	/**
	 * Creates client handler.
	 *
	 * @param server
	 *            Automation server.
	 * @param socket
	 *            Client socket.
	 */
	public AutomationClientHandler(AutomationServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
		Equinox.LOGGER.info("Automation client connection established from '" + socket.getInetAddress().toString() + "'.");
	}

	@Override
	public void run() {

		// initialize variables
		PrintWriter out = null;
		BufferedReader in = null;

		try {

			// create output writer and input reader
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// hand shake with client
			out.println("Connected to automation server version " + Equinox.VERSION.toString() + ". Please supply path to instruction set file (*.xml or *.json).");

			// get input path
			Path inputFile = Paths.get(in.readLine());

			// invalid path
			if (inputFile == null || !Files.exists(inputFile)) {
				String message = "Invalid input file path supplied to automation server. Aborting operation.";
				out.println(message);
				Equinox.LOGGER.warning(message);
				return;
			}

			// invalid file type
			FileType fileType = FileType.getFileType(inputFile.toFile());
			if (fileType == null || !fileType.equals(FileType.XML) && !fileType.equals(FileType.JSON)) {
				String message = "Invalid input file type supplied to automation server. Please supply XML or JSON file. Aborting operation.";
				out.println(message);
				Equinox.LOGGER.warning(message);
				return;
			}

			// run task in JavaFX thread
			Platform.runLater(() -> {
				server.getOwner().getActiveTasksPanel().runTaskInParallel(new CheckInstructionSet(inputFile, CheckInstructionSet.RUN));
			});
			out.println("Job submitted.");
		}

		// exception occurred during process
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during processing client inputs.", e);
			if (out != null) {
				out.println("Exception occurred during processing client inputs. Aborting operation.");
			}
		}

		// clean up
		finally {

			// close streams and client socket
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
				if (socket != null) {
					socket.close();
				}
			}

			// exception occurred during process
			catch (Exception e) {
				Equinox.LOGGER.log(Level.WARNING, "Exception occurred during cleanup after client inputs processed.", e);
			}
		}
	}
}