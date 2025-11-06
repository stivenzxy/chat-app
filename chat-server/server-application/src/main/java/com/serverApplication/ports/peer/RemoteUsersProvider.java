package com.serverApplication.ports.peer;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface RemoteUsersProvider {
    Map<String, List<String>> getRemoteUsers();
}
