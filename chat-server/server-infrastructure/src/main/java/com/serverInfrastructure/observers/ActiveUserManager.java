package com.serverInfrastructure.observers;

import com.serverDomain.entities.User;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ActiveUserManager {

    private static final ActiveUserManager INSTANCE = new ActiveUserManager();
    private final Map<String, User> activeUsers = new ConcurrentHashMap<>();

    private final List<ActiveUserObserver> observers = new CopyOnWriteArrayList<>();

    private ActiveUserManager() {}

    public static ActiveUserManager getInstance() {
        return INSTANCE;
    }

    public void addObserver(ActiveUserObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ActiveUserObserver observer) {
        observers.remove(observer);
    }

    public void userLoggedIn(String username, User user) {
        if (activeUsers.putIfAbsent(username, user) == null) {
            notifyUserLoggedIn(user);
        }
    }

    public void userLoggedOut(String username) {
        User user = activeUsers.remove(username);
        if (user != null) {
            notifyUserLoggedOut(user);
        } else {
        }
    }

    private void notifyUserLoggedIn(User user) {
        for (ActiveUserObserver observer : observers) {
            observer.onUserLoggedIn(user);
        }
    }

    private void notifyUserLoggedOut(User user) {
        for (ActiveUserObserver observer : observers) {
            observer.onUserLoggedOut(user);
        }
    }

    public Map<String, User> getActiveUsers() {
        return Collections.unmodifiableMap(activeUsers);
    }
}