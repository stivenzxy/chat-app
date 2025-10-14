package com.serverPresentation.observers;

import com.serverApplication.dto.UserPresentationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserRegistrationObservable {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationObservable.class);

    private final List<UserRegistrationObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(UserRegistrationObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void notifyUserRegistered(UserPresentationDTO user) {
        for (UserRegistrationObserver observer : observers) {
            try {
                observer.onUserRegistered(user);
            } catch (Exception e) {
                logger.error("Error notificando observer {}: {}", 
                    observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    public void notifyUserRegistrationError(String error) {
        for (UserRegistrationObserver observer : observers) {
            try {
                observer.onUserRegistrationError(error);
            } catch (Exception e) {
                logger.error("Error notificando error a observer {}: {}", 
                    observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    public void notifyUserListUpdated() {
        for (UserRegistrationObserver observer : observers) {
            try {
                observer.onUserListUpdated();
            } catch (Exception e) {
                logger.error("Error notificando actualización a observer {}: {}", 
                    observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    public int getObserverCount() {
        return observers.size();
    }
}