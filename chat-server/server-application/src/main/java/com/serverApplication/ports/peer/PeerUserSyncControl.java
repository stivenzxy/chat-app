package com.serverApplication.ports.peer;

import java.util.List;
import java.util.Map;

public interface PeerUserSyncControl {
    void notifyUserChangeToPeers(String username, String action);
    Map<String, List<String>> getAllUsersAcrossPeers();
}