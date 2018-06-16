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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import equinox.Equinox;
import equinox.controller.MainScreen;
import equinox.data.Settings;
import equinox.utility.Utility;
import equinoxServer.remote.Registry;
import equinoxServer.remote.listener.AnalysisListener;
import equinoxServer.remote.listener.DatabaseQueryListener;
import equinoxServer.remote.listener.StandardMessageListener;
import equinoxServer.remote.message.AnalysisMessage;
import equinoxServer.remote.message.DatabaseQueryMessage;
import equinoxServer.remote.utility.BigMessage;
import equinoxServer.remote.utility.NetworkMessage;
import equinoxServer.remote.utility.PartialMessage;
import equinoxServer.remote.utility.SplitMessage;
import javafx.application.Platform;

/**
 * Class for network watcher. Network watcher listens for server messages and transfers them to registered listeners.
 *
 * @author Murat Artim
 *
 */
public class NetworkWatcher {

	/** The kryonet client of network watcher. */
	private final Client kryoNetClient_;

	/** The owner of the network watcher. */
	private final MainScreen owner_;

	/** Server listener of the network watcher. */
	private volatile StandardMessageListener listener_;

	/** Analysis listeners. */
	private List<AnalysisListener> analysisListeners_;

	/** Database query listeners. */
	private List<DatabaseQueryListener> databaseQueryListeners_;

	/** Stop indicator of the network watcher. */
	private volatile boolean isStopped_ = false;

	/** The thread executor of the network watcher. */
	private final ExecutorService threadExecutor_;

	/**
	 * Creates network watcher.
	 *
	 * @param owner
	 *            The owner of the network watcher.
	 */
	public NetworkWatcher(MainScreen owner) {

		// set owner
		owner_ = owner;

		// create analysis listener list
		analysisListeners_ = Collections.synchronizedList(new ArrayList<AnalysisListener>());

		// create database query listener list
		databaseQueryListeners_ = Collections.synchronizedList(new ArrayList<DatabaseQueryListener>());

		// create thread executor of the network watcher
		threadExecutor_ = Executors.newSingleThreadExecutor();

		// create kryonet client
		kryoNetClient_ = new Client(65536, 8192);

		// register objects to be sent over network
		Registry.register(kryoNetClient_);

		// add server handler (listener)
		kryoNetClient_.addListener(new ServerHandler());

		// start client
		kryoNetClient_.start();
		Equinox.LOGGER.info("Network watcher initialized.");
	}

	/**
	 * Sets standard message listener.
	 *
	 * @param listener
	 *            Standard message listener to set.
	 */
	public void setStandardMessageListener(StandardMessageListener listener) {
		listener_ = listener;
	}

	/**
	 * Adds given analysis listener to this network watcher. Analysis listener will start receiving analysis messages.
	 *
	 * @param listener
	 *            Analysis listener to add.
	 */
	public void addAnalysisListener(AnalysisListener listener) {
		synchronized (analysisListeners_) {
			analysisListeners_.add(listener);
		}
	}

	/**
	 * Adds given database query listener to this network watcher. Database query listener will start receiving database query messages.
	 *
	 * @param listener
	 *            Database query listener to add.
	 */
	public void addDatabaseQueryListener(DatabaseQueryListener listener) {
		synchronized (databaseQueryListeners_) {
			databaseQueryListeners_.add(listener);
		}
	}

	/**
	 * Removes the given analysis listener from this network watcher. Analysis listener will stop receiving analysis messages.
	 *
	 * @param listener
	 *            Analysis listener to remove.
	 */
	public void removeAnalysisListener(AnalysisListener listener) {
		synchronized (analysisListeners_) {
			analysisListeners_.remove(listener);
		}
	}

	/**
	 * Removes the given database query listener from this network watcher. Database query listener will stop receiving analysis messages.
	 *
	 * @param listener
	 *            Database query listener to remove.
	 */
	public void removeDatabaseQueryListener(DatabaseQueryListener listener) {
		synchronized (databaseQueryListeners_) {
			databaseQueryListeners_.remove(listener);
		}
	}

	/**
	 * Returns the owner of the network watcher.
	 *
	 * @return The owner of the network watcher.
	 */
	public MainScreen getOwner() {
		return owner_;
	}

