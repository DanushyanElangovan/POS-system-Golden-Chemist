package com.goldenchemist.pos.model;

import java.time.LocalDate;

public class Employee {
    private int id;
    private String fullName;
    private String username;
    private String passwordHash;
    private Role role = Role.CASHIER;
    private String phone;
    private String email;
    private String nic;
    private LocalDate hiredDate;
    private boolean active = true;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }
    public LocalDate getHiredDate() { return hiredDate; }
    public void setHiredDate(LocalDate hiredDate) { this.hiredDate = hiredDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() { return fullName; }
}
