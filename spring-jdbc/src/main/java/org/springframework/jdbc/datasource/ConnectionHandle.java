/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;

/**
 * Simple interface to be implemented by handles for a JDBC Connection.
 * Used by JpaDialect, for example.
 *
 *
 * { @link https://blog.csdn.net/songjinbin/article/details/19857567}
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see SimpleConnectionHandle
 * @see ConnectionHolder
 */
@FunctionalInterface
public interface ConnectionHandle {

	/**
	 * Fetch the JDBC Connection that this handle refers to.
	 */
	Connection getConnection();

	/**
	 * Release the JDBC Connection that this handle refers to.
	 * <p>The default implementation is empty, assuming that the lifecycle
	 * of the connection is managed externally.
	 * @param con the JDBC Connection to release
	 */
	default void releaseConnection(Connection con) {
	}

}