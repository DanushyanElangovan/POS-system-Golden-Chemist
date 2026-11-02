package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.dao.CustomerDao;
import com.goldenchemist.pos.model.Customer;
import com.goldenchemist.pos.model.Module;
import com.goldenchemist.pos.util.Strings;

import java.util.List;

public class CustomerController {

    private final CustomerDao dao = new CustomerDao();

    public List<Customer> search(String q) {
        Session.require(Module.CUSTOMERS);
        return dao.search(q);
    }

    /** Also used by the billing screen's quick-add; cashiers have the CUSTOMERS module. */
    public Customer save(Customer c) {
        Session.require(Module.CUSTOMERS);
        c.setFullName(Strings.required("Customer name", c.getFullName(), 100));
        c.setPhone(Strings.phone("Phone", c.getPhone()));
        c.setEmail(Strings.email("E-mail", c.getEmail()));
        c.setAddress(Strings.optional("Address", c.getAddress(), 200));
        if (c.getId() == 0) {
            c.setId(dao.insert(c));
        } else {
            dao.update(c);
        }
        return c;
    }

    public void delete(Customer c) {
        Session.require(Module.CUSTOMERS);
        dao.delete(c.getId());
    }
}
