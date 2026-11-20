"""Repackage a Gradle debug APK for Android 1.0 and verify its v1 signature."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile
from zipfile import ZipFile

from convert_arsc_utf16 import convert_arsc, convert_xml


PROJECT = Path(__file__).resolve().parents[2]
DEBUG_OUTPUT = PROJECT / "app" / "build" / "outputs" / "apk" / "debug"
DEFAULT_DEBUG_KEY = Path.home() / ".android" / "debug.keystore"


def is_stale_jar_metadata(name):
    directory, _, filename = name.upper().rpartition("/")
    return directory == "META-INF" and (
        filename in ("MANIFEST.MF", "INDEX.LIST")
        or filename.startswith("SIG-")
        or filename.endswith((".SF", ".RSA", ".DSA", ".EC"))
    )


def repack(source, output):
    with ZipFile(source) as apk, ZipFile(output, "w") as rewritten:
        for entry in apk.infolist():
            name = entry.filename
            if is_stale_jar_metadata(name):
                continue
            data = apk.read(entry)
            if name == "resources.arsc":
                data = convert_arsc(data)
            elif name == "AndroidManifest.xml" or (name.startswith("res/") and name.endswith(".xml")):
                data = convert_xml(data)
            rewritten.writestr(entry, data)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build-tools", required=True, type=Path,
                        help="Android SDK build-tools/28.0.3 directory")
    parser.add_argument("--keystore", type=Path, default=DEFAULT_DEBUG_KEY,
                        help="Gradle debug keystore (default: ~/.android/debug.keystore)")
    parser.add_argument("--input", type=Path, default=DEBUG_OUTPUT / "app-debug.apk")
    parser.add_argument("--output", type=Path, default=DEBUG_OUTPUT / "app-debug-api1-signed.apk")
    args = parser.parse_args()

    exe_suffix = ".exe" if os.name == "nt" else ""
    java_home = os.environ.get("JAVA_HOME")
    java = Path(java_home) / "bin" / ("java" + exe_suffix) if java_home else "java"
    zipalign = args.build_tools / ("zipalign" + exe_suffix)
    signer = [java, "-jar", args.build_tools / "lib" / "apksigner.jar"]
    for path in (zipalign, signer[-1], args.keystore, args.input):
        if not path.is_file():
            parser.error(f"file not found: {path}")
    if args.input.resolve() == args.output.resolve():
        parser.error("input and output must be different files")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=args.output.parent) as work:
        raw = Path(work) / "rewritten.apk"
        aligned = Path(work) / "aligned.apk"
        signed = Path(work) / "signed.apk"
        repack(args.input, raw)
        subprocess.run([zipalign, "-f", "4", raw, aligned], check=True)
        subprocess.run(signer + [
            "sign", "--ks", args.keystore, "--ks-pass", "pass:android",
            "--key-pass", "pass:android", "--ks-key-alias", "androiddebugkey",
            "--min-sdk-version", "1", "--v1-signing-enabled", "true",
            "--v2-signing-enabled", "false",
            "--out", signed, aligned,
        ], check=True)
        subprocess.run(signer + ["verify", "--min-sdk-version", "1", signed], check=True)
        signed.replace(args.output)
    print(args.output)


if __name__ == "__main__":
    main()
