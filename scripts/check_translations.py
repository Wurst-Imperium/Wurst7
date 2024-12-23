import util
from pathlib import Path

translations_dir = Path("src") / "main" / "resources" / "assets" / "wurst" / "translations"


def show_translation_stats(en_us: dict, translations: dict):
	"""Render a table of the current translation progress for each language."""
	util.add_github_summary("| Language | Translated | % |")
	util.add_github_summary("| --- | --- | --- |")
	util.add_github_summary(f"| en_us | {len(en_us)} | 100.00% |")
	for lang, data in translations.items():
		util.add_github_summary(f"| {lang} | {len(data)} | {len(data) / len(en_us) * 100:.2f}% |")
	util.add_github_summary("")


def check_extra_keys(en_us: dict, translations: dict):
	"""Check if any translation files contain keys that don't exist in the original."""
	extra_keys_found = False
	for lang, data in translations.items():
		extra_keys = set(data.keys()) - set(en_us.keys())
		if extra_keys:
			extra_keys_found = True
			util.add_github_summary(
				f"⚠ {lang}.json contains translations that don't exist in en_us.json ({len(extra_keys)} found):"
			)
			for key in extra_keys:
				util.add_github_summary(f"- {key}")
	if extra_keys_found:
		raise Exception("Found extra keys in one or more translation files, see summary")


def main():
	en_us = util.read_json_file(translations_dir / "en_us.json")
	translations = {}
	for path in sorted(translations_dir.rglob("*.json"), key=lambda x: x.name):
		if path.is_file() and path.name != "en_us.json":
			lang = path.name.removesuffix(".json")
			data = util.read_json_file(path)
			translations[lang] = data

	show_translation_stats(en_us, translations)
	check_extra_keys(en_us, translations)


if __name__ == "__main__":
	main()
