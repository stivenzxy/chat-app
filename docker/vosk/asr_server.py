#!/usr/bin/env python3

import json
import os
import sys
import asyncio
import websockets
from vosk import Model, KaldiRecognizer

MODEL_PATH = sys.argv[1] if len(sys.argv) > 1 else "/opt/vosk-model"
SAMPLE_RATE = 16000

# Cargar modelo
print(f"Loading model from {MODEL_PATH}...", file=sys.stderr)
model = Model(MODEL_PATH)
print("Model loaded successfully", file=sys.stderr)

async def recognize(websocket):
    rec = None
    try:
        async for message in websocket:
            # Si es un mensaje de texto (configuración)
            if isinstance(message, str):
                config = json.loads(message)
                if 'config' in config:
                    sample_rate = config['config'].get('sample_rate', SAMPLE_RATE)
                    rec = KaldiRecognizer(model, sample_rate)
                    rec.SetWords(config['config'].get('words', True))
                elif 'eof' in config:
                    if rec:
                        final_result = json.loads(rec.FinalResult())
                        await websocket.send(json.dumps(final_result))
            # Si es un mensaje binario (audio)
            elif isinstance(message, bytes):
                if rec and rec.AcceptWaveform(message):
                    result = json.loads(rec.Result())
                    await websocket.send(json.dumps(result))
                elif rec:
                    partial = json.loads(rec.PartialResult())
                    await websocket.send(json.dumps(partial))
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
    finally:
        if rec:
            final_result = json.loads(rec.FinalResult())
            if final_result.get('text'):
                await websocket.send(json.dumps(final_result))

async def main():
    print(f"Starting VOSK WebSocket server on port 2700...", file=sys.stderr)
    async with websockets.serve(recognize, "0.0.0.0", 2700):
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    asyncio.run(main())
