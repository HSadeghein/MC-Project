package com.example.findmyhomie;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;

import androidx.room.Room;

import java.util.List;

public class UserRepository {

//    private static com.example.findmyhomie.UserRepository sing_instance = null;

    private String DB_NAME = "users";

    private AppDatabase userDatabase = null;

    public UserRepository(Context context) {
        userDatabase = Room.databaseBuilder(context, AppDatabase.class, DB_NAME).allowMainThreadQueries().fallbackToDestructiveMigration().build();
    }

    //    public static com.example.findmyhomie.UserRepository getInstance()
//    {
//        if(sing_instance == null)
//        {
//            sing_instance = new com.example.findmyhomie.UserRepository()
//        }
//    }
    public void insertUser(String _fullName,
                           String _username,
                           float _Lat,
                           float _Lng,
                           String _spotifySongID) {

        User user = new User();
        user.setFullName(_fullName);
        user.setUsername(_username);
        user.setLat(_Lat);
        user.setLng(_Lng);
        user.setSpotifySongID(_spotifySongID);

        insertUser(user);
    }

    public void insertUser(final User user) throws SQLiteConstraintException {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                userDatabase.userDao().insertTask(user);
                return null;
            }
        }.execute();
    }

    public void updateTask(final User user) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                userDatabase.userDao().updateUser(user);
                return null;
            }
        }.execute();
    }
//
//    public void deleteTask(final int id) {
//        final LiveData<Note> task = getTask(id);
//        if(task != null) {
//            new AsyncTask<Void, Void, Void>() {
//                @Override
//                protected Void doInBackground(Void... voids) {
//                    noteDatabase.daoAccess().deleteTask(task.getValue());
//                    return null;
//                }
//            }.execute();
//        }
//    }
//
//    public void deleteTask(final Note note) {
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... voids) {
//                noteDatabase.daoAccess().deleteTask(note);
//                return null;
//            }
//        }.execute();
//    }

//    public User getUser(final String _username) {
//        new AsyncTask<String, Void, User>() {
//            @Override
//            protected User doInBackground(String... params) {
//                return userDatabase.userDao().getUserByUsername(params[0]);
//            }
//        }.execute(_username);
//    }

    public User getUser(String _username) {
        return userDatabase.userDao().getUserByUsername(_username);
    }
    public List<User> getAllUsers()
    {
        return userDatabase.userDao().getAll();
    }
//
//    public LiveData<List<Note>> getTasks() {
//        return noteDatabase.daoAccess().fetchAllTasks();
//    }
}
