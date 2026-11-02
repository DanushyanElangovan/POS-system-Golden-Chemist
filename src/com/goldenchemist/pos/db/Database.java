package com.goldenchemist.pos.db;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.util.PosException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/** Connection factory plus convenience wrappers that translate SQL errors into PosException. */
public final class Database {

    static {
        String[] drivers = {"com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver", "org.mariadb.jdbc.Driver"};
        for (String d : drivers) {
            try {
                Class.forName(d);
                break;
            } catch (ClassNotFoundException ignored) {
                // try next driver
            }
        }
    }

    private Database() {
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.get("db.url", "jdbc:mysql://localhost:3306/golden_chemist_pos"),
                AppConfig.get("db.user", "root"),
                AppConfig.get("db.password", ""));
    }

    /** Throws PosException with a friendly message if the database cannot be reached. */
    public static void verifyConnection() {
        try (Connection c = get()) {
            c.isValid(2);
        } catch (SQLException e) {
            throw wrap("connect to the database", e);
        }
    }

    public static PosException wrap(String action, SQLException e) {
        int code = e.getErrorCode();
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("No suitable driver") || msg.contains("Client does not support authentication")) {
            return new PosException("The JDBC driver in the lib folder cannot talk to your database server. "
                    + "See 'Choosing the JDBC driver' in README.md.", e);
        }
        if (code == 1062) {
            return new PosException("Cannot " + action + ": a record with the same unique value already exists.", e);
        }
        if (code == 1451) {
            return new PosException("Cannot " + action + ": the record is in use by other records.", e);
        }
        if (code == 1452) {
            return new PosException("Cannot " + action + ": it refers to a record that no longer exists.", e);
        }
        if (code == 1049) {
            return new PosException("Database 'golden_chemist_pos' was not found. Run database/schema.sql first.", e);
        }
        if (code == 1045 || state.startsWith("28")) {
            return new PosException("Database login failed. Check db.user / db.password in config/app.properties.", e);
        }
        if (state.startsWith("08")) {
            return new PosException("Cannot reach the database server. Make sure MySQL is running and "
                    + "db.url in config/app.properties is correct.", e);
        }
        return new PosException("Database error while trying to " + action + ": " + e.getMessage(), e);
    }

    public static <T> List<T> list(String action, String sql, RowMapper<T> mapper, Object... args) {
        try (Connection c = get()) {
            return Sql.queryList(c, sql, mapper, args);
        } catch (SQLException e) {
            throw wrap(action, e);
        }
    }

    public static <T> T one(String action, String sql, RowMapper<T> mapper, Object... args) {
        try (Connection c = get()) {
            return Sql.queryOne(c, sql, mapper, args);
        } catch (SQLException e) {
            throw wrap(action, e);
        }
    }

    public static int update(String action, String sql, Object... args) {
        try (Connection c = get()) {
            return Sql.exec(c, sql, args);
        } catch (SQLException e) {
            throw wrap(action, e);
        }
    }

    public static long insert(String action, String sql, Object... args) {
        try (Connection c = get()) {
            return Sql.insert(c, sql, args);
        } catch (SQLException e) {
            throw wrap(action, e);
        }
    }

    /** Runs the work in one transaction: commit on success, rollback on any failure. */
    public static <T> T inTransaction(String action, TxWork<T> work) {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException e) {
                rollbackQuietly(c);
                throw wrap(action, e);
            } catch (RuntimeException e) {
                rollbackQuietly(c);
                throw e;
            }
        } catch (SQLException e) {
            throw wrap(action, e);
        }
    }

    private static void rollbackQuietly(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignored) {
            // nothing more we can do
        }
    }
}
