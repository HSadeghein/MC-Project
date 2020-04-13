package com.example.findmyhomie;

import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users")
    List<User> getAll();

    @Query("SELECT * FROM users WHERE username IN (:userNames)")
    List<User> loadAllByUsername(String[] userNames);

    @Query("SELECT * FROM users WHERE username =:username" )
    User getUserByUsername(String username);

//    @Query("SELECT * FROM users WHERE username =:username" )
//    User getUserID(String username);

    @Insert
    void insertAll(User... users);

    @Insert
    Long insertTask(User user) throws SQLiteConstraintException;

    @Update
    public void updateUser(User user);

    @Delete
    void delete(User user);
}