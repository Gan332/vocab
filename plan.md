# Implementation Plan: Refactor CI Build Workflow (build.yml)

## Goal
Fix the persistent CI build failure by refactoring `.github/workflows/build.yml` into a clean, debuggable workflow that Gradle can actually run. The build step currently fails in **0 seconds** (Gradle never executes), while the old workflow ran Gradle for ~50 seconds.

## Root Cause (confirmed by CI timing analysis)

**Primary**: The build step lacks explicit `ANDROID_HOME`/`ANDROID_SDK_ROOT` that the last successful workflow had. The step-level `env:` section (with 5+ variables including secrets) may interfere with how the runner inherits environment variables set by `setup-android@v4` via `$GITHUB_ENV`. When Gradle's Android plugin can't find the SDK, it fails at configuration time before any task executes — producing the 0-second failure pattern.

**Secondary**: Using `${{ env.BUILD_TYPE }}` (template expression) in the `run:` script is risky because step-level env vars may not be accessible at workflow parse time. The backup `build-apk.yml` correctly uses `$BUILD_TYPE` (shell variable) instead.

## Tasks

### 1. Create diagnostics step (pre-build)
   - File: `.github/workflows/build.yml`
   - **What**: Add a new step before "Build APK" that prints env vars, Java version, SDK location, and Gradle wrapper info.
   - **Why**: Future CI failures will be diagnosable from step output without needing log artifacts.
   - **Acceptance**: Step output in CI job log shows ANDROID_HOME, ANDROID_SDK_ROOT, JAVA_HOME, SDK contents, and Gradle wrapper version.

### 2. Create build-type determination step
   - File: `.github/workflows/build.yml`
   - **What**: Separate step that determines the Gradle task name based on `inputs.build_type` and secrets availability. Outputs via `id: build-type` → `steps.build-type.outputs.gradle_task`.
   - **Details**:
     - Uses `$BUILD_TYPE` (shell var from step-level `env:`) — safe, proven pattern from `backup/build-apk.yml`
     - For `workflow_dispatch release` with `KEYSTORE_BASE64`: prepares keystore, outputs `assembleRelease`
     - For `workflow_dispatch release` without `KEYSTORE_BASE64`: falls back to `assembleDebug` with warning
     - All other cases: outputs `assembleDebug`
   - **Acceptance**: Correct task name output for push, PR, and manual dispatch scenarios.

### 3. Refactor "Build APK" step — THE KEY FIX
   - File: `.github/workflows/build.yml`
   - **What**: Rewrite the build step with critical fixes:
     1. Use `working-directory: ./VocabApp` (matches old successful workflow pattern)
     2. **Add explicit `ANDROID_HOME: /usr/local/lib/android/sdk` and `ANDROID_SDK_ROOT: /usr/local/lib/android/sdk`** to step `env:` (matches last successful run at commit 77553fa)
     3. Remove all template expressions from `run:` script body
     4. Use `${{ steps.build-type.outputs.gradle_task }}` only at the assignment line (this IS safe because step outputs are available at parse time)
     5. Log file at `build-output.log` (relative to working directory)
     6. `set -xeuo pipefail` for maximum debuggability
   - **Why**: This directly addresses both root causes (missing ANDROID_HOME + template expression risk).
   - **Acceptance**: Build step runs Gradle (duration > 0 seconds) and either succeeds or produces real Gradle errors in log.

### 4. Fix "Upload build log" artifact path
   - File: `.github/workflows/build.yml`
   - **What**: Change `path` from `build-output.log` to `VocabApp/build-output.log` to match new working-directory-based log location.
   - **Acceptance**: Build log artifact visible in CI run artifacts on failure.

### 5. Update "Upload APK" artifact name
   - File: `.github/workflows/build.yml`
   - **What**: Use `${{ steps.build-type.outputs.gradle_task }}` in artifact name for accuracy.
   - **Acceptance**: APK artifact name correctly reflects build type.

### 6. Diagnostics in "Install required SDK components" step
   - File: `.github/workflows/build.yml`
   - **What**: Add `echo "ANDROID_HOME=$ANDROID_HOME"` at start of this step (it already uses `$ANDROID_HOME` successfully).
   - **Acceptance**: Confirms ANDROID_HOME is set correctly for this step.

## Files to Modify
- `.github/workflows/build.yml` — Full rewrite of build job steps (keep structure, fix step implementations)

## New Files
- None

## Dependencies
- Tasks 1-2 (diagnostics + build-type) must precede Task 3 (build step) in workflow order
- Tasks 4-5 (uploads) depend on Task 3 working correctly for verification

## Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| ANDROID_HOME path `/usr/local/lib/android/sdk` is wrong for newer ubuntu runners | Build fails with SDK not found | Task 1 (diagnostics) prints actual SDK path; can adjust |
| `setup-android@v4` conflicts with explicit ANDROID_HOME override | env var conflict, build fails | Remove explicit override if diagnostics show @v4 sets it correctly |
| Gradle fails with REAL build error after fix (old workflow also failed at 50s) | CI still red but with actionable errors | Build log artifact will contain the actual error for diagnosis |
| Log path mismatch between tee output and upload | artifact missing | Target `VocabApp/build-output.log` matches working-directory |

## Verification Plan
1. Push refactored workflow to `main`
2. Check CI run: diagnostics step shows correct env vars and SDK path
3. Verify Build APK step duration > 0 seconds (confirms Gradle starts)
4. If Gradle fails: download `build-log-*` artifact from CI run page
5. Inspect build log to find actual Gradle error
6. Fix real error in follow-up PR

