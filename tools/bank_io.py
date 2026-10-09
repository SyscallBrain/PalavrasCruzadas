"""Shared reader for the clue bank (content/clues/pt-PT/*.jsonl).

Used by validate_clues.py, compile_bank.py and clue_report.py. Writes nothing.
"""
import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "content"
CLUES_DIR = CONTENT / "clues" / "pt-PT"
THEMES_FILE = CONTENT / "themes.json"
EXCLUSIONS_FILE = CONTENT / "exclusions.txt"
SPELLING_ALLOW_FILE = CONTENT / "spelling_allow.txt"
COMPILED_FILE = ROOT / "app" / "src" / "main" / "assets" / "clues" / "pt-PT.json"

# Game alphabet: A–Z plus the Portuguese accented letters.
ALPHABET = frozenset("ABCDEFGHIJKLMNOPQRSTUVWXYZÁÀÂÃÉÊÍÓÔÕÚÇ")

# Brazilian Portuguese words. Compared without accents, in upper case.
BR_BLACKLIST = frozenset({
    "ONIBUS", "TREM", "TRENS", "CELULAR", "TELA", "SUCO", "TIME", "GOL", "MOCA",
    "GELADEIRA", "JACARE", "BANHEIRO", "XICARA", "CAFEZINHO", "CONTATO",
})

# Spellings from before the 1990 Orthographic Agreement (AO1990).
AO_BLACKLIST = frozenset({
    "ACCAO", "ACCOES", "ACTO", "ACTOR", "ACTORES", "OPTIMO", "DIRECTOR",
    "PROJECTO", "OBJECTO", "SECTOR", "RECEPCAO", "ESPECTACULO",
})


def nfc_upper(s: str) -> str:
    """NFC, upper case and trimmed: the form stored in the bank."""
    return unicodedata.normalize("NFC", s).strip().upper()


def plain(s: str) -> str:
    """Only A–Z, without accents, in upper case. For comparisons and blacklists."""
    decomposed = unicodedata.normalize("NFD", nfc_upper(s))
    return "".join(c for c in decomposed if unicodedata.category(c) != "Mn")


def tokens(text: str) -> list[str]:
    """Words of a text (A–Z without accents), in upper case."""
    return re.findall(r"[A-Z]+", plain(text))


def accented_words(text: str) -> list[str]:
    """Words of a text with their accents, in lower case (for hunspell)."""
    return [w.lower() for w in re.findall(r"[A-ZÁÀÂÃÉÊÍÓÔÕÚÇ]+", nfc_upper(text)) if len(w) > 1]


def clue_words(text: str) -> list[str]:
    """Like accented_words, but skips proper nouns: capitalised words after the start of the clue."""
    words = []
    for index, match in enumerate(re.finditer(r"[A-Za-zÁÀÂÃÉÊÍÓÔÕÚÇáàâãéêíóôõúç]+", unicodedata.normalize("NFC", text))):
        word = match.group()
        if index > 0 and word[0].isupper():
            continue
        if len(word) > 1:
            words.append(word.lower())
    return words


def load_themes() -> dict[str, dict]:
    themes = json.loads(THEMES_FILE.read_text(encoding="utf-8"))
    return {t["id"]: t for t in themes}


def load_exclusions() -> set[str]:
    return _load_word_list(EXCLUSIONS_FILE)


def load_spelling_allow() -> set[str]:
    return _load_word_list(SPELLING_ALLOW_FILE)


def _load_word_list(path: Path) -> set[str]:
    words = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            words.add(plain(line))
    return words


def load_entries(directory: Path = CLUES_DIR):
    """Returns (entries, parse_errors).

    Each entry is a dict with the line's JSON plus 'file' and 'line' (for the reports).
    A line that is not valid JSON goes to parse_errors and is left out of the entries.
    """
    entries: list[dict] = []
    parse_errors: list[str] = []
    for path in sorted(directory.glob("*.jsonl")):
        rel = path.relative_to(ROOT).as_posix()
        for number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            if not raw.strip():
                continue
            try:
                obj = json.loads(raw)
            except json.JSONDecodeError as exc:
                parse_errors.append(f"{rel}:{number}: invalid JSON ({exc.msg})")
                continue
            if not isinstance(obj, dict):
                parse_errors.append(f"{rel}:{number}: line is not a JSON object")
                continue
            obj["file"] = rel
            obj["line"] = number
            entries.append(obj)
    return entries, parse_errors
