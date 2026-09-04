# Domain Docs

Before exploring the codebase, read the root `CONTEXT.md` and relevant ADRs under `docs/adr/` when they exist. Missing files should not block work.

## Layout

This is a single-context repository:

```text
/
├── CONTEXT.md
├── docs/adr/
└── app/
```

Use terminology defined in `CONTEXT.md`. If proposed work contradicts an existing ADR, surface the conflict explicitly rather than silently overriding it.
