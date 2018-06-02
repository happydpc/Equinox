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
package equinox.utility.exception;

import equinoxServer.remote.message.DatabaseQueryFailed;

/**
 * Class for server database query failed exception.
 *
 * @author Murat Artim
 * @date 23 Jan 2018
 * @time 10:59:47
 */
public class ServerDatabaseQueryFailedException extends Exception {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Server message. */
	private final DatabaseQueryFailed serverMessage;

	/**
	 * Creates server database query failed exception.
	 *
	 * @param serverMessage
	 *            Server failure message.
	 */
	public ServerDatabaseQueryFailedException(DatabaseQueryFailed serverMessage) {
		this.serverMessage = serverMessage;
	}

	@Override
	public String getMessage() {
		return serverMessage.getExceptionMessage();
	}

	/**
	 * Returns the database query ID. Database query ID is the hash code of the database query listener object on the client.
	 *
	 * @return Database query ID.
	 */
	public int getDatabaseQueryID() {
		return serverMessage.getDatabaseQueryID();
	}
}