## Detailed Workflow YAML (final structure)

```yaml
name: Build VocabApp APK

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  workflow_dispatch:
    inputs:
      build_type:
        description: 'Build type'
        required: true
        default: 'debug'
        type: choice
        options:
          - debug
          - release

env:
  JAVA_VERSION: '17'
  JAVA_DISTRIBUTION: 'temurin'
  SDK_API_LEVEL: '34'
  SDK_BUILD_TOOLS: '34.0.0'

jobs:
  build:
    name: Build ${{ inputs.build_type || 'debug' }} APK
    runs-on: ubuntu-latest

    steps:
      # ===== 检出代码 =====
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 1

      # ===== 环境准备 =====
      - name: Set up JDK ${{ env.JAVA_VERSION }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Android SDK
        uses: android-actions/setup-android@v4

      - name: Install required SDK components
        run: |
          set -x
          echo "ANDROID_HOME=$ANDROID_HOME"
          SDKMANAGER=""
          for p in "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
                    "$ANDROID_HOME/tools/bin/sdkmanager"; do
            if [ -x "$p" ]; then SDKMANAGER="$p"; break; fi
          done
          if [ -z "$SDKMANAGER" ]; then
            echo "::warning::sdkmanager not found, relying on pre-installed SDK"
          else
            echo "Using: $SDKMANAGER"
            yes | "$SDKMANAGER" --install \
              "platforms;android-${{ env.SDK_API_LEVEL }}" \
              "build-tools;${{ env.SDK_BUILD_TOOLS }}" \
              2>&1 | tail -5
          fi

      # ===== 权限 =====
      - name: Grant execute permission for gradlew
        run: chmod +x VocabApp/gradlew

      # ===== 缓存 Gradle 依赖 =====
      - name: Cache Gradle dependencies
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('VocabApp/**/*.gradle*', 'VocabApp/**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      # ===== 版本号 =====
      - name: Generate version info
        id: version
        run: |
          echo "version_code=$(date +%Y%m%d%H%M)" >> $GITHUB_OUTPUT
          echo "version_name=1.0.${{ github.run_number }}" >> $GITHUB_OUTPUT

      # ===== 诊断信息 =====
      - name: Diagnostics (pre-build)
        run: |
          set -x
          echo "=== Java ==="
          java -version 2>&1
          echo "=== SDK ==="
          echo "ANDROID_HOME=$ANDROID_HOME"
          echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
          ls -ld "$ANDROID_HOME/" 2>&1 || echo "ANDROID_HOME not accessible"
          ls "$ANDROID_HOME/platforms/" 2>&1 || echo "No platforms dir"
          ls "$ANDROID_HOME/build-tools/" 2>&1 || echo "No build-tools dir"
          echo "=== Gradle wrapper ==="
          ls -la VocabApp/gradlew VocabApp/gradle/wrapper/
          head -5 VocabApp/gradle/wrapper/gradle-wrapper.properties

      # ===== 确定构建类型 =====
      - name: Determine build type
        id: build-type
        env:
          BUILD_TYPE: ${{ inputs.build_type || 'debug' }}
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
        run: |
          set -xeuo pipefail
          cd VocabApp
          if [ "$BUILD_TYPE" = "release" ] && [ -n "$KEYSTORE_BASE64" ]; then
            echo "$KEYSTORE_BASE64" | base64 -d > app/keystore.jks
            cat > app/keystore.properties <<-EOF
            storeFile=keystore.jks
            storePassword=$STORE_PASSWORD
            keyAlias=$KEY_ALIAS
            keyPassword=$KEY_PASSWORD
            EOF
            echo "gradle_task=assembleRelease" >> "$GITHUB_OUTPUT"
          else
            if [ "$BUILD_TYPE" = "release" ]; then
              echo "::warning::Release build selected but KEYSTORE_BASE64 not set, falling back to debug"
            fi
            echo "gradle_task=assembleDebug" >> "$GITHUB_OUTPUT"
          fi

      # ===== 构建 APK（修复核心） =====
      - name: Build APK
        working-directory: ./VocabApp
        env:
          ANDROID_HOME: /usr/local/lib/android/sdk
          ANDROID_SDK_ROOT: /usr/local/lib/android/sdk
        run: |
          set -xeuo pipefail
          GRADLE_TASK="${{ steps.build-type.outputs.gradle_task }}"
          echo "Starting: ./gradlew $GRADLE_TASK"
          ./gradlew "$GRADLE_TASK" --no-daemon --stacktrace 2>&1 | tee build-output.log
          exit ${PIPESTATUS[0]}

      # ===== 上传构建产物 =====
      - name: Upload APK
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: VocabApp-${{ steps.build-type.outputs.gradle_task }}
          path: VocabApp/app/build/outputs/apk/**/*.apk
          if-no-files-found: error
          retention-days: 30

      # ===== 上传构建日志（失败时辅助排查） =====
      - name: Upload build log
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: build-log-${{ steps.build-type.outputs.gradle_task }}
          path: VocabApp/build-output.log
          if-no-files-found: warn
          retention-days: 7
```

## Implementation Order
1. Edit `.github/workflows/build.yml` with the complete YAML above
2. Commit: `git add .github/workflows/build.yml && git commit -m "fix: refactor CI build workflow - add ANDROID_HOME, diagnostics, extract build-type step"`
3. Push: `git push origin main`
4. Monitor CI run on GitHub
5. Check diagnostics step output to confirm env vars
6. Check build step duration (>0s = Gradle started)
7. If failed: download `build-log-*` artifact and inspect
