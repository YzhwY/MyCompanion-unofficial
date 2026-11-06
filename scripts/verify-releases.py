"""Check both release archives using only Python 3.11+ standard library."""
from pathlib import Path
import argparse
import hashlib
import json
import struct
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parent.parent
NAME = "我的伙伴非官方移植版"
ICON_PATH = "assets/my_companion/icon.png"
ICON_MASTER = ROOT / "branding/resources" / ICON_PATH


def properties(path):
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip() and not line.lstrip().startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def verify(edition=None):
    releases = []
    license_text = (ROOT / "LICENSE").read_text(encoding="utf-8").replace("\r\n", "\n")
    assert license_text.startswith("MIT License\n"), "Expected MIT license"
    icon = ICON_MASTER.read_bytes()
    assert icon[:8] == b"\x89PNG\r\n\x1a\n", "Icon must be a PNG"
    width, height = struct.unpack(">II", icon[16:24])
    assert width == height and width > 0, "Icon must be square"
    origin = json.loads((ROOT / "branding/source.json").read_text(encoding="utf-8"))
    assert hashlib.sha256(icon).hexdigest() == origin["sha256"], "Icon differs from user-provided source"
    versions = {properties(ROOT / name / "gradle.properties")["mod_version"] for name in ("Fabric1201", "NeoForge263")}
    for folder, loader, java_major in [("Fabric1201", "Fabric", 61), ("NeoForge263", "NeoForge", 69)]:
        if edition and folder != edition:
            continue
        project = ROOT / folder
        props = properties(project / "gradle.properties")
        version = props["mod_version"]
        versions.add(version)
        stem = f"[{NAME}]my-companion-unofficial-{version}-{loader}{props['minecraft_version']}"
        jar = project / "build/libs" / (stem + ".jar")
        sources = jar.with_name(stem + "-sources.jar")
        if not sources.is_file():
            raise ValueError(f"Missing sources: {sources}")
        with zipfile.ZipFile(jar) as archive:
            entries = archive.namelist()
            assert archive.read("LICENSE").decode("utf-8").replace("\r\n", "\n") == license_text
            assert archive.read(ICON_PATH) == icon, "Packaged icon differs from user-provided image"
            manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8").replace("\r\n ", "").replace("\n ", "")
            assert f"Implementation-Title: {NAME}" in manifest
            assert f"Implementation-Version: {version}" in manifest
            if any("CompanionGameTests" in entry for entry in entries):
                raise ValueError(f"Test entrypoint leaked into release: {jar}")
            assert not any("test_arena.nbt" in entry for entry in entries), "Test arena leaked into release"
            client_classes = {str(source.relative_to(project / "src/client/java").with_suffix("")).replace("\\", "/")
                              for source in (project / "src/client/java").rglob("*.java")}
            for entry in entries:
                if entry.endswith(".class"):
                    data = archive.read(entry)
                    if struct.unpack(">H", data[6:8])[0] != java_major:
                        raise ValueError(f"Unexpected Java bytecode: {entry}")
                    if "/client/" not in entry and not any(entry.startswith(name + ".") or entry.startswith(name + "$") for name in client_classes) and b"net/minecraft/client/" in data:
                        raise ValueError(f"Client reference in shared class: {entry}")
            for lang in ("zh_cn", "en_us"):
                translations = json.loads(archive.read(f"assets/my_companion/lang/{lang}.json"))
                for key in ("screen.my_companion.title", "screen.my_companion.feet", "screen.my_companion.traps", "message.my_companion.not_owner"):
                    assert translations.get(key), f"Missing translation: {key}"
            if loader == "Fabric":
                metadata = json.loads(archive.read("fabric.mod.json"))
                assert metadata["id"] == "my_companion" and metadata["version"] == version
                assert metadata["name"] == NAME and metadata["depends"]["minecraft"] == props["minecraft_version"]
                assert metadata["icon"] == ICON_PATH
                assert metadata["license"] == "MIT"
                assert "fabric-gametest" not in metadata["entrypoints"], "Test entrypoint leaked into metadata"
                for entrypoints in metadata["entrypoints"].values():
                    for entrypoint in entrypoints:
                        assert entrypoint.replace(".", "/") + ".class" in entries
            else:
                metadata = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode("utf-8"))
                assert metadata["license"] == "MIT"
                mod = metadata["mods"][0]
                assert mod["modId"] == "my_companion" and mod["version"] == version and mod["displayName"] == NAME
                assert mod["iconFile"] == ICON_PATH and mod["bannerFile"] == ICON_PATH and mod["iconBlur"] is False
                assert "logoFile" not in mod, "Avoid the deprecated 26.3 logo property"
                dependencies = {dep["modId"]: dep for dep in metadata["dependencies"]["my_companion"]}
                assert dependencies["minecraft"]["versionRange"] == props["minecraft_version_range"]
                assert dependencies["neoforge"]["versionRange"] == f"[{props['neo_version']},)"
                assert "dev/codex/mycompanion/MyCompanionMod.class" in entries
                assert "dev/codex/mycompanion/client/MyCompanionClient.class" in entries
        with zipfile.ZipFile(sources) as archive:
            assert archive.read("LICENSE").decode("utf-8").replace("\r\n", "\n") == license_text
            assert "dev/codex/mycompanion/CompanionManager.java" in archive.namelist()
            assert archive.read(ICON_PATH) == icon, "Source archive must include the original icon"
            assert not any("CompanionGameTests" in entry or "test_arena.nbt" in entry for entry in archive.namelist())
        releases.append({"edition": loader, "minecraft": props["minecraft_version"], "mod_version": version,
                         "file": str(jar.relative_to(ROOT)), "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
                         "display_name": NAME, "icon_path": ICON_PATH, "icon_sha256": hashlib.sha256(icon).hexdigest()})
    if len(versions) != 1:
        raise ValueError(f"Edition versions differ: {versions}")
    output = ROOT / "build" / (f"release-manifest-{edition}.json" if edition else "release-manifest.json")
    output.parent.mkdir(exist_ok=True)
    output.write_text(json.dumps(releases, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Selected release archives verified; manifest: " + str(output))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--edition", choices=("Fabric1201", "NeoForge263"))
    verify(parser.parse_args().edition)
