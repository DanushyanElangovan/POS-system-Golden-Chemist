package com.goldenchemist.pos.view;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Read-only table model backed by a list of domain objects. */
public class ObjectTableModel<T> extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    private final String[] columns;
    private final transient Function<T, Object[]> rowMapper;
    private transient List<T> items = new ArrayList<>();

    public ObjectTableModel(String[] columns, Function<T, Object[]> rowMapper) {
        this.columns = columns;
        this.rowMapper = rowMapper;
    }

    public void setItems(List<T> newItems) {
        this.items = new ArrayList<>(newItems);
        fireTableDataChanged();
    }

    public T getItem(int modelRow) {
        return modelRow >= 0 && modelRow < items.size() ? items.get(modelRow) : null;
    }

    public List<T> getItems() {
        return items;
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        return rowMapper.apply(items.get(row))[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (!items.isEmpty()) {
            Object v = getValueAt(0, column);
            if (v != null) {
                return v.getClass();
            }
        }
        return Object.class;
    }
}
