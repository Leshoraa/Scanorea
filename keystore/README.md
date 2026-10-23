# Release keystore

**Do not commit the `.jks` file or `keystore.properties`.** Both are git-ignored.

## What is here

`scanorea-release.jks` — the signing key used for `assembleRelease`. The matching
passwords and alias live in `keystore.properties` at the repository root.

## Back this up

The signing key is irreplaceable. Without it you cannot publish an update that replaces an
already installed copy of Scanorea (Android refuses to update an app signed with a
different key). Back up **both** files somewhere safe:

- `keystore/scanorea-release.jks`
- `keystore.properties`

## Recreating on a new machine

1. Copy the two files above into the same paths in the cloned repository.
2. Run `./gradlew assembleRelease` — the build reads `keystore.properties` automatically.

If `keystore.properties` is missing, the release build falls back to the debug signing
key so the APK is still installable for side-loading. That fallback must **not** be used
for Play Store uploads.