	/**
	 * Returns the registered standard message listener of this network watcher.
	 *
	 * @return The registered standard message listener of this network watcher.
	 */
	public StandardMessageListener getStandardMessageListener() {
		return listener_;
	}

	/**
	 * Sends given message to the server.
	 *
	 * @param message
	 *            Message to send.
	 */
	synchronized public void sendMessage(NetworkMessage message) {

		// submit new task
		threadExecutor_.submit(() -> {

			try {

				// not a big message
				if (message instanceof BigMessage == false) {
					kryoNetClient_.sendTCP(message);
					return;
				}

				// not big
				if (!((BigMessage) message).isReallyBig()) {
					kryoNetClient_.sendTCP(message);
					return;
				}

				// split message into partial messages
				PartialMessage[] parts = SplitMessage.splitMessage((BigMessage) message);

				// no need to split message
				if (parts == null) {
					kryoNetClient_.sendTCP(message);
					return;
				}

				// send parts
				for (PartialMessage part : parts) {
					kryoNetClient_.sendTCP(part);
				}
			}

			// exception occurred during sending message
			catch (Exception e) {

				// log error
				Equinox.LOGGER.log(Level.SEVERE, "Exception occurred during sending network message to exchange server.", e);

				// show warning
				String msg = "Exception occurred during sending network message to exchange server: " + e.getLocalizedMessage();
				msg += " Click 'Details' for more information.";
				owner_.getNotificationPane().showError("Problem encountered", msg, e);
			}
		});
	}

	/**
	 * Connects to server.
	 *
	 * @param message
	 *            Initial message to send.
	 * @return True if successfully connected to server.
	 */
	public boolean connect(NetworkMessage message) {

		// set stop indicator
		isStopped_ = false;

		// already connected
		if (kryoNetClient_.isConnected()) {
			kryoNetClient_.sendTCP(message);
			return true;
		}

		// connect to server
		try {
			String hostname = (String) owner_.getSettings().getValue(Settings.NETWORK_HOSTNAME);
			int port = Integer.parseInt((String) owner_.getSettings().getValue(Settings.NETWORK_PORT));
			kryoNetClient_.connect(5000, hostname, port);
			kryoNetClient_.sendTCP(message);
			return true;
		}

		// exception occurred during connecting to server
		catch (IOException e) {

			// log error
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during connecting to exchange server.", e);
			String msg = "Cannot connect to exchange server. Please check your network connection and try again.";
			owner_.getNotificationPane().showWarning(msg, null);
			return false;
		}
	}

	/**
	 * Disconnects from server. Note that, this method doesn't stop the network thread.
	 */
	public void disconnect() {

		// set stop indicator
		isStopped_ = true;

		// disconnect kryonet client
		kryoNetClient_.close();
		Equinox.LOGGER.info("Disconnected from exchange server.");
	}

	/**
	 * Disconnects from server and stops network thread.
	 */
	public void stop() {

		// stop thread executor
		Utility.shutdownThreadExecutor(threadExecutor_);
		Equinox.LOGGER.info("Network thread executor stopped.");

		// set stop indicator
		isStopped_ = true;

		// stop kryonet client
		kryoNetClient_.stop();
		Equinox.LOGGER.info("Network thread stopped.");
	}

	/**
	 * Returns true if there is active connection to server.
	 *
	 * @return True if there is active connection to server.
	 */
	public boolean isConnected() {
		return kryoNetClient_.isConnected();
	}

	/**
	 * Inner class for server handler. Server handler listens for incoming messages and passes them to registered listener.
	 *
	 * @author Murat Artim
	 * @time 5:51:08 PM
	 * @date Jul 11, 2011
	 */
	private class ServerHandler extends Listener {

		/** Map containing the partial messages and their ancestors' hash codes. */
		private final HashMap<Integer, PartialMessage[]> partialMessages_ = new HashMap<>();

		@Override
		public void connected(Connection connection) {

			// log connection
			Equinox.LOGGER.info("Successfully connected to exchange server.");

			// set 20 seconds timeout for connection (this will allow 12 seconds of network latency)
			connection.setTimeout(20000);
		}

