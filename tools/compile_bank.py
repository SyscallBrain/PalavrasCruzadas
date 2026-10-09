#!/usr/bin/env python3
"""Compiles content/clues/pt-PT/*.jsonl into app/src/main/assets/clues/pt-PT.json.

Refuses to compile if the bank does not validate. The output is deterministic: the same input
gives the same file, byte for byte, sorted by answer.

Format: {"v":1,"entries":[[answer, lemma, [themes], [[text, difficulty, short|null], ...]], ...]}
"""
import json
import sys

from bank_io import COMPILED_FILE, ROOT, load_entries, nfc_upper
from validate_clues import check_bank


def compile_entries(entries: list[dict]) -> dict:
    rows = []
    for e in sorted(entries, key=lambda e: e["answer"]):
        clues = [[c["t"], c["d"], c.get("s")] for c in e["clues"]]
        rows.append([nfc_upper(e["answer"]), nfc_upper(e["lemma"]), list(e["themes"]), clues])
    return {"v": 1, "entries": rows}


def render(bank: dict) -> str:
    return json.dumps(bank, ensure_ascii=False, separators=(",", ":")) + "\n"


def main() -> int:
    entries, parse_errors = load_entries()
    report = check_bank(entries, parse_errors, min_total=0, min_theme=0)
    if report.problems:
        report.print()
        print("compile_bank: refused, the bank does not validate")
        return 1
    COMPILED_FILE.parent.mkdir(parents=True, exist_ok=True)
    COMPILED_FILE.write_text(render(compile_entries(entries)), encoding="utf-8", newline="\n")
    print(f"compile_bank: {len(entries)} answers -> {COMPILED_FILE.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
