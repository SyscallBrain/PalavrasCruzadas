#!/usr/bin/env python3
"""Bank statistics: theme × length, letters per position and accents.

Shows where words are missing. Never fails because of the content.
"""
from collections import Counter

from bank_io import load_entries, load_themes, plain

LENGTH_TARGET = {3: 450, 4: 750, 5: 900, 6: 800, 7: 650, 8: 450, 9: 300}
LENGTH_TARGET_LONG = 200  # 10–13 letters
THEME_TARGET = 180


def length_bucket(n: int) -> str:
    return str(n) if n <= 9 else "10-13"


def main() -> int:
    entries, errors = load_entries()
    themes = load_themes()
    total = len(entries)
    print(f"Answers: {total}   (parse errors: {len(errors)})")
    if not total:
        return 0

    by_length = Counter(length_bucket(len(e["answer"])) for e in entries)
    print("\nLength (actual / target):")
    for n in range(3, 10):
        print(f"  {n:>5}: {by_length.get(str(n), 0):>5} / {LENGTH_TARGET[n]}")
    print(f"  10-13: {by_length.get('10-13', 0):>5} / {LENGTH_TARGET_LONG}")

    short = [e["answer"] for e in entries if 3 <= len(e["answer"]) <= 5]
    accented_short = sum(1 for w in short if plain(w) != w)
    if short:
        print(f"\n3–5 letter answers without accents: {100 * (len(short) - accented_short) / len(short):.0f} % (target 75 %)")

    print("\nTheme × length (3-5 / 6-8 / 9-13), and total:")
    grid: dict[str, Counter] = {tid: Counter() for tid in themes}
    for e in entries:
        size = len(e["answer"])
        band = "3-5" if size <= 5 else "6-8" if size <= 8 else "9-13"
        for tid in e["themes"]:
            grid.setdefault(tid, Counter())[band] += 1
    for tid in themes:
        c = grid.get(tid, Counter())
        total_t = sum(c.values())
        flag = "" if tid == "geral" or total_t >= THEME_TARGET else f"   {THEME_TARGET - total_t} missing"
        print(f"  {tid:<12} {c['3-5']:>5} {c['6-8']:>5} {c['9-13']:>5}  = {total_t:>5}{flag}")

    print("\nLetters per position (top 5 per position, first 6 positions):")
    max_len = min(6, max(len(e["answer"]) for e in entries))
    for pos in range(max_len):
        freq = Counter(e["answer"][pos] for e in entries if len(e["answer"]) > pos)
        top = ", ".join(f"{ch} {n}" for ch, n in freq.most_common(5))
        print(f"  {pos + 1}: {top}")

    accented_letters = Counter(ch for e in entries for ch in e["answer"] if plain(ch) != ch)
    print("\nAccented letters in the answers:", ", ".join(f"{ch} {n}" for ch, n in accented_letters.most_common()) or "none")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
