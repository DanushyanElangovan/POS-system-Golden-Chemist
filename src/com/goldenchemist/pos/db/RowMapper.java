package com.goldenchemist.pos.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps the current row of a ResultSet to an object. */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
}
