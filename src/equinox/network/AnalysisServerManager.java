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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import equinox.Equinox;
import equinox.analysisServer.remote.Registry;
import equinox.analysisServer.remote.listener.AnalysisMessageListener;
import equinox.analysisServer.remote.message.AnalysisMessage;
import equinox.analysisServer.remote.message.HandshakeWithAnalysisServer;
import equinox.controller.MainScreen;
import equinox.data.Settings;
import equinox.serverUtilities.BigMessage;
import equinox.serverUtilities.NetworkMessage;
import equinox.serverUtilities.PartialMessage;
import equinox.serverUtilities.PermissionDenied;
import equinox.serverUtilities.SplitMessage;
import equinox.utility.Utility;
import javafx.application.Platform;

/**
 * Class for analysis server manager.
 *
 * @author Murat Artim
 * @date 24 Jun 2018
 * @time 16:21:45
 */
public class AnalysisServerManager implements AnalysisMessageListener {

	/** Serial id. */
	private static final long serialVersionUID = 1L;

	/** The kryonet client of network watcher. */
	private final Client kryoNetClient_;

	/** The owner of the network watcher. */
	private final MainScreen owner_;

	/** Analysis message listeners. */
	private List<AnalysisMessageListener> messageListeners_;

	/** Stop indicator of the network watcher. */
	private volatile boolean isStopped_ = false;

	/** The thread executor of the network watcher. */
	private final ExecutorService threadExecutor_;

	/** Message queue. */
	private final ConcurrentLinkedQueue<AnalysisMessage> messageQueue_;

	/**
	 * Creates analysis server manager.
	 *
	 * @param owner
	 *            The owner main screen.
	 */
	public AnalysisServerManager(MainScreen owner) {

		// set owner
		owner_ = owner;

		// create message queue
		messageQueue_ = new ConcurrentLinkedQueue<>();

		// create analysis message listener list
		messageListeners_ = Collections.synchronizedList(new ArrayList<AnalysisMessageListener>());

		// create thread executor of the network watcher
		threadExecutor_ = Executors.newSingleThreadExecutor();

		// create kryonet client
		kryoNetClient_ = new Client(65536, 8192);

		// register objects to be sent over network
		Registry.register(kryoNetClient_);

		// add server handler (listener)
		kryoNetClient_.addListener(new AnalysisServerListener());

		// start client
		kryoNetClient_.start();
		addMessageListener(this);
		Equinox.LOGGER.info("Analysis server client initialized.");
	}

	/**
	 * Adds given analysis message listener.
	 *
	 * @param listener
	 *            Analysis message listener to add.
	 */
	public void addMessageListener(AnalysisMessageListener listener) {
		synchronized (messageListeners_) {
			messageListeners_.add(listener);
		}
	}

	/**
	 * Removes the given analysis message listener.
	 *
	 * @param listener
	 *            Analysis message listener to remove.
	 */
	public void removeMessageListener(AnalysisMessageListener listener) {
		synchronized (messageListeners_) {
			messageListeners_.remove(listener);
		}
	}

	/**
	 * Returns the owner screen.
	 *
	 * @return The owner screen.
	 */
	public MainScreen getOwner() {
		return owner_;
	}

	/**
	 * Connects to analysis server.
	 *
	 * @param message
	 *            Message to be sent once the connection is established. Can be <code>null</code>.
	 *
	 * @return True if successfully connected to server.
	 */
	public boolean connect(AnalysisMessage message) {

		// set stop indicator
		isStopped_ = false;

		// already connected
		if (kryoNetClient_.isConnected()) {
			if (message != null) {
				sendMessage(message);
			}
			return true;
		}

		// add message to queue
		if (message != null) {
			messageQueue_.add(message);
		}

		// connect to server
		try {
			String hostname = (String) owner_.getSettings().getValue(Settings.ANALYSIS_SERVER_HOSTNAME);
			int port = Integer.parseInt((String) owner_.getSettings().getValue(Settings.ANALYSIS_SERVER_PORT));
			kryoNetClient_.connect(5000, hostname, port);
			HandshakeWithAnalysisServer handshake = new HandshakeWithAnalysisServer(Equinox.USER.getAlias());
			handshake.setListenerHashCode(hashCode());
			kryoNetClient_.sendTCP(handshake);
			return true;
		}

		// exception occurred during connecting to server
		catch (IOException e) {

			// log error
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during connecting to analysis server.", e);
			String msg = "Cannot connect to analysis server. Please check your network connection and analysis server connection settings.";
			owner_.getNotificationPane().showWarning(msg, null);
			messageQueue_.clear();
			return false;
		}
	}

