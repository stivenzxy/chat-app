package com.clientApplication.listeners;

import com.clientApplication.events.PrivateAudioEvent;

public interface PrivateAudioListener {
    void onPrivateAudioReceived(PrivateAudioEvent event);
}