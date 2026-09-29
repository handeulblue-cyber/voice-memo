"""Public sample smoke comparison, not a claim about a user's microphone/accent."""
import json
import re
import tempfile
import wave
import zipfile
from pathlib import Path
import numpy as np
import sherpa_onnx
import vosk
from prepare_model import ASSETS, ROOT, download

BASE = "https://huggingface.co/k2-fsa/sherpa-onnx-streaming-zipformer-korean-2024-06-16/resolve/ba6078bca4daf3f0dd37f79d0ab505af71df14a6/test_wavs/"
REFERENCES = ["그는 괜찮은 척하려고 애쓰는 것 같았다.", "지하철에서 다리를 벌리고 앉지 마라.", "부모가 저지르는 큰 실수 중 하나는 자기 아이를 다른 집 아이와 비교하는 것이다.", "주민등록증을 보여 주시겠어요?"]


def chars(text):
    return re.sub(r"[^가-힣0-9a-z]", "", text.lower())


def distance(a, b):
    row = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        following = [i]
        for j, y in enumerate(b, 1):
            following.append(min(following[-1] + 1, row[j] + 1, row[j - 1] + (x != y)))
        row = following
    return row[-1]


def main():
    rows = []
    with tempfile.TemporaryDirectory() as temporary:
        target = Path(temporary)
        with zipfile.ZipFile(ASSETS / "korean.zip") as archive:
            archive.extractall(target)
        with zipfile.ZipFile(ASSETS / "whisper-small.zip") as archive:
            archive.extractall(target / "whisper")
        vosk.SetLogLevel(-1)
        baseline = vosk.Model(str(target / "vosk-model-small-ko-0.22"))
        for word in ["녹음", "끝", "[unk]"]:
            assert baseline.vosk_model_find_word(word) >= 0, f"Missing command vocabulary: {word}"
        def check_no_command(data):
            command = vosk.KaldiRecognizer(baseline, 16000, json.dumps(["녹음 끝", "[unk]"], ensure_ascii=False))
            command.SetWords(True)
            # Feed trailing silence to exercise natural endpoint finals, like the app.
            data += bytes(16000 * 2 * 3)
            finals = []
            for start in range(0, len(data), 8000):
                if command.AcceptWaveform(data[start:start + 8000]):
                    result = json.loads(command.Result())
                    finals.append(result)
                    words = result.get("result", [])
                    recognized = [w["word"] for w in words]
                    stop = (re.sub(r"\s+", "", result.get("text", "")) == "녹음끝"
                            and recognized in [["녹음", "끝"], ["녹음끝"]]
                            and all(0.85 <= w.get("conf", 0) <= 1 for w in words))
                    assert not stop, f"False command on negative sample: {result}"
            return finals
        check_no_command(bytes(16000 * 2 * 5))
        whisper = target / "whisper"
        improved = sherpa_onnx.OfflineRecognizer.from_whisper(
            encoder=str(whisper / "small-encoder.int8.onnx"),
            decoder=str(whisper / "small-decoder.int8.onnx"),
            tokens=str(whisper / "small-tokens.txt"), language="ko", task="transcribe", num_threads=2,
        )
        for index, reference in enumerate(REFERENCES):
            wav = target / f"{index}.wav"
            download(BASE + wav.name, wav)
            with wave.open(str(wav)) as source:
                assert source.getnchannels() == 1 and source.getsampwidth() == 2
                rate = source.getframerate()
                data = source.readframes(source.getnframes())
            assert rate == 16000
            command_finals = check_no_command(data)
            rec = vosk.KaldiRecognizer(baseline, rate)
            final_parts = []
            for start in range(0, len(data), 8000):
                if rec.AcceptWaveform(data[start:start + 8000]):
                    final_parts.append(json.loads(rec.Result())["text"])
            final_parts.append(json.loads(rec.FinalResult())["text"])
            old = " ".join(final_parts).strip()
            stream = improved.create_stream()
            stream.accept_waveform(rate, np.frombuffer(data, dtype=np.int16).astype(np.float32) / 32768)
            improved.decode_stream(stream)
            new = stream.result.text.strip()
            assert re.search(r"[가-힣]", new), "Korean inference returned no Hangul"
            row = dict(reference=reference, vosk=old, whisper=new, characters=len(chars(reference)),
                       vosk_errors=distance(chars(reference), chars(old)), whisper_errors=distance(chars(reference), chars(new)),
                       command_negative_finals=command_finals)
            rows.append(row)
            print(json.dumps(row, ensure_ascii=False), flush=True)
    report = {"scope": "Four public clean samples only; personal pronunciation untested", "samples": rows}
    total = sum(r["characters"] for r in rows)
    report["vosk_cer"] = sum(r["vosk_errors"] for r in rows) / total
    report["whisper_cer"] = sum(r["whisper_errors"] for r in rows) / total
    (ROOT / "stt-comparison.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "samples"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
