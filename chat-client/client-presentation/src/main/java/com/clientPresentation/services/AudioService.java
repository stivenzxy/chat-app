package com.clientPresentation.services;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AudioService {

    private TargetDataLine microphone;
    private ByteArrayOutputStream recordStream;
    private boolean isRecording = false;
    
    private AudioFormat getAudioFormat() {
        float sampleRate = 44100; // Frecuencia de muestreo CD
        int sampleSizeInBits = 16; // Resolución de 16 bits
        int channels = 1; // Mono para reducir tamaño
        boolean signed = true;
        boolean bigEndian = false; // Cambiado a little-endian para mejor compatibilidad
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, channels, 
                             (sampleSizeInBits / 8) * channels, sampleRate, bigEndian);
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
            byte[] buffer = new byte[4096]; 
            try {
                while (isRecording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        recordStream.write(buffer, 0, bytesRead);
                    }
                    // Pequeña pausa para evitar sobrecarga del CPU
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Error durante la grabación de audio: " + e.getMessage());
            }
        }).start();
    }

    public byte[] stopRecording() {
        if (!isRecording) return null;

        isRecording = false;
        microphone.stop();
        microphone.close();
        
        byte[] rawAudio = recordStream.toByteArray();
        return normalizeAudioVolume(rawAudio);
    }
    
    /**
     * Normaliza el volumen del audio para reducir distorsión y mejorar la calidad
     */
    private byte[] normalizeAudioVolume(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return audioData;
        }
        
        // Encontrar el valor máximo en el audio
        int maxValue = 0;
        for (int i = 0; i < audioData.length; i += 2) {
            if (i + 1 < audioData.length) {
                // Convertir bytes a short (16-bit sample)
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                int absValue = Math.abs(sample);
                if (absValue > maxValue) {
                    maxValue = absValue;
                }
            }
        }
        
        // Si el volumen es muy bajo, no normalizar
        if (maxValue < 1000) {
            return audioData;
        }
        
        // Calcular factor de normalización (evitar saturación)
        double normalizationFactor = 0.8 * Short.MAX_VALUE / maxValue;
        
        byte[] normalizedAudio = new byte[audioData.length];
        for (int i = 0; i < audioData.length; i += 2) {
            if (i + 1 < audioData.length) {
                // Convertir bytes a short, normalizar y convertir de vuelta
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                short normalizedSample = (short) (sample * normalizationFactor);
                
                normalizedAudio[i] = (byte) (normalizedSample & 0xFF);
                normalizedAudio[i + 1] = (byte) ((normalizedSample >> 8) & 0xFF);
            }
        }
        
        return normalizedAudio;
    }

    public void playAudio(byte[] audioData) throws LineUnavailableException, IOException {
        if (audioData == null || audioData.length == 0) {
            throw new IOException("Datos de audio vacíos");
        }
        
        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("El altavoz no es compatible con el formato de audio.");
        }
        
        SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(info);
        speaker.open(format);
        speaker.start();

        try {
            // Reproducir audio en chunks para mejor calidad
            int chunkSize = 4096;
            int offset = 0;
            
            while (offset < audioData.length) {
                int bytesToWrite = Math.min(chunkSize, audioData.length - offset);
                int bytesWritten = speaker.write(audioData, offset, bytesToWrite);
                offset += bytesWritten;
                
                // Pequeña pausa para sincronización
                Thread.sleep(1);
            }
            
            // Esperar a que termine la reproducción
            speaker.drain();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Reproducción de audio interrumpida");
        } finally {
            speaker.close();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}