		@Override
		public void received(Connection connection, final Object object) {

			// unsupported protocol
			if (object == null || object instanceof NetworkMessage == false)
				return;

			// respond
			threadExecutor_.submit(() -> {

				try {

					// partial message
					if (object instanceof PartialMessage) {
						receivePartialMessage((PartialMessage) object);
					}

					// analysis message
					else if (object instanceof AnalysisMessage) {
						respondToAnalysisMessage((AnalysisMessage) object);
					}

					// database query message
					else if (object instanceof DatabaseQueryMessage) {
						respondToDatabaseQueryMessage((DatabaseQueryMessage) object);
					}

					// standard message
					else {
						respondToStandardMessage((NetworkMessage) object);
					}
				}

				// exception occurred during responding to message
				catch (Exception e) {

					// log warning
					Equinox.LOGGER.log(Level.WARNING, "Exception occurred during responding to exchange server message.", e);

					// show warning
					Platform.runLater(() -> {
						String message = "Exception occurred during responding to exchange server message: " + e.getLocalizedMessage();
						message += " Click 'Details' for more information.";
						owner_.getNotificationPane().showError("Problem encountered", message, e);
					});
				}
			});
		}

		@Override
		public void disconnected(Connection connection) {

			// stopped by the user
			if (isStopped_)
				return;

			// show warning
			Platform.runLater(() -> {
				String message = "Connection to exchange server lost. Please check your network connection and try again.";
				owner_.getNotificationPane().showWarning(message, null);
			});
		}

		/**
		 * Responds to partial messages received from the server.
		 *
		 * @param part
		 *            Partial message to respond.
		 * @throws Exception
		 *             If exception occurs during process.
		 */
		private void receivePartialMessage(PartialMessage part) throws Exception {

			// get message parameters
			int id = part.getID();
			int numParts = part.getNumParts();
			int index = part.getIndex();

			// get all current parts
			PartialMessage[] parts = partialMessages_.get(id);

			// ID not contained
			if (parts == null) {
				parts = new PartialMessage[numParts];
				parts[index] = part;
				partialMessages_.put(id, parts);
			}

			// contained
			else {
				parts[index] = part;
			}

			// check whether all parts completed
			for (PartialMessage p : parts)
				if (p == null)
					return;

			// combine parts and respond
			BigMessage combinedMessage = SplitMessage.combineMessages(parts);

			// analysis message
			if (combinedMessage instanceof AnalysisMessage) {
				respondToAnalysisMessage((AnalysisMessage) combinedMessage);
			}

			// database query message
			else if (combinedMessage instanceof DatabaseQueryMessage) {
				respondToDatabaseQueryMessage((DatabaseQueryMessage) combinedMessage);
			}

			// standard message
			else {
				respondToStandardMessage(combinedMessage);
			}

			// remove parts
			partialMessages_.remove(id);
		}

		/**
		 * Responds to given analysis network message.
		 *
		 * @param message
		 *            Analysis message to respond to.
		 * @throws Exception
		 *             If exception occurs during process.
		 */
		private void respondToAnalysisMessage(AnalysisMessage message) throws Exception {

			// sync over listeners
			synchronized (analysisListeners_) {

				// get listeners
				Iterator<AnalysisListener> i = analysisListeners_.iterator();

				// loop over listeners
				while (i.hasNext()) {

					// get listener
					AnalysisListener c = i.next();

					// listener hash code matches to message analysis ID
					if (c.hashCode() == message.getAnalysisID()) {
						c.respondToAnalysisMessage(message);
						break;
					}
				}
			}
		}

		/**
		 * Responds to given database query network message.
		 *
		 * @param message
		 *            Database query message to respond to.
		 * @throws Exception
		 *             If exception occurs during process.
		 */
		private void respondToDatabaseQueryMessage(DatabaseQueryMessage message) throws Exception {

			// sync over listeners
			synchronized (databaseQueryListeners_) {

				// get listeners
				Iterator<DatabaseQueryListener> i = databaseQueryListeners_.iterator();

				// loop over listeners
				while (i.hasNext()) {

					// get listener
					DatabaseQueryListener c = i.next();

					// listener hash code matches to message database query ID
					if (c.hashCode() == message.getDatabaseQueryID()) {
						c.respondToDatabaseQueryMessage(message);
						break;
					}
				}
			}
		}

		/**
		 * Responds to given standard network message.
		 *
		 * @param message
		 *            Standard message to respond to.
		 * @throws Exception
		 *             If exception occurs during process.
		 */
		private void respondToStandardMessage(NetworkMessage message) throws Exception {
			if (listener_ != null) {
				listener_.respond(message);
			}
		}
	}
}
