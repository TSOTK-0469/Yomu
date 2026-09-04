---
status: accepted
---

# Persist the library index in Room

Yomu will persist mount sources, album summaries, bookshelf membership, hidden state, cover references, and reading positions in Room, while keeping only small UI preferences outside the database. Cold start reads this index without scanning external storage. Room was chosen over SharedPreferences or a JSON snapshot because the library now has relational invariants, cascading lifecycle rules, and schema migrations that flat key-value storage would make fragile. The first Room-based release intentionally starts with an empty library and releases legacy directory grants, but every later schema change must use an explicit non-destructive migration. ADR-0002 extends the index with ordered image rows and moves source reconciliation to mounting and explicit refresh.
