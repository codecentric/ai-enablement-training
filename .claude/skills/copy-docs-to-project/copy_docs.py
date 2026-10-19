#!/usr/bin/env python3
"""Copy the Kiezmarkt docs from the training repo into a participant's project,
without trainer notes.

Usage: copy_docs.py [project-dir] [--force]

Writes <project-dir>/docs/project/. Without project-dir, it uses the one
my-kiezmarkt* folder inside the training repo or next to it. Only the first
word counts as project-dir, anything after it is ignored. Every cut below
has to match its source exactly once. If a source changed, the script stops
before writing anything, so a trainer note never slips through unnoticed.

Exit codes: 0 done, 1 error, 2 files exist and differ (rerun with --force),
3 no project folder found, or more than one (pass project-dir).
"""

import re
import sys
from pathlib import Path

SOURCE = Path(__file__).resolve().parents[3]
PROJECT_GLOB = "my-kiezmarkt*"

# source path in the training repo -> path under docs/project/
FILES = {
    "domain.md": "domain.md",
    "ops.md": "ops.md",
    "docs/project/backlog.md": "backlog.md",
    "contracts/api.yaml": "contracts/api.yaml",
    "contracts/events.md": "contracts/events.md",
    "contracts/ui.md": "contracts/ui.md",
    "contracts/data.md": "contracts/data.md",
    "contracts/acceptance.md": "contracts/acceptance.md",
}

# ("section", heading) drops the heading and everything up to the next heading
# of the same or a higher level. ("text", old, new) replaces one exact passage.
CUTS = {
    "domain.md": [
        ("text",
         "These are not oversights. They are the material for discovery, planning and example\n"
         "mapping, and every one of them has bitten somebody in a block.\n",
         "These are not oversights.\n"),
    ],
    "docs/project/backlog.md": [
        ("text", "| **7** — risk classification | 8 tasks | 8 tasks | 8 tasks | 8 tasks |\n", ""),
        ("text",
         "The four block 2 tickets each carry a **Proposed approach** quoted from a colleague. It\n"
         "is plausible, confidently phrased, and either wrong or much smaller than the real\n"
         "problem. An agent will not object to it unaided. That is the point of the block.\n\n",
         ""),
        ("section", "### Block 7 — risk classification"),
    ],
    "contracts/events.md": [
        ("text",
         "Two questions are open. They are exercise material for the backend planning\n"
         "block and are recorded here as open, not as decided.\n",
         "Two questions are open. They are recorded here as open, not as decided.\n"),
    ],
    "contracts/ui.md": [
        ("text",
         "This is a hard requirement and it is where the frontend planning exercise goes wrong,\n"
         "so it is spelled out:\n",
         "This is a hard requirement, so it is spelled out:\n"),
        ("text",
         "constraint of the exercise, not an omission from this document.\n",
         "constraint, not an omission from this document.\n"),
    ],
    "contracts/data.md": [
        ("text",
         "the people who consume the number, and that conversation is the exercise.\n",
         "the people who consume the number.\n"),
        ("text",
         "Block 5.1 of the training audits exactly this list. The gap is the material; do not\n"
         "close it ahead of the session.\n\n",
         ""),
    ],
    "contracts/acceptance.md": [
        ("section", "## What is deliberately not in here"),
        ("text",
         "`domain.md` invariant 3. It is written down, nothing enforces it, and it will be broken\n"
         "within the first hour of anybody generating code against these contracts. That is not a\n"
         "prediction, it is the reason block 5.1 exists.\n",
         "`domain.md` invariant 3.\n"),
    ],
}

# Words that must not survive in any copied file, whatever the sources say later.
TELLS = ("Trainer", "trainer", "planted")

HEADING = re.compile(r"^(#{1,6}) ")


def cut_section(text, heading):
    lines = text.splitlines(keepends=True)
    hits = [i for i, line in enumerate(lines) if line.rstrip("\n") == heading]
    if len(hits) != 1:
        return None
    start, level = hits[0], len(HEADING.match(heading).group(1))
    end, fenced = len(lines), False
    for i in range(start + 1, len(lines)):
        if lines[i].startswith("```"):
            fenced = not fenced
        match = None if fenced else HEADING.match(lines[i])
        if match and len(match.group(1)) <= level:
            end = i
            break
    return "".join(lines[:start] + lines[end:])


def clean(src, text):
    cuts = CUTS.get(src, [])
    for cut in cuts:
        if cut[0] == "section":
            new = cut_section(text, cut[1])
        else:
            new = text.replace(cut[1], cut[2]) if text.count(cut[1]) == 1 else None
        if new is None:
            raise SystemExit(
                f"{src}: passage not found exactly once, the source changed.\n"
                f"  {cut[1].splitlines()[0]!r}\n"
                f"Update CUTS in {__file__}. Nothing was written."
            )
        text = new
    if cuts:
        text = text.rstrip("\n") + "\n"
    left = [tell for tell in TELLS if tell in text]
    if left:
        raise SystemExit(f"{src}: still contains {left} after the cuts. Nothing was written.")
    return text


def find_projects():
    found = {path.resolve() for base in (SOURCE, SOURCE.parent) for path in base.glob(PROJECT_GLOB)}
    return sorted(path for path in found if path.is_dir())


def main(argv):
    # The skill passes everything typed after the command as one string, so
    # split it and keep only the first word as the folder.
    words = " ".join(argv).split()
    force = "--force" in words
    args = [word for word in words if word != "--force"]
    if args:
        target = Path(args[0]).expanduser().resolve()
    else:
        found = find_projects()
        if len(found) != 1:
            if found:
                print("More than one project folder found:")
                for path in found:
                    print(f"  {path}")
            else:
                print(f"No {PROJECT_GLOB} folder found in or next to {SOURCE}.")
            print("Pass the path of your project folder.")
            return 3
        target = found[0]
    if not target.is_dir():
        raise SystemExit(f"{target} is not a folder.")
    if target == SOURCE or SOURCE.is_relative_to(target):
        raise SystemExit(
            f"{target} is the training repo or contains it. Pass your own project folder."
        )
    print(f"Project: {target}")
    missing = [src for src in FILES if not (SOURCE / src).is_file()]
    if missing:
        raise SystemExit(f"Missing in {SOURCE}: {', '.join(missing)}. Nothing was written.")

    out = {
        target / "docs/project" / dst: clean(src, (SOURCE / src).read_text(encoding="utf-8"))
        for src, dst in FILES.items()
    }
    current = {path: path.read_text(encoding="utf-8") if path.is_file() else None for path in out}
    differ = [path for path, text in out.items() if current[path] not in (None, text)]
    if differ and not force:
        print("These files exist and differ from the training repo. Nothing was written:")
        for path in differ:
            print(f"  {path.relative_to(target)}")
        print("Run again with --force to overwrite them.")
        return 2

    for path, text in out.items():
        if current[path] == text:
            print(f"unchanged  {path.relative_to(target)}")
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        print(f"written    {path.relative_to(target)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
