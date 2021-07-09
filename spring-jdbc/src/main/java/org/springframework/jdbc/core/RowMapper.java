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

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.lang.Nullable;

/**
 * An interface used by {@link JdbcTemplate} for mapping rows of a
 * {@link java.sql.ResultSet} on a per-row basis. Implementations of this
 * interface perform the actual work of mapping each row to a result object,
 * but don't need to worry about exception handling.
 * {@link java.sql.SQLException SQLExceptions} will be caught and handled
 * by the calling JdbcTemplate.
 *
 * <p>Typically used either for {@link JdbcTemplate}'s query methods
 * or for out parameters of stored procedures. RowMapper objects are
 * typically stateless and thus reusable; they are an ideal choice for
 * implementing row-mapping logic in a single place.
 *
 * <p>Alternatively, consider subclassing
 * {@link org.springframework.jdbc.object.MappingSqlQuery} from the
 * {@code jdbc.object} package: Instead of working with separate
 * JdbcTemplate and RowMapper objects, you can build executable query
 * objects (containing row-mapping logic) in that style.
 *
 * JdbcTemplate用于按行映射ResultSet的行的接口。
 * 该接口的实现执行将每行映射到结果对象的实际工作，但不需要担心异常处理。
 * SQLExceptions将被调用的JdbcTemplate捕获和处理。
 * 通常用于JdbcTemplate的查询方法或存储过程的输出参数。
 * RowMapper对象通常是无状态的，因此可重用;它们是在单个位置实现行映射逻辑的理想选择。
 * 或者，考虑从jdbc中子类化org.springframework.jdbc.object.MappingSqlQuery。
 * 对象包:您可以以这种风格构建可执行的查询对象(包含行映射逻辑)，而不是使用单独的JdbcTemplate和RowMapper对象。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @param <T> the result type
 * @see JdbcTemplate
 * @see RowCallbackHandler
 * @see ResultSetExtractor
 * @see org.springframework.jdbc.object.MappingSqlQuery
 */
@FunctionalInterface
public interface RowMapper<T> {

	/**
	 * Implementations must implement this method to map each row of data
	 * in the ResultSet. This method should not call {@code next()} on
	 * the ResultSet; it is only supposed to map values of the current row.
	 * @param rs the ResultSet to map (pre-initialized for the current row)
	 * @param rowNum the number of the current row
	 * @return the result object for the current row (may be {@code null})
	 * @throws SQLException if an SQLException is encountered getting
	 * column values (that is, there's no need to catch SQLException)
	 *
	 * 实现必须实现此方法来映射ResultSet中的每一行数据。
	 * 这个方法不应该在ResultSet上调用next();它只能映射当前行的值。
	 *
	 * 参数:
	 * rs—要映射的ResultSet(为当前行预初始化)
	 * rowNum -当前行数
	 *
	 * 返回:
	 * 当前行的结果对象(可能为空)
	 *
	 */
	@Nullable
	T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
