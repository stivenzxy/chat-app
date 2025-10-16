package com.clientPresentation.services;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AudioService {

    private TargetDataLine microphone;
    private ByteArrayOutputStream recordStream;
    private boolean isRecording = false;

    // Formato de audio estándar (calidad CD, mono)
    private AudioFormat getAudioFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 1; // Mono
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public void startRecording() throws LineUnavailableException {
        if (isRecording) return;

        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("El micrófono no es compatible con el formato de audio.");
        }

        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        recordStream = new ByteArrayOutputStream();
        isRecording = true;

        // Hilo para capturar el audio
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isRecording) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    recordStream.write(buffer, 0, bytesRead);
                }
            }
        }).start();
    }

    public byte[] stopRecording() {
        if (!isRecording) return null;

        isRecording = false;
        microphone.stop();
        microphone.close();
        return recordStream.toByteArray();
    }

    public void playAudio(byte[] audioData) throws LineUnavailableException, IOException {
        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(info);

        speaker.open(format);
        speaker.start();

        // Escribir los datos en el altavoz
        speaker.write(audioData, 0, audioData.length);

        // Limpiar y cerrar
        speaker.drain();
        speaker.close();
    }

    public boolean isRecording() {
        return isRecording;
    }
}