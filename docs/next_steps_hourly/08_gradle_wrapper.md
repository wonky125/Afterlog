# Hour 8 - Restore Gradle wrapper

Goal:
Enable CLI/CI builds by restoring wrapper scripts and jar.

Steps:
- From `android/`, run:
  `gradle wrapper --gradle-version 8.11.1 --distribution-type bin`
- Verify these files exist:
  - `android/gradlew`
  - `android/gradlew.bat`
  - `android/gradle/wrapper/gradle-wrapper.jar`
- Add them to version control.

Push:
- Message suggestion: "restore Gradle wrapper scripts"