	/**
	 * Sends given message to the server.
	 *
	 * @param message
	 *            Message to send.
	 */
	synchronized public void sendMessage(AnalysisMessage message) {

		// null message
		if (message == null)
			return;

		// submit new task
		threadExecutor_.submit(() -> {

			try {

				// not connected
				if (!kryoNetClient_.isConnected()) {
					connect(message);
					return;
				}

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
				Equinox.LOGGER.log(Level.SEVERE, "Exception occurred during sending network message to analysis server.", e);

				// show warning
				String msg = "Exception occurred during sending message to analysis server: " + e.getLocalizedMessage();
				msg += " Click 'Details' for more information.";
				owner_.getNotificationPane().showError("Problem encountered", msg, e);
			}
		});
	}

	/**
	 * Disconnects from server. Note that, this method doesn't stop the network thread.
	 */
	public void disconnect() {

		// set stop indicator
		isStopped_ = true;

		// disconnect kryonet client
		kryoNetClient_.close();
		Equinox.LOGGER.info("Disconnected from analysis server.");
	}

	/**
	 * Disconnects from server and stops network thread.
	 */
	public void stop() {

		// stop thread executor
		Utility.shutdownThreadExecutor(threadExecutor_);
		Equinox.LOGGER.info("Analysis server manager network thread executor stopped.");

		// set stop indicator
		isStopped_ = true;

		// stop kryonet client
		kryoNetClient_.stop();
		Equinox.LOGGER.info("Disconnected from analysis server.");
	}

	/**
	 * Returns true if there is active connection to server.
	 *
	 * @return True if there is active connection to server.
	 */
	public boolean isConnected() {
		return kryoNetClient_.isConnected();
	}

	@Override
	public void respondToAnalysisMessage(AnalysisMessage message) throws Exception {

		// handshake
		if (message instanceof HandshakeWithAnalysisServer) {
			processHandshake((HandshakeWithAnalysisServer) message);
		}
	}

	/**
	 * Processes handshake message from the server.
	 *
	 * @param handshake
	 *            Handshake message.
	 */
	private void processHandshake(HandshakeWithAnalysisServer handshake) {

		// successful
		if (handshake.isHandshakeSuccessful()) {

			// log
			Equinox.LOGGER.info("Successfully connected to analysis server.");

			// send all queued message (if any)
			AnalysisMessage message;
			while ((message = messageQueue_.poll()) != null) {
				sendMessage(message);
			}
		}

		// unsuccessful
		else {
			String text = "Cannot connect to analysis server. Please check your network connection and analysis server connection settings.";
			owner_.getNotificationPane().showWarning(text, null);
			messageQueue_.clear();
		}
	}

	/**
	 * Inner class for analysis server listener. Server handler listens for incoming messages and passes them to registered listener.
	 *
	 * @author Murat Artim
	 * @time 5:51:08 PM
	 * @date Jul 11, 2011
	 */
	private class AnalysisServerListener extends Listener {

		/** Map containing the partial messages and their ancestors' hash codes. */
		private final HashMap<Integer, PartialMessage[]> partialMessages_ = new HashMap<>();

		@Override
		public void connected(Connection connection) {

			// log connection
			Equinox.LOGGER.info("Successfully connected to analysis server.");

			// set 20 seconds timeout for connection (this will allow 12 seconds of network latency)
			connection.setTimeout(20000);

			// notify UI
			owner_.getInputPanel().analysisServiceConnectionStatusChanged(true);
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

					// permission denied message
					else if (object instanceof PermissionDenied) {
						owner_.getNotificationPane().showPermissionDenied(((PermissionDenied) object).getPermission());
					}

					// analysis message
					else if (object instanceof AnalysisMessage) {
						respond((AnalysisMessage) object);
					}
				}

				// exception occurred during responding to message
				catch (Exception e) {

					// log warning
					Equinox.LOGGER.log(Level.WARNING, "Exception occurred during responding to analysis server message.", e);

					// show warning
					Platform.runLater(() -> {
						String message = "Exception occurred during responding to analysis server message: " + e.getLocalizedMessage();
						message += " Click 'Details' for more information.";
						owner_.getNotificationPane().showError("Problem encountered", message, e);
					});
				}
			});
		}

		@Override
		public void disconnected(Connection connection) {

			// notify UI
			owner_.getInputPanel().analysisServiceConnectionStatusChanged(false);

			// stopped by the user
			if (isStopped_)
				return;

			// log warning
			Equinox.LOGGER.log(Level.WARNING, "Connection to analysis server lost.");

			// show warning
			Platform.runLater(() -> {
				String message = "Connection to analysis server lost. Please check your network connection.";
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
				respond((AnalysisMessage) combinedMessage);
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
		private void respond(AnalysisMessage message) throws Exception {

			// sync over listeners
			synchronized (messageListeners_) {

				// get listeners
				Iterator<AnalysisMessageListener> i = messageListeners_.iterator();

				// loop over listeners
				while (i.hasNext()) {

					// get listener
					AnalysisMessageListener c = i.next();

					// listener hash code matches to message analysis ID
					if (c.hashCode() == message.getListenerHashCode()) {
						c.respondToAnalysisMessage(message);
						break;
					}
				}
			}
		}
	}
}