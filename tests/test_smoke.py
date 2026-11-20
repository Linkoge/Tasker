"""Smoke checks: project structure, committed APK, and the JVM unit tests for the core logic."""
import os
import shutil
import subprocess
import zipfile
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
pytestmark = pytest.mark.smoke


def test_project_structure():
    for rel in (
        "settings.gradle.kts",
        "gradlew",
        "app/build.gradle.kts",
        "app/src/main/AndroidManifest.xml",
        "app/src/main/java/dev/tasker/domain/TaskLogic.kt",
        "app/src/test/java/dev/tasker/TaskLogicTest.kt",
        "app/src/test/java/dev/tasker/TaskRepositoryTest.kt",
        "README.md",
    ):
        assert (ROOT / rel).is_file(), rel


def test_apk_is_present_and_looks_installable():
    apk = ROOT / "Tasker.apk"
    assert apk.is_file() and apk.stat().st_size > 100_000
    with zipfile.ZipFile(apk) as z:
        names = z.namelist()
        assert "AndroidManifest.xml" in names
        assert any(n.startswith("classes") and n.endswith(".dex") for n in names)
        assert any(n.startswith("META-INF/") for n in names), "APK looks unsigned"
        assert b"dev.tasker" in z.read("AndroidManifest.xml").replace(b"\x00", b"")


def test_readme_documents_build_and_apk():
    text = (ROOT / "README.md").read_text(encoding="utf-8")
    assert "Tasker.apk" in text and "assembleDebug" in text


@pytest.mark.skipif(
    not os.environ.get("ANDROID_HOME") and not (Path.home() / "Android" / "Sdk").is_dir(),
    reason="Android SDK not available",
)
@pytest.mark.skipif(shutil.which("java") is None, reason="java not available")
def test_jvm_unit_tests_pass():
    """Runs the priority-sort / reorder / move unit tests (offline-capable once Gradle caches are warm)."""
    env = dict(os.environ)
    env.setdefault("ANDROID_HOME", str(Path.home() / "Android" / "Sdk"))
    result = subprocess.run(
        ["./gradlew", "--no-daemon", "-q", ":app:testDebugUnitTest"],
        cwd=ROOT, env=env, capture_output=True, text=True, timeout=540,
    )
    assert result.returncode == 0, result.stdout[-2000:] + result.stderr[-2000:]
