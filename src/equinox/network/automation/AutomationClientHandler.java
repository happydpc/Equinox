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
package equinox.network.automation;

import java.net.Socket;

import equinox.Equinox;

/**
 * TODO
 * 
 * @author Murat Artim
 * @date 23 Sep 2018
 * @time 23:34:08
 */
public class AutomationClientHandler extends Thread {

	/** Client socket to respond to. */
	private final Socket socket;

	/**
	 * Creates client handler.
	 *
	 * @param socket
	 *            Client socket.
	 */
	public AutomationClientHandler(Socket socket) {
		super("Automation Client Handler");
		this.socket = socket;
		Equinox.LOGGER.info("Automation client connection established from '" + socket.getInetAddress().toString() + "'.");
	}

	/**
	 * Processes client inputs.
	 */
	@Override
	public void run() {

	}
}