#!/usr/bin/env python3
"""Spell-checks answers and clues with hunspell pt_PT.

Needs 'hunspell' and the pt_PT dictionary (Debian: apt install hunspell hunspell-pt-pt).
Without hunspell it prints a warning and exits 0, unless --require is passed (used in CI).
Legitimate words the dictionary does not know go into content/spelling_allow.txt.
"""
import argparse
import shutil
import subprocess
import sys

from bank_io import accented_words, clue_words, load_entries, load_spelling_allow, plain


def unknown_words(words: list[str]) -> list[str]:
    result = subprocess.run(
        ["hunspell", "-d", "pt_PT", "-l"],
        input="\n".join(words) + "\n",
        capture_output=True,
        text=True,
        check=True,
    )
    return sorted(set(result.stdout.split()))


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--require", action="store_true", help="fail if hunspell is not installed (CI)")
    args = parser.parse_args(argv)

    if shutil.which("hunspell") is None:
        message = "check_spelling: hunspell is not installed (apt install hunspell hunspell-pt-pt)"
        if args.require:
            print(f"FAIL {message}")
            return 1
        print(f"SKIP {message}")
        return 0

    entries, _ = load_entries()
    allow = load_spelling_allow()
    words: set[str] = set()
    for e in entries:
        words.update(accented_words(e["answer"]))
        for clue in e["clues"]:
            words.update(clue_words(clue["t"]))
    words = {w for w in words if plain(w) not in allow}
    bad = unknown_words(sorted(words))
    for word in bad:
        print(f"FAIL unknown word {word!r}")
    if bad:
        print(f"check_spelling: {len(bad)} unknown words (add legitimate ones to content/spelling_allow.txt)")
        return 1
    print(f"check_spelling: ok ({len(words)} distinct words)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
