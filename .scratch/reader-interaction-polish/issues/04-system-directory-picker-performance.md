# Improve system directory picker performance

Type: enhancement
Status: wontfix

Android's system directory picker can load slowly when a directory contains many entries.

## Comments

- The delay occurs before Yomu receives a selected directory and is controlled by Android's DocumentsUI and storage provider.
- A custom direct-path browser would require a broader storage-access design and sensitive permissions.
- Decision: keep the current Storage Access Framework flow and defer this optimization.

## Answer

No code change. Yomu retains user-scoped directory authorization through the Android system picker.
