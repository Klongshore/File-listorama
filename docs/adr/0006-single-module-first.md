# 6. Single module first

Status: accepted

The project starts as one Gradle module with clear package boundaries (domain, data/local, ui). It splits into per-backend modules when the first cloud backend arrives, when the boundary is proven rather than guessed.
