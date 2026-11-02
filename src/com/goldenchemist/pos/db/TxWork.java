package com.goldenchemist.pos.db;

import java.sql.Connection;
import java.sql.SQLException;

/** A unit of work executed inside one database transaction. */
@FunctionalInterface
public interface TxWork<T> {
    T run(Connection c) throws SQLException;
}
