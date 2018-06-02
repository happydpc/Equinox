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
package equinox.process;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Interface for processes.
 *
 * @author Murat Artim
 * @param <U>
 *            Object to be returned as output of the process.
 * @date Sep 24, 2014
 * @time 12:14:12 PM
 */
public interface EquinoxProcess<U> {

	/**
	 * Starts this process.
	 *
	 * @param connection
	 *            Database connection.
	 * @param preparedStatements
	 *            Prepared statements (if any).
	 * @return The output spectrum item.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	U start(Connection connection, PreparedStatement... preparedStatements) throws Exception;
}
