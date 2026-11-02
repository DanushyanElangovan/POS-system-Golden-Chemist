package com.goldenchemist.pos.dao;

import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.model.Category;

import java.util.List;

public class CategoryDao {

    public List<Category> findAll() {
        return Database.list("load categories", "SELECT category_id, name FROM category ORDER BY name",
                rs -> new Category(rs.getInt("category_id"), rs.getString("name")));
    }

    public int insert(String name) {
        return (int) Database.insert("add the category", "INSERT INTO category (name) VALUES (?)", name);
    }
}
