"""Run on the GitHub runner. Never runs inside the Android application."""
import hashlib
import shutil
import time
import urllib.request
import zipfile
import tarfile
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-ko-0.22.zip"
WHISPER_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-small.tar.bz2"
SHERPA_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.12.14/sherpa-onnx-1.12.14.aar"
SHERPA_SHA = "5a629a899888cb2760e245d9d5340858b15591aee7fa13644cf57199ff2829b9"


def download(url: str, target: Path) -> None:
    partial = target.with_suffix(target.suffix + ".part")
    for attempt in range(3):
        try:
            print(f"Downloading {url}", flush=True)
            with urllib.request.urlopen(url, timeout=120) as response, partial.open("wb") as output:
                shutil.copyfileobj(response, output)
            partial.replace(target)
            return
        except Exception:
            partial.unlink(missing_ok=True)
            if attempt == 2:
                raise
            time.sleep(3 * (attempt + 1))


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    model = ASSETS / "korean.zip"
    download(MODEL_URL, model)
    with zipfile.ZipFile(model) as archive:
        assert archive.testzip() is None, "Corrupt model ZIP"
        assert "vosk-model-small-ko-0.22/am/final.mdl" in archive.namelist()
    digest = hashlib.sha256(model.read_bytes()).hexdigest()
    (ROOT / "licenses/model-sha256.txt").write_text(f"{digest}  korean.zip\n", encoding="utf-8")
    download("https://www.apache.org/licenses/LICENSE-2.0.txt", ASSETS / "Apache-2.0.txt")
    download("https://raw.githubusercontent.com/openai/whisper/main/LICENSE", ASSETS / "Whisper-MIT.txt")
    download("https://raw.githubusercontent.com/microsoft/onnxruntime/v1.17.1/LICENSE", ASSETS / "ONNXRuntime-MIT.txt")
    libraries = ROOT / "app/libs"
    libraries.mkdir(parents=True, exist_ok=True)
    aar = libraries / "sherpa-onnx-1.12.14.aar"
    download(SHERPA_URL, aar)
    assert hashlib.sha256(aar.read_bytes()).hexdigest() == SHERPA_SHA, "sherpa-onnx checksum mismatch"
    with tempfile.TemporaryDirectory() as temporary:
        archive_file = Path(temporary) / "whisper.tar.bz2"
        download(WHISPER_URL, archive_file)
        names = ["small-encoder.int8.onnx", "small-decoder.int8.onnx", "small-tokens.txt"]
        with tarfile.open(archive_file, "r:bz2") as archive, zipfile.ZipFile(ASSETS / "whisper-small.zip", "w", zipfile.ZIP_STORED) as packed:
            for name in names:
                source = archive.extractfile("sherpa-onnx-whisper-small/" + name)
                assert source is not None, name
                with source, packed.open(name, "w") as out:
                    shutil.copyfileobj(source, out)
    whisper_hash = hashlib.sha256((ASSETS / "whisper-small.zip").read_bytes()).hexdigest()
    with (ROOT / "licenses/model-sha256.txt").open("a", encoding="utf-8") as out:
        out.write(f"{whisper_hash}  whisper-small.zip\n{SHERPA_SHA}  sherpa-onnx-1.12.14.aar\n")
    shutil.copyfile(ROOT / "licenses/THIRD_PARTY.md", ASSETS / "THIRD_PARTY.md")
    print(f"Bundled model verified, SHA-256: {digest}")
    print(f"Bundled Korean second-pass model: {whisper_hash}")


if __name__ == "__main__":
    main()
