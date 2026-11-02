package com.goldenchemist.pos.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Small JDBC helpers working on a connection supplied by the caller (used inside transactions). */
public final class Sql {

    private Sql() {
    }

    public static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            int idx = i + 1;
            if (a == null) {
                ps.setNull(idx, Types.NULL);
            } else if (a instanceof LocalDate) {
                ps.setDate(idx, java.sql.Date.valueOf((LocalDate) a));
            } else if (a instanceof LocalDateTime) {
                ps.setTimestamp(idx, Timestamp.valueOf((LocalDateTime) a));
            } else if (a instanceof Boolean) {
                ps.setBoolean(idx, (Boolean) a);
            } else if (a instanceof BigDecimal) {
                ps.setBigDecimal(idx, (BigDecimal) a);
            } else if (a instanceof Enum) {
                ps.setString(idx, ((Enum<?>) a).name());
            } else {
                ps.setObject(idx, a);
            }
        }
    }

    public static <T> List<T> queryList(Connection c, String sql, RowMapper<T> mapper, Object... args)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
                return out;
            }
        }
    }

    /** Returns the first row or null. */
    public static <T> T queryOne(Connection c, String sql, RowMapper<T> mapper, Object... args)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapper.map(rs) : null;
            }
        }
    }

    public static int exec(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        }
    }

    /** Executes an INSERT and returns the generated key. */
    public static long insert(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        }
    }

    public static Integer getInteger(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public static LocalDate getDate(ResultSet rs, String col) throws SQLException {
        java.sql.Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    public static LocalDateTime getDateTime(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }
}
