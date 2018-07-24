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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.analysisServer.remote.message.AnalysisMessage;
import equinox.analysisServer.remote.message.AnalysisServerStatisticsRequest;
import equinox.analysisServer.remote.message.AnalysisServerStatisticsRequestFailed;
import equinox.analysisServer.remote.message.AnalysisServerStatisticsResponse;
import equinox.controller.HealthMonitorViewPanel;
import equinox.controller.ViewPanel;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DataServerStatisticsRequest;
import equinox.dataServer.remote.message.DataServerStatisticsRequestFailed;
import equinox.dataServer.remote.message.DataServerStatisticsResponse;
import equinox.exchangeServer.remote.message.ExchangeMessage;
import equinox.exchangeServer.remote.message.ExchangeServerStatisticsRequest;
import equinox.exchangeServer.remote.message.ExchangeServerStatisticsRequestFailed;
import equinox.exchangeServer.remote.message.ExchangeServerStatisticsResponse;
import equinox.network.AnalysisServerManager;
import equinox.network.DataServerManager;
import equinox.network.ExchangeServerManager;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.AnalysisServerStatisticsRequestFailedException;
import equinox.utility.exception.DataServerStatisticsRequestFailedException;
import equinox.utility.exception.ExchangeServerStatisticsRequestFailedException;

/**
 * Class for get server diagnostics task.
 *
 * @author Murat Artim
 * @date 16 Jul 2018
 * @time 00:33:39
 */
