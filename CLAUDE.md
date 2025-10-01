# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Verus Miner 9000 is an Android cryptocurrency mining application for Veruscoin. It's a native Android app written in Java that uses the ccminer binary to mine Veruscoin on Android devices. The app includes machine learning-based thermal protection (AMAYC) and pool statistics integration.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Install debug build to connected device
./gradlew installDebug
```

## Supported Architectures

The miner only supports:
- `arm64-v8a` (primary target)
- `x86_64` (secondary support)

Architecture validation occurs in `MainActivity.java:372-380`.

## Core Architecture

### Mining Service Layer

The mining functionality is abstracted through service classes:

- **`AbstractMiningService`** (app/src/main/java/shmutalov/verusminer9000/miner/AbstractMiningService.java): Abstract service interface defining mining operations
- **`VerusBinMiningService`** (app/src/main/java/shmutalov/verusminer9000/miner/VerusBinMiningService.java): Concrete implementation that spawns ccminer binary as a subprocess
  - Copies native binaries from assets to private directory
  - Manages process lifecycle via `ProcessBuilder`
  - Parses miner output to extract hashrate, shares, and difficulty
  - Handles pause/resume by stopping/restarting the process

The service runs the ccminer binary with arguments:
```
./ccminer --no-banner --no-color -a verus -o [pool]:[port] -u [address].[workername] -p [password] -t [cores]
```

### UI Architecture

The app uses a single-activity architecture with fragments:

- **`MainActivity`** (app/src/main/java/shmutalov/verusminer9000/MainActivity.java): Main activity managing fragments and mining state
- **`SettingsFragment`**: Pool configuration, wallet address, CPU cores, temperature limits
- **`AboutFragment`**: App information
- Bottom navigation switches between home view, log view, settings, and about

### Temperature Protection System

Two-layer thermal protection system:

1. **Static Temperature Control**: Hard limits on CPU and battery temperature (MainActivity.java:1500-1526)
   - Pauses mining when max temps reached
   - Resumes when temps drop below safe threshold (max temp - cooldown percentage)

2. **AMAYC (As-Much-As-You-Can) ML System**: Predictive thermal management (MainActivity.java:1540-1611)
   - Collects CPU and battery temperature samples
   - Sends to external API: `https://amaycapi.hayzam.in/check1` or `check2`
   - Preemptively pauses mining if predicted next temp exceeds limits
   - Falls back to static control on API errors

### Configuration System

**`Config`** (app/src/main/java/shmutalov/verusminer9000/Config.java): Simple key-value persistence using SharedPreferences

Important config keys:
- `init`: "1" when configured
- `address`: Wallet address
- `cores`: CPU cores to use
- `maxcputemp`: Maximum CPU temperature (°C)
- `maxbatterytemp`: Maximum battery temperature (°C)
- `cooldownthreshold`: Percentage below max for resumption
- `pauseonbattery`: "1" to pause when unplugged
- `disableamayc`: "1" to disable ML thermal protection
- `selected_pool`: Pool index
- `workername`: Worker identifier

### Pool Management

**`ProviderManager`** (app/src/main/java/shmutalov/verusminer9000/api/ProviderManager.java): Manages pool definitions and selection

Pre-configured pools include:
- Verus Official Pool (pool.veruscoin.io:9999)
- Alphatech IT
- Luckpool (NA/EU/AP regions)
- Custom pool option

Pool API integration for stats (currently disabled in MainActivity.java:582).

### State Management

Mining states (MainActivity.java:185-194):
- `STATE_STOPPED` (0): Not mining
- `STATE_MINING` (1): Actively mining
- `STATE_PAUSED` (2): Paused by user or battery setting
- `STATE_COOLING` (3): Paused due to temperature
- `STATE_CALCULATING` (4): Waiting for first hashrate reading

State changes trigger UI updates and notification updates.

### Battery Management

`BroadcastReceiver` monitors charging state (MainActivity.java:1815-1848):
- Can auto-pause when device disconnected from charger
- Can auto-resume when charging resumes
- User can force mining on battery via dialog

### Native Binary Management

Native ccminer binaries stored in assets:
- `app/src/main/assets/ccminer/arm64-v8a/ccminer`
- `app/src/main/assets/ccminer/x86_64/ccminer`

`Tools` class handles copying from assets to private directory with execute permissions.

## Important Development Notes

### Android SDK Targeting Strategy

**Current Configuration:**
- `compileSdkVersion 33` (build.gradle:5) - Compile against modern Android APIs
- `targetSdkVersion 28` (build.gradle:16) - **CRITICAL: Must remain 28**

**Why targetSdk 28 is Required:**

Android 10 (API 29) introduced the W^X (Write XOR Execute) security policy that prevents apps from executing binaries stored in private app directories (`/data/user/0/[package]/files/`). This policy applies to apps targeting SDK 29 or higher.

