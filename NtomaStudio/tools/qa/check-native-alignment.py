#!/usr/bin/env python3
"""Check 64-bit ELF LOAD segments in APK/AAB native libraries for 16 KB alignment."""
import struct
import sys
import zipfile
from pathlib import Path


def check(path):
    checked = 0
    failures = []
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            if not name.endswith(".so") or not any(f"/{abi}/" in name for abi in ("arm64-v8a", "x86_64")):
                continue
            data = archive.read(name)
            if data[:5] != b"\x7fELF\x02":
                failures.append(f"{name}: not ELF64")
                continue
            endian = "<" if data[5] == 1 else ">"
            phoff = struct.unpack_from(endian + "Q", data, 32)[0]
            entsize, count = struct.unpack_from(endian + "HH", data, 54)
            for i in range(count):
                fields = struct.unpack_from(endian + "IIQQQQQQ", data, phoff + i * entsize)
                if fields[0] == 1:
                    offset, address, alignment = fields[2], fields[3], fields[7]
                    if alignment < 16384 or (offset - address) % 16384 != 0:
                        failures.append(f"{name}: LOAD segment is not 16 KB aligned")
            checked += 1
    if not checked:
        raise SystemExit(f"{path}: no 64-bit native libraries checked")
    if failures:
        raise SystemExit("\n".join(failures))
    print(f"PASS: {Path(path).name}: {checked} 64-bit libraries have 16 KB ELF alignment. APK ZIP alignment checked separately.")


if __name__ == "__main__":
    for argument in sys.argv[1:]:
        check(argument)
