package com.serverApplication.dto.sync;

import java.util.List;

public record FullDatabaseSyncDTO(
    List<UserSyncDTO> users,
    List<ChannelSyncDTO> channels,
    List<ChannelMemberSyncDTO> channelMembers,
    List<ChannelInviteSyncDTO> channelInvites,
    List<MessageSyncDTO> messages,
    List<AudioTranscriptionSyncDTO> transcriptions
) {}
