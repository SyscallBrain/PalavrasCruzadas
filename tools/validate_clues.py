#!/usr/bin/env python3
"""Validates the clue bank.

Exits with a non-zero code and prints one FAIL line per problem. Wired into Gradle
(validateClues task), so an invalid bank blocks the tests and the build.

Usage:
  python3 tools/validate_clues.py                     # rules that always apply
  python3 tools/validate_clues.py --min-total 1500    # require a minimum total
  python3 tools/validate_clues.py --min-theme 180     # require a minimum per theme (except geral)
"""
import argparse
import difflib
import sys
from collections import Counter

from bank_io import (
    ALPHABET, AO_BLACKLIST, BR_BLACKLIST, load_entries, load_exclusions,
    load_themes, nfc_upper, plain, tokens,
)

ANSWER_MIN, ANSWER_MAX = 3, 13
CLUE_MAX, SHORT_MAX = 60, 14
MIN_CLUES, MAX_CLUES = 2, 4
LEVELS = (1, 2, 3)
MIN_SHORT_RATIO = 0.30      # entries with at least one short clue
MIN_PLAIN_SHORT_RATIO = 0.75  # 3–5 letter answers without accents
NEAR_DUPLICATE = 0.85       # SequenceMatcher: two nearly identical clues

BLACKLIST = BR_BLACKLIST | AO_BLACKLIST


class Report:
    def __init__(self):
        self.problems: list[str] = []

    def fail(self, where: str, message: str) -> None:
        self.problems.append(f"{where}: {message}")

    def print(self) -> None:
        for p in self.problems:
            print(f"FAIL {p}")


def check_entry(entry: dict, themes: dict, exclusions: set, report: Report) -> None:
    where = f"{entry['file']}:{entry['line']}"
    answer = entry.get("answer")
    if not isinstance(answer, str):
        report.fail(where, "answer missing or not a string")
        return
    if nfc_upper(answer) != answer:
        report.fail(where, f"answer {answer!r} is not NFC uppercase")
    if not (ANSWER_MIN <= len(answer) <= ANSWER_MAX):
        report.fail(where, f"answer {answer!r} has {len(answer)} letters, need {ANSWER_MIN}-{ANSWER_MAX}")
    bad = sorted({c for c in answer if c not in ALPHABET})
    if bad:
        report.fail(where, f"answer {answer!r} has characters outside the alphabet: {bad}")
    if plain(answer) in BLACKLIST:
        report.fail(where, f"answer {answer!r} is a blacklisted spelling")
    if plain(answer) in exclusions:
        report.fail(where, f"answer {answer!r} is excluded")

    lemma = entry.get("lemma")
    if not isinstance(lemma, str) or nfc_upper(lemma) != lemma or not lemma:
        report.fail(where, f"lemma {lemma!r} missing or not NFC uppercase")
    elif not set(lemma) <= ALPHABET:
        report.fail(where, f"lemma {lemma!r} has characters outside the alphabet")

    theme_ids = entry.get("themes")
    if not isinstance(theme_ids, list) or not (1 <= len(theme_ids) <= 3):
        report.fail(where, f"themes must be a list of 1-3 ids, got {theme_ids!r}")
        theme_ids = []
    for tid in theme_ids:
        if tid not in themes:
            report.fail(where, f"unknown theme {tid!r}")
    if len(set(theme_ids)) != len(theme_ids):
        report.fail(where, "repeated theme ids")
    if theme_ids and theme_ids[0] in themes and entry["file"] != f"content/clues/pt-PT/{theme_ids[0]}.jsonl":
        report.fail(where, f"entry must live in the file of its first theme ({theme_ids[0]}.jsonl)")

    clues = entry.get("clues")
    if not isinstance(clues, list) or not (MIN_CLUES <= len(clues) <= MAX_CLUES):
        report.fail(where, f"{answer!r}: need {MIN_CLUES}-{MAX_CLUES} clues")
        return

    # Answer stem: first 4 characters without accents (the whole answer if shorter).
    answer_root = plain(answer)[:4]
    lemma_root = plain(lemma)[:4] if lemma else ""

    texts = []
    for clue in clues:
        if not isinstance(clue, dict):
            report.fail(where, f"{answer!r}: clue is not an object")
            continue
        text = clue.get("t")
        level = clue.get("d")
        short = clue.get("s")
        if not isinstance(text, str) or not text:
            report.fail(where, f"{answer!r}: clue text missing")
            continue
        if len(text) > CLUE_MAX:
            report.fail(where, f"{answer!r}: clue longer than {CLUE_MAX} chars: {text!r}")
        if text != text.strip() or "  " in text:
            report.fail(where, f"{answer!r}: clue has stray spaces: {text!r}")
        if text.endswith("."):
            report.fail(where, f"{answer!r}: clue must not end with a full stop: {text!r}")
        if not (text[0].isupper() or text[0].isdigit()):
            report.fail(where, f"{answer!r}: clue must start with a capital: {text!r}")
        if level not in LEVELS:
            report.fail(where, f"{answer!r}: difficulty d must be 1, 2 or 3, got {level!r}")
        if short is not None:
            if not isinstance(short, str) or not short or len(short) > SHORT_MAX:
                report.fail(where, f"{answer!r}: short clue s must be 1-{SHORT_MAX} chars: {short!r}")
        words = tokens(text)
        for word in words:
            if word in BLACKLIST or word in exclusions:
                report.fail(where, f"{answer!r}: clue uses blacklisted word {word!r}: {text!r}")
            if answer_root and word.startswith(answer_root):
                report.fail(where, f"{answer!r}: clue reveals the answer root ({word!r}): {text!r}")
            if lemma_root and word.startswith(lemma_root):
                report.fail(where, f"{answer!r}: clue reveals the lemma root ({word!r}): {text!r}")
        texts.append(text)

    for i, a in enumerate(texts):
        for b in texts[i + 1:]:
            if difflib.SequenceMatcher(None, plain(a), plain(b)).ratio() >= NEAR_DUPLICATE:
                report.fail(where, f"{answer!r}: two almost identical clues: {a!r} / {b!r}")


