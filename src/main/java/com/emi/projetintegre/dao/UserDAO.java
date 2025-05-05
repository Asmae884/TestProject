package com.emi.projetintegre.dao;

import com.emi.projetintegre.models.User;

public interface UserDAO {
    void addUser(User user);
    User getUserByLogin(String login);
}