## Plan: Close issue #1103

TL;DR — Read issue #1103, reproduce the bug or spec, implement a focused fix in the repo, add or update tests/docs if needed, then open a PR that references and closes the issue. Use a branch named `fix/1103-<short>` and the GitHub CLI to create the PR.

### Steps
1. Review the issue at https://github.com/rikkahub/rikkahub/issues/1103 and copy reproduction steps.
2. Reproduce locally (use Android Studio / `./gradlew` / run web UI) and capture logs/screenshots.
3. Locate affected code with search (IDE or `git grep`) and open candidate files (e.g., files under `app/`, `web-ui/`).
4. Implement a minimal, well-scoped fix in the affected files (link changed files in commit).
5. Add or update unit / integration tests and update docs or changelog where applicable.
6. Run lint/build: `./gradlew test assembleDebug` or `npm run build` in `web-ui/` as applicable.
7. Commit changes, push branch, then open PR using the gh CLI with a body containing `Closes #1103`.
8. Add reviewers, CI labels, and link any screenshots or logs to the PR.

### Checklist
- [ ] Read full issue description and confirm acceptance criteria.
- [ ] Reproduce bug and save evidence (logs/screenshots).
- [ ] Identify all modified files and link them in PR.
- [ ] Add/update tests that cover the fix.
- [ ] Run local builds and linters successfully.
- [ ] Use descriptive commit and PR messages.
- [ ] PR body includes `Closes #1103`.
- [ ] Assign reviewer(s) and set target base branch (`master`).

### PR title template
Fix/resolve: short summary of fix (scope) — closes #1103

Example: Fix: correct image model selection behavior — closes #1103

### Commit message template
Short summary (type/scope): 50 chars or less

More detailed explanation (one paragraph). If applicable, mention the root cause and why the change is safe.

Refs: #1103

(Example)
fix(image): correct model selection when uploading images

Model selection previously fell back to default provider due to missing
`providerId` mapping. Add mapping and unit test to ensure correct provider.
Refs: #1103

### Suggested PR body (paste as-is and edit)
This PR fixes the problem described in issue #1103 by implementing a targeted change to the affected module and adding tests.

What I changed
- Brief bullet summary of code changes and files modified (e.g., `app/src/main/...`, `web-ui/app/...`)
- Tests added/updated: describe test scope
- Any UX/screenshots or logs attached

Why this fixes the issue
- Short reasoning; root cause and how fix addresses it

How to verify
1. Steps to reproduce original issue
2. Steps showing the fix works (expected results)

Closes #1103

### Git / gh commands (Windows; run in Git Bash or PowerShell)
1. Fetch upstream and base a new branch off upstream default:
- `git fetch upstream`
- `git checkout -b fix/1103-short-description upstream/master`

2. Work locally: edit files, add tests.

3. Stage and commit:
- `git add <changed-files>`
- `git commit -m "fix(scope): short summary

More detailed explanation (one paragraph).

Refs: #1103"`

4. Push branch to your fork (origin):
- `git push --set-upstream origin fix/1103-short-description`

5. Open PR with GitHub CLI (adjust base if not `master`):
- `gh pr create --base master --head fix/1103-short-description --title "Fix: short summary — closes #1103" --body "Paste the suggested PR body here (include 'Closes #1103')"`

Optional flags:
- `--reviewer user1,user2`
- `--label bug,fix`

### Further Considerations
1. Confirm base branch: upstream default is `master` — choose `master` / `main` as project requires.
2. If change affects translations, follow the README rule: avoid translation PRs unless required.
3. If the fix is large, split into smaller PRs: Option A) minimal fix + tests / Option B) follow-up refactor PR.

Summary of what I produced
- A concise, actionable PR plan to close issue #1103: checklist, step-by-step actions, PR title and commit message templates, suggested PR body with `Closes #1103`, and exact git + gh CLI commands to create branch, commit, push, and open the PR.

