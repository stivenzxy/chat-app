package com.serverInfrastructure.observers;

import com.serverDomain.entities.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ActiveUserManager {

    private static final ActiveUserManager INSTANCE = new ActiveUserManager();
    // Cambio: Ahora soporta múltiples sesiones por usuario
    private final Map<String, List<User>> activeUserSessions = new ConcurrentHashMap<>();
    // Mapa adicional: connectionId -> userId real (para mantener referencia al userId original)
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

    // Nuevo método que acepta connectionId por separado
    public void userLoggedIn(String username, User user, String connectionId) {
        // Guardar el mapeo connectionId -> userId real
        connectionToUserId.put(connectionId, user.getId());
        
        // Crear un User de sesión con el connectionId como ID para tracking
        User sessionUser = new User(
            connectionId,  // Usar connectionId como ID para esta sesión
            user.getUsername(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getPhotoData(),
            user.getIpAddress(),
            user.getCreatedAt()
        );
        
        activeUserSessions.compute(username, (key, sessions) -> {
            if (sessions == null) {
                sessions = new CopyOnWriteArrayList<>();
            }
            // Evitar duplicados por connectionId
            boolean exists = sessions.stream()
                    .anyMatch(u -> u.getId().equals(connectionId));
            if (!exists) {
                sessions.add(sessionUser);
                // Notificar solo si es la primera sesión
                if (sessions.size() == 1) {
                    notifyUserLoggedIn(user);
                }
            }
            return sessions;
        });
    }
    
    // Método antiguo para compatibilidad (asume que user.getId() es el connectionId)
    @Deprecated
    public void userLoggedIn(String username, User user) {
        userLoggedIn(username, user, user.getId());
    }

    public void userLoggedOut(String username, String connectionId) {
        activeUserSessions.computeIfPresent(username, (key, sessions) -> {
            // Buscar el índice del usuario con el connectionId específico
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
                // Verificar si es la última sesión ANTES de removerla
                boolean isLastSession = sessions.size() == 1;
                
                // Remover por índice (NO usar remove(object) porque User.equals() compara por username)
                sessions.remove(indexToRemove);
                
                // Limpiar el mapeo connectionId -> userId
                connectionToUserId.remove(connectionId);
                
                // Notificar DESPUÉS de remover, pasando información de si era la última sesión
                notifyUserLoggedOut(removedUser, isLastSession);
            }
            
            // Si no quedan sesiones, eliminar la entrada
            return sessions.isEmpty() ? null : sessions;
        });
    }

    // Método de compatibilidad con código antiguo
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
    
    // Sobrecarga para compatibilidad con código antiguo
    private void notifyUserLoggedOut(User user) {
        notifyUserLoggedOut(user, true);
    }

    // Retorna todas las sesiones de un usuario
    public List<User> getUserSessions(String username) {
        List<User> sessions = activeUserSessions.get(username);
        return sessions != null ? new ArrayList<>(sessions) : Collections.emptyList();
    }

    // Método de compatibilidad: retorna la primera sesión (para código legacy)
    @Deprecated
    public Map<String, User> getActiveUsers() {
        return activeUserSessions.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get(0)
                ));
    }

    // Nuevo método para obtener todas las sesiones
    public Map<String, List<User>> getAllUserSessions() {
        return Collections.unmodifiableMap(activeUserSessions);
    }
    
    // Nuevo método: obtener userId real desde connectionId
    public String getUserIdFromConnection(String connectionId) {
        return connectionToUserId.get(connectionId);
    }
}