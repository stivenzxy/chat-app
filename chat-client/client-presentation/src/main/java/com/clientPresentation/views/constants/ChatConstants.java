package com.clientPresentation.views.constants;


public final class ChatConstants {
    public static final String SELF_DISPLAY_NAME = "Yo";
    public static final String AUDIO_BUTTON_RECORD = "🎤";
    public static final String AUDIO_BUTTON_STOP = "⏹️";

    public static final String ERROR_AUDIO_ACCESS = "Error al acceder al micrófono: ";
    public static final String ERROR_AUDIO_TITLE = "Error de Audio";
    public static final String ERROR_LOAD_HISTORY = "Error al cargar el historial de chat.";
    public static final String ERROR_LOAD_HISTORY_TITLE = "Error";
    

    private ChatConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