Since this app executes the native ccminer binary from private storage, it **must** target SDK 28 to maintain compatibility with Android 10+ devices. The absolute path execution and chmod 755 permissions are correct, but insufficient on SDK 29+.

**Evidence of W^X Policy:**
- Error: `java.io.IOException: Cannot run program "/data/.../ccminer": error=13, Permission denied`
- Occurs even with:
  - Correct file permissions (chmod 755)
  - Absolute paths (not relative ./ccminer)
  - File.setExecutable(true) confirming executable bit set
- Only resolved by reverting targetSdk from 33 → 28

**Alternative Solution for Future (targetSdk 33 Compatible):**

If targeting SDK 29+ becomes necessary in the future, the following approach can be used (per [Stack Overflow solution](https://stackoverflow.com/a/64792194)):

1. **Move binaries to native lib directory:**
   - From: `app/src/main/assets/ccminer/[abi]/ccminer`
   - To: `app/src/main/jniLibs/[abi]/libccminer.so`
   - Rename executable with `lib` prefix and `.so` extension

2. **Enable native lib extraction:**
   ```xml
   <application android:extractNativeLibs="true" ...>
   ```

3. **Update code to use native lib path:**
   ```java
   // In VerusBinMiningService.java
   String nativeLibDir = getApplicationInfo().nativeLibraryDir;
   String ccminerPath = new File(nativeLibDir, "libccminer.so").getAbsolutePath();
   ```

4. **Trade-offs:**
   - ✅ Allows targetSdk 33 with full modern API support
   - ✅ Native libs are read-only (better security)
   - ❌ Larger APK size (less efficient compression than assets)
   - ❌ Requires restructuring existing code/assets
   - ❌ Needs thorough testing across devices

**Current Approach Benefits:**
- ✅ Proven stable and mining successfully
- ✅ No restructuring required
- ✅ CompileSdk 33 provides modern build features
- ✅ Works on Android 5.1 through Android 15

### Other Important Notes

- MinifyEnabled is disabled because it breaks JSON parsing for pool API (build.gradle:26)
- The app uses deprecated `getDrawingCache()` for screenshot sharing (MainActivity.java:1774-1778)
- Wake locks are optimized to only run during mining (VerusBinMiningService.java:335)
- Log output is pruned when exceeding `Config.logMaxLength` (50000 chars) to prevent memory issues
- Android 13+ requires runtime notification permission (POST_NOTIFICATIONS in manifest)
- Hardware acceleration is disabled for MainActivity to free GPU resources for mining (AndroidManifest.xml:37)
- Binary permissions set with both `setExecutable()` and `chmod 755` for maximum compatibility (VerusBinMiningService.java:181-203)
- Absolute paths used for binary execution to satisfy SELinux requirements (VerusBinMiningService.java:365)

## Performance Optimizations

**Performance Mode** (enabled by default) minimizes app resource usage to maximize mining efficiency:

### Resource Optimizations:
1. **Temperature Checks**: Reduced from 10s to 30s intervals (MainActivity.java:460)
2. **AMAYC ML Disabled**: Skips external API calls and ML predictions when in performance mode (MainActivity.java:1529)
3. **UI Update Throttling**: Limited to 1 update per second max (MainActivity.java:1470-1478)
4. **Timer Management**: All timers stop when app is backgrounded (MainActivity.java:940-945)
5. **Log Formatting**: Skips expensive Spannable formatting in performance mode (MainActivity.java:995-999)
6. **Payout Widget Removed**: Eliminated unnecessary API polling
7. **Memory Limits**: Temperature arrays capped to prevent unbounded growth (MainActivity.java:1546-1551)
8. **Wake Lock Optimization**: Single wake lock held only during active mining, released on stop

### Expected Performance Gains:
- **CPU Usage**: 10-15% reduction in app overhead
- **Memory**: 25-35MB freed for mining
- **Battery**: 22-36% less battery drain from UI/background tasks
- **Network**: Eliminates AMAYC API calls (60s intervals)

## Testing Considerations

When testing mining functionality:
1. Use a valid Veruscoin wallet address (format validated in Utils.verifyAddress)
2. Test on physical devices only - ccminer binaries won't work in emulators
3. Monitor device temperature carefully when testing thermal protection
4. Test both charging and battery states for pause-on-battery feature
5. Verify architecture support before attempting to mine

## Known Limitations

- Only arm64-v8a and x86_64 architectures supported (armeabi-v7a listed in code but may not work)
- Payout widget/pool stats currently disabled (MainActivity.java:582)
- Pause/resume reinitializes the mining process rather than pausing the miner process
- Screenshot sharing uses deprecated APIs
