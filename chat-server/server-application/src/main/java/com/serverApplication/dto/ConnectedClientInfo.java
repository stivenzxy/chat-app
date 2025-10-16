package com.serverApplication.dto;

public record ConnectedClientInfo(long poolSeq, String connectionId, String ipAddress, int reuseCount) {
}