# 2. All files access for local browsing

Status: accepted

The app browses shared storage like a real file manager, so it uses `MANAGE_EXTERNAL_STORAGE` rather than the Storage Access Framework picker. Other apps private folders (`Android/data` and similar) are out of scope. Distribution is sideloaded for now; Google Play would require a permission declaration and review, which is a later decision.
