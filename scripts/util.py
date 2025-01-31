import json
import os
from pathlib import Path


def read_json_file(path: Path) -> dict:
	"""Read a JSON data file."""
	return json.loads(path.read_text(encoding="utf-8"))


def add_github_summary(summary: str):
	"""Add a line to the GitHub Actions summary for the current step."""
	if "GITHUB_STEP_SUMMARY" not in os.environ:
		print(summary)
		return
	with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as summary_file:
		print(summary, file=summary_file)
