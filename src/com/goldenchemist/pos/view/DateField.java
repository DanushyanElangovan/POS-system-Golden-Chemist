package com.goldenchemist.pos.view;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/** A date input (yyyy-MM-dd) - type the date or use the arrow keys / buttons to step through days. */
public class DateField extends JSpinner {
    private static final long serialVersionUID = 1L;

    public DateField() {
        this(LocalDate.now());
    }

    public DateField(LocalDate initial) {
        super(new SpinnerDateModel(toDate(initial), null, null, Calendar.DAY_OF_MONTH));
        setEditor(new JSpinner.DateEditor(this, "yyyy-MM-dd"));
        setPreferredSize(new Dimension(130, 28));
    }

    public LocalDate getDate() {
        return ((Date) getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setDate(LocalDate d) {
        setValue(toDate(d == null ? LocalDate.now() : d));
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
