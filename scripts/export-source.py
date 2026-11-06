"""Export only repository source inputs into a GitHub-ready ZIP (Python 3.11+)."""
from pathlib import Path
import argparse
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parent.parent
FILES = {"README.md", "LICENSE", "CHANGELOG.md", "AGENTS.md", ".gitignore", ".gitattributes"}
DIRECTORIES = ("docs", "scripts", "branding", "Fabric1201", "NeoForge263")
EXCLUDED = {".git", ".gradle", ".gradle-user-home", ".gradle-temp", "build", "bin", "out", "logs",
            ".idea", ".vscode", ".settings", "__pycache__", ".cache", "node_modules"}


def allowed(path):
    relative = path.relative_to(ROOT)
    if any(part in EXCLUDED or part == "run" or part.startswith("run-") for part in relative.parts):
        return False
    if path.is_symlink():
        raise ValueError(f"Refusing to export a symbolic link: {relative}")
    name = path.name.lower()
    if name == ".env" or name.startswith(".env.") or name in {"local.properties", "gradle-local.properties", "gradle-user.properties", ".ds_store", "thumbs.db"}:
        return False
    if path.suffix.lower() in {".log", ".zip", ".hprof", ".jfr", ".lock", ".pyc", ".pyo"}:
        return False
    if path.suffix.lower() == ".jar" and relative.parts[-3:] != ("gradle", "wrapper", "gradle-wrapper.jar"):
        return False
    return True


def inputs():
    result = [ROOT / name for name in FILES]
    for name in DIRECTORIES:
        directory = ROOT / name
        if not directory.is_dir():
            raise ValueError(f"Missing repository directory: {name}")
        # Walk explicitly so caches and worlds are never traversed.
        pending = [directory]
        while pending:
            parent = pending.pop()
            for child in parent.iterdir():
                if child.is_symlink():
                    raise ValueError(f"Refusing to export link: {child.relative_to(ROOT)}")
                if child.is_dir():
                    if child.name not in EXCLUDED and child.name != "run" and not child.name.startswith("run-"):
                        pending.append(child)
                elif allowed(child):
                    result.append(child)
    return sorted(result, key=lambda path: path.relative_to(ROOT).as_posix())


def export(output=None):
    props = dict(line.split("=", 1) for line in (ROOT / "Fabric1201/gradle.properties").read_text(encoding="utf-8").splitlines()
                 if "=" in line and not line.lstrip().startswith("#"))
    version = props["mod_version"].strip()
    target = Path(output).resolve() if output else ROOT / "build/github" / f"MyCompanion-{version}-source.zip"
    selected = inputs()
    if not selected or any(not path.is_file() for path in selected):
        raise ValueError("Repository input list contains missing files")
    required = {"Fabric1201/gradle/wrapper/gradle-wrapper.jar", "NeoForge263/gradle/wrapper/gradle-wrapper.jar",
                "LICENSE", "branding/resources/assets/my_companion/icon.png", "scripts/gradle.ps1"}
    names = {path.relative_to(ROOT).as_posix() for path in selected}
    if not required <= names:
        raise ValueError(f"Missing build inputs: {required - names}")
    if target in selected:
        raise ValueError("Output cannot overwrite an input file")
    target.parent.mkdir(parents=True, exist_ok=True)
    records = []
    with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in selected:
            relative = path.relative_to(ROOT).as_posix()
            data = path.read_bytes()
            if path.name == "gradlew":
                data = data.replace(b"\r\n", b"\n")
            info = zipfile.ZipInfo("MyCompanion/" + relative, date_time=(1980, 1, 1, 0, 0, 0))
            info.create_system = 3
            info.external_attr = ((0o100755 if path.name == "gradlew" else 0o100644) << 16)
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, data)
            records.append({"file": relative, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})
        info = zipfile.ZipInfo("MyCompanion/SOURCE-MANIFEST.json", date_time=(1980, 1, 1, 0, 0, 0))
        info.external_attr = 0o100644 << 16
        archive.writestr(info, json.dumps({"version": version, "files": records}, ensure_ascii=False, indent=2) + "\n")
    with zipfile.ZipFile(target) as archive:
        bad = archive.testzip()
        if bad:
            raise ValueError(f"Corrupt archive entry: {bad}")
    print(f"Exported {len(records)} source files: {target}")
    return target


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path)
    export(parser.parse_args().output)
