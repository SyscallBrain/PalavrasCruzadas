"""Tests for the bank tools. Run: python3 -m unittest discover -s tools/tests"""
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))

from bank_io import plain, tokens  # noqa: E402
from compile_bank import compile_entries  # noqa: E402
from validate_clues import check_bank  # noqa: E402


def entry(answer="MOLDE", clues=None, themes=("comida",), lemma=None, file=None, line=1):
    if clues is None:
        clues = [
            {"t": "Forma onde se verte a massa do bolo", "d": 1, "s": "Forma"},
            {"t": "Padrão que serve de modelo", "d": 2},
        ]
    first = themes[0]
    return {
        "answer": answer,
        "clues": clues,
        "themes": list(themes),
        "lemma": lemma or answer,
        "file": file or f"content/clues/pt-PT/{first}.jsonl",
        "line": line,
    }


def problems(entries, min_total=0, min_theme=0, parse_errors=()):
    return check_bank(entries, list(parse_errors), min_total, min_theme).problems


class BankIoTest(unittest.TestCase):
    def test_plain_removes_accents_and_uppercases(self):
        self.assertEqual(plain("ação"), "ACAO")
        self.assertEqual(plain("ÇÃO"), "CAO")

    def test_tokens_split_words_without_accents(self):
        self.assertEqual(tokens("Forma de ação, pequeno-almoço"), ["FORMA", "DE", "ACAO", "PEQUENO", "ALMOCO"])


class ValidateClueTest(unittest.TestCase):
    def test_valid_entry_passes(self):
        self.assertEqual(problems([entry()]), [])

    def test_answer_must_be_nfc_uppercase(self):
        self.assertTrue(any("NFC uppercase" in p for p in problems([entry(answer="molde")])))

    def test_answer_length_bounds(self):
        self.assertTrue(any("letters" in p for p in problems([entry(answer="AB")])))
        self.assertTrue(any("letters" in p for p in problems([entry(answer="A" * 14)])))

    def test_answer_outside_alphabet(self):
        self.assertTrue(any("outside the alphabet" in p for p in problems([entry(answer="GUARDA-ROUPA")])))

    def test_pre_ao_spelling_is_blacklisted(self):
        result = problems([entry(answer="ACCAO", lemma="ACCAO", clues=[
            {"t": "Ato de fazer algo", "d": 1}, {"t": "Movimento de um corpo", "d": 2}])])
        self.assertTrue(any("blacklisted spelling" in p for p in result))

    def test_clue_must_not_reveal_answer_root(self):
        result = problems([entry(clues=[
            {"t": "Molde de forma redonda", "d": 1}, {"t": "Padrão que serve de modelo", "d": 2}])])
        self.assertTrue(any("reveals the answer root" in p for p in result))

    def test_clue_must_not_end_with_full_stop(self):
        result = problems([entry(clues=[
            {"t": "Forma onde se verte a massa.", "d": 1}, {"t": "Padrão que serve de modelo", "d": 2}])])
        self.assertTrue(any("full stop" in p for p in result))

    def test_clue_must_start_with_capital(self):
        result = problems([entry(clues=[
            {"t": "forma onde se verte", "d": 1}, {"t": "Padrão que serve de modelo", "d": 2}])])
        self.assertTrue(any("capital" in p for p in result))

    def test_difficulty_must_be_one_to_three(self):
        result = problems([entry(clues=[
            {"t": "Forma onde se verte", "d": 4}, {"t": "Padrão que serve de modelo", "d": 2}])])
        self.assertTrue(any("difficulty" in p for p in result))

    def test_short_clue_limit(self):
        result = problems([entry(clues=[
            {"t": "Forma onde se verte", "d": 1, "s": "Uma pista curta demais"},
            {"t": "Padrão que serve de modelo", "d": 2}])])
        self.assertTrue(any("short clue" in p for p in result))

    def test_clue_length_limit(self):
        long_text = "Coisa " * 12
        result = problems([entry(clues=[
            {"t": long_text.strip(), "d": 1}, {"t": "Padrão que serve de modelo", "d": 2}])])
        self.assertTrue(any("longer than 60" in p for p in result))

    def test_clue_count_bounds(self):
        one = [{"t": "Forma onde se verte", "d": 1}]
        five = [{"t": f"Pista número {i} bem diferente", "d": 1} for i in range(5)]
        self.assertTrue(any("need 2-4 clues" in p for p in problems([entry(clues=one)])))
        self.assertTrue(any("need 2-4 clues" in p for p in problems([entry(clues=five)])))

    def test_near_identical_clues_rejected(self):
        result = problems([entry(clues=[
            {"t": "Forma onde se verte a massa", "d": 1},
            {"t": "Forma onde se verte a massas", "d": 2}])])
        self.assertTrue(any("almost identical" in p for p in result))

    def test_unknown_theme(self):
        self.assertTrue(any("unknown theme" in p for p in problems([entry(themes=("inventado",), file="content/clues/pt-PT/inventado.jsonl")])))

    def test_entry_must_live_in_first_theme_file(self):
        result = problems([entry(themes=("comida", "casa"), file="content/clues/pt-PT/casa.jsonl")])
        self.assertTrue(any("first theme" in p for p in result))

    def test_parse_errors_are_reported(self):
        result = problems([], parse_errors=["content/clues/pt-PT/comida.jsonl:3: invalid JSON (Expecting value)"])
        self.assertEqual(len(result), 1)


