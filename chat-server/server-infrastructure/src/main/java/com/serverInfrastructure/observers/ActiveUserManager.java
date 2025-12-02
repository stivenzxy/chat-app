package com.serverInfrastructure.observers;

import com.serverDomain.entities.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ActiveUserManager {

    private static final ActiveUserManager INSTANCE = new ActiveUserManager();
    private final Map<String, List<User>> activeUserSessions = new ConcurrentHashMap<>();
    private final Map<String, String> connectionToUserId = new ConcurrentHashMap<>();

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

    public void userLoggedIn(String username, User user, String connectionId) {
        connectionToUserId.put(connectionId, user.getId());
        
        User sessionUser = new User(
            connectionId,  
            user.getUsername(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getPhotoData(),
            user.getIpAddress(),
            user.getCreatedAt()
        );
        
        final boolean[] shouldNotify = {false};
        
        activeUserSessions.compute(username, (key, sessions) -> {
            if (sessions == null) {
                sessions = new CopyOnWriteArrayList<>();
            }
            boolean exists = sessions.stream()
                    .anyMatch(u -> u.getId().equals(connectionId));
            if (!exists) {
                sessions.add(sessionUser);
                if (sessions.size() == 1) {
                    shouldNotify[0] = true;
                }
            }
            return sessions;
        });
        
        if (shouldNotify[0]) {
            notifyUserLoggedIn(user);
        }
    }
    
    @Deprecated
    public void userLoggedIn(String username, User user) {
        userLoggedIn(username, user, user.getId());
    }

    public void userLoggedOut(String username, String connectionId) {
        activeUserSessions.computeIfPresent(username, (key, sessions) -> {
            int indexToRemove = -1;
            User removedUser = null;
            
            for (int i = 0; i < sessions.size(); i++) {
                if (sessions.get(i).getId().equals(connectionId)) {
                    indexToRemove = i;
                    removedUser = sessions.get(i);
                    break;
                }
            }
            
            if (indexToRemove != -1) {
                boolean isLastSession = sessions.size() == 1;
                
                sessions.remove(indexToRemove);
                
                connectionToUserId.remove(connectionId);
                
                notifyUserLoggedOut(removedUser, isLastSession);
            }
            
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public void userLoggedOut(String username) {
        List<User> sessions = activeUserSessions.remove(username);
        if (sessions != null) {
            sessions.forEach(this::notifyUserLoggedOut);
        }
    }

    private void notifyUserLoggedIn(User user) {
        for (ActiveUserObserver observer : observers) {
            observer.onUserLoggedIn(user);
        }
    }

    private void notifyUserLoggedOut(User user, boolean isLastSession) {
        for (ActiveUserObserver observer : observers) {
            observer.onUserLoggedOut(user, isLastSession);
        }
    }
    
    private void notifyUserLoggedOut(User user) {
        notifyUserLoggedOut(user, true);
    }

    public List<User> getUserSessions(String username) {
        List<User> sessions = activeUserSessions.get(username);
        return sessions != null ? new ArrayList<>(sessions) : Collections.emptyList();
    }

    @Deprecated
    public Map<String, User> getActiveUsers() {
        return activeUserSessions.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get(0)
                ));
    }

    public Map<String, List<User>> getAllUserSessions() {
        return Collections.unmodifiableMap(activeUserSessions);
    }
    
    public String getUserIdFromConnection(String connectionId) {
        return connectionToUserId.get(connectionId);
    }
}