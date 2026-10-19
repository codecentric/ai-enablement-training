---
name: copy-docs-to-project
description: Copy the Kiezmarkt docs (domain, backlog, ops, contracts) from this training repo into docs/project/ of your own Kiezmarkt project. Run once in the training repo, at the start of Block 2.
argument-hint: "[project-folder]"
disable-model-invocation: true
allowed-tools: Bash(python3 "${CLAUDE_SKILL_DIR}/copy_docs.py" *), Bash(git -C * rev-parse --is-inside-work-tree), Bash(git -C * add docs/project), Bash(git -C * commit -m "Import project docs" -- docs/project)
---

# Copy the docs to your project

Copy the Kiezmarkt docs from this training repo into `docs/project/` of your
own Kiezmarkt project: `domain.md`, `ops.md`, `backlog.md`, and `contracts/`
with `api.yaml`, `events.md`, `ui.md`, `data.md` and `acceptance.md`.

Run this in the training repo, the folder you cloned. Without a folder name,
the script looks for one `my-kiezmarkt*` folder inside the training repo or
next to it.

A script does the copy. It also removes text meant for trainers, so a copy by
hand is not the same. Never copy or edit these files yourself.

## Steps

1. Run the script:
   ```bash
   python3 "${CLAUDE_SKILL_DIR}/copy_docs.py" "$ARGUMENTS"
   ```
   On success, its first line names the project: `Project: <path>`.
2. Check the exit code.
   - `0`: go to step 3.
   - `2`: some files in `docs/project/` already exist and differ. Show the
     user the list and ask: overwrite them? Only on a clear yes, run the
     script again with the project path and `--force`.
   - `3`: no project folder found, or more than one. Show the user what the
     script printed and ask for the path of their project. Run the script
     again with that path.
   - Anything else: stop. Show the user the message and tell them to raise
     their hand. Do not copy the files by hand.
3. Check for git in the project:
   `git -C <project> rev-parse --is-inside-work-tree`. If it prints `true`,
   commit only the docs:
   ```bash
   git -C <project> add docs/project
   git -C <project> commit -m "Import project docs" -- docs/project
   ```
   Nothing to commit is fine. No git repository: skip this step and say so.
4. Tell the user in two lines: which project got the files, and the commit.

Change nothing else, in the project or in the training repo: not `CLAUDE.md`,
not `.claude/rules/`, not the code.