class BankRulesTest(unittest.TestCase):
    def test_duplicate_answer(self):
        result = problems([entry(answer="MAR", clues=[{"t": "Água salgada do oceano", "d": 1}, {"t": "Extensão de água salgada", "d": 2}], themes=("mar",)),
                           entry(answer="MAR", clues=[{"t": "Lugar onde o rio desagua", "d": 1}, {"t": "Água salgada muito grande", "d": 2}], themes=("mar",))])
        self.assertTrue(any("duplicate answer" in p for p in result))

    def test_same_clue_for_two_answers(self):
        shared = [{"t": "Forma onde se verte a massa", "d": 1}, {"t": "Utensílio da cozinha", "d": 2}]
        result = problems([entry(answer="MOLDE", clues=shared),
                           entry(answer="FORMA", clues=shared, themes=("casa",))])
        self.assertTrue(any("same clue" in p for p in result))

    def test_total_minimum(self):
        self.assertTrue(any("need at least 1500" in p for p in problems([entry()], min_total=1500)))

    def test_thirty_percent_short_clue_rule(self):
        no_short = [{"t": "Forma onde se verte a massa", "d": 1}, {"t": "Padrão que serve de modelo", "d": 2}]
        result = problems([entry(clues=no_short)])
        self.assertTrue(any("short clue" in p and "30%" in p for p in result))

    def test_plain_short_answers_rule(self):
        pairs = {
            "CÃO": ("Melhor amigo do homem", "Companheiro de quatro patas"),
            "PÉS": ("Extremidades do corpo", "Parte que pisa o chão"),
            "LÁPIS": ("Instrumento de escrita", "Escreve sobre papel"),
            "SÍTIO": ("Local ou lugar", "Povoação pequena"),
        }
        entries = [entry(answer=a, clues=[{"t": t1, "d": 1, "s": "Pista"}, {"t": t2, "d": 2}], themes=("geral",))
                   for a, (t1, t2) in pairs.items()]
        result = problems(entries)
        self.assertTrue(any("without accents" in p for p in result))


class CompileTest(unittest.TestCase):
    def test_compile_is_sorted_and_compact(self):
        bank = compile_entries([entry(answer="PÉ", clues=[{"t": "Parte do corpo", "d": 1}, {"t": "Extremidade da perna", "d": 2}], themes=("corpo",)),
                                entry()])
        self.assertEqual(bank["v"], 1)
        self.assertEqual([row[0] for row in bank["entries"]], ["MOLDE", "PÉ"])
        self.assertEqual(bank["entries"][0][3][0], ["Forma onde se verte a massa do bolo", 1, "Forma"])
        self.assertIsNone(bank["entries"][1][3][0][2])


if __name__ == "__main__":
    unittest.main()