def check_bank(entries: list[dict], parse_errors: list[str], min_total: int, min_theme: int) -> Report:
    report = Report()
    for err in parse_errors:
        report.fail(err.split(":", 1)[0], err)
    themes = load_themes()
    exclusions = load_exclusions()

    answers = Counter()
    clue_texts = Counter()
    for entry in entries:
        check_entry(entry, themes, exclusions, report)
        if isinstance(entry.get("answer"), str):
            answers[entry["answer"]] += 1
    for answer, count in answers.items():
        if count > 1:
            report.fail("bank", f"duplicate answer {answer!r} ({count} times)")

    # The same clue used for different answers.
    seen_clue: dict[str, str] = {}
    for entry in entries:
        for clue in entry.get("clues") or []:
            if isinstance(clue, dict) and isinstance(clue.get("t"), str):
                key = plain(clue["t"])
                owner = seen_clue.get(key)
                if owner and owner != entry.get("answer"):
                    report.fail("bank", f"same clue for {owner!r} and {entry.get('answer')!r}: {clue['t']!r}")
                seen_clue.setdefault(key, entry.get("answer") or "")
                clue_texts[key] += 1

    # Global targets.
    total = len(entries)
    if total < min_total:
        report.fail("bank", f"{total} answers, need at least {min_total}")
    if total:
        with_short = sum(
            1 for e in entries
            if any(isinstance(c, dict) and c.get("s") for c in (e.get("clues") or []))
        )
        if with_short / total < MIN_SHORT_RATIO:
            report.fail("bank", f"only {with_short}/{total} answers have a short clue (need 30%)")
        short_words = [e["answer"] for e in entries if isinstance(e.get("answer"), str) and 3 <= len(e["answer"]) <= 5]
        if short_words:
            plain_short = sum(1 for w in short_words if plain(w) == w)
            if plain_short / len(short_words) < MIN_PLAIN_SHORT_RATIO:
                report.fail("bank", f"{plain_short}/{len(short_words)} short answers without accents (need 75%)")
    if min_theme:
        per_theme = Counter()
        for e in entries:
            for tid in e.get("themes") or []:
                per_theme[tid] += 1
        for tid in load_themes():
            if tid == "geral":
                continue
            if per_theme[tid] < min_theme:
                report.fail("bank", f"theme {tid} has {per_theme[tid]} answers, need {min_theme}")
    return report


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--min-total", type=int, default=0)
    parser.add_argument("--min-theme", type=int, default=0)
    args = parser.parse_args(argv)

    entries, parse_errors = load_entries()
    report = check_bank(entries, parse_errors, args.min_total, args.min_theme)
    report.print()
    if report.problems:
        print(f"validate_clues: {len(report.problems)} problems in {len(entries)} answers")
        return 1
    print(f"validate_clues: ok ({len(entries)} answers)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
