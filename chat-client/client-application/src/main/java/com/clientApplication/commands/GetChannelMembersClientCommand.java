package com.clientApplication.commands;

import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GetChannelMembersClientCommand implements ClientCommand<String, List<String>> {

    private final ServerGatewayPort gateway;

    private static final Map<String, String> userIdToNameCache = new ConcurrentHashMap<>();

    public GetChannelMembersClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public List<String> execute(String channelId) {
        try {
            List<String> responseParts = gateway.sendAndReceive(
                    "GET_CHANNEL_MEMBERS",
                    channelId);

            if (!responseParts.isEmpty() && "OK".equals(responseParts.get(0))) {
                String membersPayload = responseParts.size() > 1 ? responseParts.get(1) : "";
                if (membersPayload.isEmpty()) {
                    return List.of();
                }

                List<String> usernames = new ArrayList<>();
                for (String pair : membersPayload.split(",")) {
                    String[] parts = pair.split(":", 2);
                    if (parts.length == 2) {
                        String userId = parts[0];
                        String username = parts[1];
                        userIdToNameCache.put(userId, username);
                        usernames.add(username);
                        System.out.println("DEBUG: Cached user " + userId + " -> " + username);
                    }
                }
                System.out.println("DEBUG: Cache size after loading members: " + userIdToNameCache.size());
                return usernames;
            } else {
                return List.of();
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception in GetChannelMembersClientCommand: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public static String getUsernameFromCache(String userId) {
        return userIdToNameCache.get(userId);
    }

    public static Map<String, String> getUserCache() {
        return new ConcurrentHashMap<>(userIdToNameCache);
    }

    public static class Request {
        private final String channelId;

        public Request(String channelId) {
            this.channelId = channelId;
        }

        public String getChannelId() {
            return channelId;
        }
    }
}
