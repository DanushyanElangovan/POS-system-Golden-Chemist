package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.dao.SupplierDao;
import com.goldenchemist.pos.model.Module;
import com.goldenchemist.pos.model.Supplier;
import com.goldenchemist.pos.util.Strings;

import java.util.List;

public class SupplierController {

    private final SupplierDao dao = new SupplierDao();

    public List<Supplier> search(String q) {
        Session.require(Module.SUPPLIERS);
        return dao.search(q, false);
    }

    public void save(Supplier s) {
        Session.require(Module.SUPPLIERS);
        s.setName(Strings.required("Supplier name", s.getName(), 100));
        s.setContactPerson(Strings.optional("Contact person", s.getContactPerson(), 100));
        s.setPhone(Strings.phone("Phone", s.getPhone()));
        s.setEmail(Strings.email("E-mail", s.getEmail()));
        s.setAddress(Strings.optional("Address", s.getAddress(), 200));
        if (s.getId() == 0) {
            dao.insert(s);
        } else {
            dao.update(s);
        }
    }

    public void setActive(Supplier s, boolean active) {
        Session.require(Module.SUPPLIERS);
        dao.setActive(s.getId(), active);
    }
}
