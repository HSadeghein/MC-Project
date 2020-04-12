package com.example.findmyhomie;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users")
    List<User> getAll();

    @Query("SELECT * FROM users WHERE username IN (:userNames)")
    List<User> loadAllByUsername(String[] userNames);

    @Query("SELECT * FROM users WHERE username =:username" )
    User getUserByUsername(String username);

    @Insert
    void insertAll(User... users);

    @Insert
    Long insertTask(User note);

    @Delete
    void delete(User user);
}