package com.serverInfrastructure.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AudioTranscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(AudioTranscriptionService.class);
    private static final String VOSK_SERVER_URL = "ws://localhost:2700";
    private static final int SAMPLE_RATE = 16000;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Transcribe audio data to text using Vosk server
     * @param audioData Raw audio data in bytes
     * @return Transcribed text or null if transcription fails
     */
    public String transcribeAudio(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return null;
        }
        
        try {
            File tempAudioFile = createTempAudioFile(audioData);
            if (tempAudioFile == null) {
                return null;
            }
            
            try {
                File convertedAudio = convertAudioForVosk(tempAudioFile);
                if (convertedAudio == null) {
                    return null;
                }
                
                int timeoutSeconds = calculateTimeout(convertedAudio);
                String transcribedText = processAudioWithVosk(convertedAudio, timeoutSeconds);
                
                if (!convertedAudio.equals(tempAudioFile)) {
                    convertedAudio.delete();
                }
                tempAudioFile.delete();
                
                return transcribedText;
                
            } catch (Exception e) {
                tempAudioFile.delete();
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error during audio transcription", e);
            return null;
        }
    }
    
    private boolean isVoskServerAvailable() {
        try {
            String httpUrl = VOSK_SERVER_URL.replace("ws://", "http://");
            URL url = new URL(httpUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            return responseCode == 400 || responseCode == 426;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private File createTempAudioFile(byte[] audioData) {
        try {
            File tempFile = File.createTempFile("chat_audio_", ".wav");
            tempFile.deleteOnExit();
            Files.write(Paths.get(tempFile.getAbsolutePath()), audioData);
            
            try (AudioInputStream testStream = AudioSystem.getAudioInputStream(tempFile)) {
                testStream.getFormat();
            }
            
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convert audio to Vosk-compatible format
     */
    private File convertAudioForVosk(File inputFile) throws Exception {
        AudioInputStream originalStream = AudioSystem.getAudioInputStream(inputFile);
        AudioFormat originalFormat = originalStream.getFormat();
        
        AudioFormat targetFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE,
            16,
            1,
            2,
            SAMPLE_RATE,
            false
        );
        
        if (isFormatCompatible(originalFormat, targetFormat)) {
            return inputFile;
        }
        
        AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);
        
        File tempFile = File.createTempFile("vosk_audio", ".wav");
        tempFile.deleteOnExit();
        
        AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, tempFile);
        
        originalStream.close();
        convertedStream.close();
        
        return tempFile;
    }
    
    /**
     * Check if audio format is compatible with Vosk requirements
     */
    private boolean isFormatCompatible(AudioFormat current, AudioFormat target) {
        return Math.abs(current.getSampleRate() - target.getSampleRate()) < 1 &&
               current.getChannels() == target.getChannels() &&
               current.getSampleSizeInBits() == target.getSampleSizeInBits();
    }
    
    private int calculateTimeout(File audioFile) {
        try (AudioInputStream durStream = AudioSystem.getAudioInputStream(audioFile)) {
            long frames = durStream.getFrameLength();
            float frameRate = durStream.getFormat().getFrameRate();
            if (frames > 0 && frameRate > 0) {
                double durationSec = frames / frameRate;
                return Math.max(180, (int) Math.ceil(durationSec * 8.0 + 120));
            }
        } catch (Exception e) {
            // Use default timeout
        }
        return 180;
    }
    
    private String processAudioWithVosk(File audioFile, int timeoutSeconds) throws Exception {
        URI serverUri = new URI(VOSK_SERVER_URL);
        StringBuilder result = new StringBuilder();
        CompletableFuture<String> processingResult = new CompletableFuture<>();
        boolean[] eofSent = {false};
        long[] lastMessageTime = {System.currentTimeMillis()};
        String[] lastPartialResult = {""};
        
        WebSocketClient client = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(audioFile)) {
                    String config = "{\"config\": {\"sample_rate\": " + SAMPLE_RATE + ", \"words\": true, \"partial\": true, \"max_alternatives\": 0}}";
                    send(config);
                    
                    Thread.sleep(200);

                    byte[] buffer = new byte[2048];
                    int read;
                    int totalSent = 0;
                    int chunkCount = 0;
                    
                    while ((read = pcmStream.read(buffer)) != -1) {
                        if (read > 0) {
                            byte[] chunk = new byte[read];
                            System.arraycopy(buffer, 0, chunk, 0, read);
                            send(chunk);
                            totalSent += read;
                            
                            if (++chunkCount % 5 == 0) {
                                Thread.sleep(100);
                            }
                        }
                    }
                    
                    byte[] silence = new byte[1024];
                    for (int i = 0; i < 3; i++) {
                        send(silence);
                        Thread.sleep(100);
                    }
                    
                    Thread.sleep(1000);
                    send("{\"eof\": 1}");
                    eofSent[0] = true;
                    
                    Thread.sleep(3000);
                    
                    send("{\"flush\": true}");
                    
                    Thread.sleep(2000);
                    
                    String currentResult = result.toString().trim();
                    String finalResult = concatenatePartialResult(currentResult, lastPartialResult[0]);
                    
                    if (!finalResult.equals(currentResult)) {
                        processingResult.complete(finalResult);
                    }

                } catch (Exception e) {
                    logger.error("Error sending audio to Vosk server", e);
                    processingResult.completeExceptionally(e);
                }
            }

            @Override
            public void onMessage(String message) {
                try {
                    lastMessageTime[0] = System.currentTimeMillis();
                    JsonNode jsonResponse = objectMapper.readTree(message);
                    
                    if (jsonResponse.has("text")) {
                        String text = jsonResponse.get("text").asText();
                        if (!text.trim().isEmpty()) {
                            result.append(text).append(" ");
                        }
                    }
                    
                    if (jsonResponse.has("partial")) {
                        String partialText = jsonResponse.get("partial").asText();
                        if (!partialText.trim().isEmpty()) {
                            lastPartialResult[0] = partialText;
                        }
                    }
                    
                    if (eofSent[0]) {
                        if (jsonResponse.has("text")) {
                            String finalText = jsonResponse.get("text").asText();
                            if (!finalText.trim().isEmpty()) {
                                String currentResult = result.toString();
                                if (!currentResult.contains(finalText.trim())) {
                                    result.append(finalText).append(" ");
                                }
                            }
                        }
                        
                        if (System.currentTimeMillis() - lastMessageTime[0] > 5000) {
                            if (!processingResult.isDone()) {
                                String confirmedResult = result.toString().trim();
                                String finalResult = concatenatePartialResult(confirmedResult, lastPartialResult[0]);
                                processingResult.complete(finalResult);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing Vosk server response: {}", message, e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (!processingResult.isDone()) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    String confirmedResult = result.toString().trim();
                    String finalResult = concatenatePartialResult(confirmedResult, lastPartialResult[0]);
                    
                    processingResult.complete(finalResult);
                }
            }

            @Override
            public void onError(Exception ex) {
                logger.error("Error in WebSocket connection with Vosk", ex);
                processingResult.completeExceptionally(ex);
            }
        };
        
        client.connect();
        
        try {
            String finalResult = processingResult.get(timeoutSeconds, TimeUnit.SECONDS);
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return finalResult;
        } finally {
            if (client.isOpen()) {
                client.close();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * Concatenate partial results with confirmed results
     */
    private String concatenatePartialResult(String confirmedResult, String partialResult) {
        if (confirmedResult.isEmpty()) {
            return partialResult;
        } else if (!partialResult.isEmpty() && !confirmedResult.contains(partialResult)) {
            return confirmedResult + " " + partialResult;
        }
        return confirmedResult;
    }
}