public class GetServerDiagnostics extends InternalEquinoxTask<Void> implements ShortRunningTask, DatabaseQueryListenerTask, AnalysisListenerTask, ExchangeMessageListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Server query completion indicator. */
	private final AtomicBoolean isDataServerCompleted, isAnalysisServerCompleted, isExchangeServerCompleted;

	/** Data server query message. */
	private final AtomicReference<DataMessage> dataServerMessageRef;

	/** Analysis server message. */
	private final AtomicReference<AnalysisMessage> analysisServerMessageRef;

	/** Exchange server message. */
	private final AtomicReference<ExchangeMessage> exchangeServerMessageRef;

	/** Data server statistics. */
	private DataServerStatisticsResponse dataServerResponse_ = null;

	/** Analysis server statistics. */
	private AnalysisServerStatisticsResponse analysisServerResponse_ = null;

	/** Exchange server statistics. */
	private ExchangeServerStatisticsResponse exchangeServerResponse_ = null;

	/** Data collection period in minutes. */
	private final long period_;

	/**
	 * Creates get server diagnostics task.
	 *
	 * @param period
	 *            Data collection period in minutes.
	 */
	public GetServerDiagnostics(long period) {
		isDataServerCompleted = new AtomicBoolean();
		dataServerMessageRef = new AtomicReference<>(null);
		isAnalysisServerCompleted = new AtomicBoolean();
		analysisServerMessageRef = new AtomicReference<>(null);
		isExchangeServerCompleted = new AtomicBoolean();
		exchangeServerMessageRef = new AtomicReference<>(null);
		period_ = period;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get server diagnostics";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, dataServerMessageRef, isDataServerCompleted);
	}

	@Override
	public void respondToAnalysisMessage(AnalysisMessage message) throws Exception {
		processServerAnalysisMessage(message, this, analysisServerMessageRef, isAnalysisServerCompleted);
	}

	@Override
	public void respondToExchangeMessage(ExchangeMessage message) throws Exception {
		processServerExchangeMessage(message, this, exchangeServerMessageRef, isExchangeServerCompleted);
	}

	@Override
	protected Void call() throws Exception {

		// get data server diagnostics
		dataServerResponse_ = getDataServerDiagnostics();

		// get exchange server diagnostics
		exchangeServerResponse_ = getExchangeServerDiagnostics();

		// get analysis server diagnostics
		analysisServerResponse_ = getAnalysisServerDiagnostics();

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set data to health monitoring panel
		HealthMonitorViewPanel panel = (HealthMonitorViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.HEALTH_MONITOR_VIEW);
		panel.setData(dataServerResponse_, analysisServerResponse_, exchangeServerResponse_);

		// show panel
		taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.HEALTH_MONITOR_VIEW);
	}

	/**
	 * Retrieves the analysis server diagnostics.
	 *
	 * @return Analysis server statistics.
	 */
	private AnalysisServerStatisticsResponse getAnalysisServerDiagnostics() {

		// progress info
		updateMessage("Getting analysis server diagnostics...");

		// initialize variables
		AnalysisServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			AnalysisServerStatisticsRequest request = new AnalysisServerStatisticsRequest();
			request.setListenerHashCode(hashCode());
			request.setFrom(new Date(Instant.now().minus(period_, ChronoUnit.MINUTES).toEpochMilli()));
			request.setTo(new Date());

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getAnalysisServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForAnalysisServer(this, isAnalysisServerCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// get query message
			AnalysisMessage message = analysisServerMessageRef.get();

			// failed
			if (message instanceof AnalysisServerStatisticsRequestFailed)
				throw new AnalysisServerStatisticsRequestFailedException((AnalysisServerStatisticsRequestFailed) message);

			// succeeded
			else if (message instanceof AnalysisServerStatisticsResponse)
				return (AnalysisServerStatisticsResponse) message;

			// invalid server response
			throw new Exception("Invalid server response.");
		}

		// exception occurred
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Analysis server diagnostics request has failed.", e);
			return null;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}

	/**
	 * Retrieves the exchange server diagnostics.
	 *
	 * @return Exchange server statistics.
	 */
	private ExchangeServerStatisticsResponse getExchangeServerDiagnostics() {

		// progress info
		updateMessage("Getting exchange server diagnostics...");

		// initialize variables
		ExchangeServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			ExchangeServerStatisticsRequest request = new ExchangeServerStatisticsRequest();
			request.setListenerHashCode(hashCode());
			request.setFrom(new Date(Instant.now().minus(period_, ChronoUnit.MINUTES).toEpochMilli()));
			request.setTo(new Date());

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getExchangeServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForExchangeServer(this, isExchangeServerCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// get query message
			ExchangeMessage message = exchangeServerMessageRef.get();

			// failed
			if (message instanceof ExchangeServerStatisticsRequestFailed)
				throw new ExchangeServerStatisticsRequestFailedException((ExchangeServerStatisticsRequestFailed) message);

			// succeeded
			else if (message instanceof ExchangeServerStatisticsResponse)
				return (ExchangeServerStatisticsResponse) message;

			// invalid server response
			throw new Exception("Invalid server response.");
		}

		// exception occurred
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Exchange server diagnostics request has failed.", e);
			return null;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}

	/**
	 * Retrieves the data server diagnostics.
	 *
	 * @return Data server statistics.
	 */
	private DataServerStatisticsResponse getDataServerDiagnostics() {

		// progress info
		updateMessage("Getting data server diagnostics...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			DataServerStatisticsRequest request = new DataServerStatisticsRequest();
			request.setListenerHashCode(hashCode());
			request.setFrom(new Date(Instant.now().minus(period_, ChronoUnit.MINUTES).toEpochMilli()));
			request.setTo(new Date());

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getDataServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForDataServer(this, isDataServerCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// get query message
			DataMessage message = dataServerMessageRef.get();

			// failed
			if (message instanceof DataServerStatisticsRequestFailed)
				throw new DataServerStatisticsRequestFailedException((DataServerStatisticsRequestFailed) message);

			// succeeded
			else if (message instanceof DataServerStatisticsResponse)
				return (DataServerStatisticsResponse) message;

			// invalid server response
			throw new Exception("Invalid server response.");
		}

		// exception occurred
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Data server diagnostics request has failed.", e);
			return null;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}
}