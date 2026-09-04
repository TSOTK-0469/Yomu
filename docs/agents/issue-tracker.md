# Issue tracker: Local Markdown

Issues and specs for this repo live as Markdown files in `.scratch/`.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`
- The spec is `.scratch/<feature-slug>/spec.md`
- Implementation issues are stored individually at `.scratch/<feature-slug>/issues/<NN>-<slug>.md`
- Triage state is recorded with a `Status:` line
- Comments are appended under a `## Comments` heading

## Publishing and fetching

When publishing an issue, create its file under the corresponding feature directory. When fetching an issue, read the referenced path or issue number.

## Wayfinding

- Map: `.scratch/<effort>/map.md`
- Ticket: `.scratch/<effort>/issues/NN-<slug>.md`
- Tickets use `Type:`, `Status:`, and optional `Blocked by:` fields
- Claim a ticket by setting `Status: claimed`
- Resolve it by adding `## Answer`, setting `Status: resolved`, and updating the map
