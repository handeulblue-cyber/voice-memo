"""Run on the GitHub runner. Never runs inside the Android application."""
import hashlib
import shutil
import time
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-ko-0.22.zip"


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
    shutil.copyfile(ROOT / "licenses/THIRD_PARTY.md", ASSETS / "THIRD_PARTY.md")
    print(f"Bundled model verified, SHA-256: {digest}")


if __name__ == "__main__":
    main()
