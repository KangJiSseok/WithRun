# Polyline Navigation Bundle Generator Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the current Valhalla-coupled navigation bundle generation path in `was/` with an engine-free polyline pipeline that uses the original course GPS coordinates as the only source of truth.

**Architecture:** Keep the existing backend trigger, validation, storage, and delivery flow, but replace the generation internals with a polyline-only preprocessing pipeline. The new pipeline reads `Course.coordinates`, computes shape metrics and segments in O(n), derives running-friendly maneuvers from anchor-window bearings, assembles a versioned bundle schema, validates it, and stores it through the existing S3-backed bundle path.

**Tech Stack:** Spring Boot 3.5.11, Java 21, Jackson JSON serialization, JUnit 5, Mockito, AWS S3 storage, record/VO-based DTO design.

---

## Non-Negotiable Constraints

- Use the original `Course.coordinates` polyline exactly as stored.
- Do not use map matching, snapping, rerouting, or any routing engine.
- Do not rewrite, simplify, deduplicate, or "clean" the persisted/output shape.
- Treat this as server-side bundle generation only; runtime client navigation is out of scope.
- Any noise filtering is transient detection logic only and must not alter the stored shape.

## Current Repo Grounding

- Current generation entry: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleGenerationService.java`
- Current reusable source geometry: `was/src/main/java/kr/withrun/was/domain/course/vo/Coordinates.java`, `was/src/main/java/kr/withrun/was/domain/course/vo/GeoPoint.java`
- Current reusable distance utility: `was/src/main/java/kr/withrun/was/domain/course/util/HaversineDistanceCalculator.java`
- Current validator to rewrite: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleDocumentValidator.java`
- Engine-coupled files to bypass/remove:
  - `was/src/main/java/kr/withrun/was/domain/navigation/service/input/CourseTraceRequestMapper.java`
  - `was/src/main/java/kr/withrun/was/domain/navigation/service/input/CourseTraceResampler.java`
  - `was/src/main/java/kr/withrun/was/domain/navigation/service/valhalla/ValhallaTraceClient.java`
  - `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/valhalla/*`
  - `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleDocumentBuilder.java`
  - `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/ShapeDistanceCalculator.java`

## Target Bundle Schema

```json
{
  "schemaVersion": "navigation-bundle/v2",
  "shape": [
    {
      "lat": 35.818365,
      "lon": 128.545391,
      "elevationM": 0.0
    }
  ],
  "segments": [
    {
      "startShapeIndex": 0,
      "endShapeIndex": 1,
      "distanceMeters": 12.4,
      "bearingDegrees": 152,
      "startCumulativeDistanceMeters": 0.0,
      "endCumulativeDistanceMeters": 12.4
    }
  ],
  "maneuvers": [
    {
      "type": "RIGHT",
      "strength": "NORMAL",
      "sampleAction": "우회전",
      "instruction": "Turn right",
      "shapeIndex": 7,
      "cumulativeDistanceMeters": 84.3,
      "incomingAnchorShapeIndex": 5,
      "outgoingAnchorShapeIndex": 8,
      "bearingBeforeDegrees": 147,
      "bearingAfterDegrees": 232,
      "deltaDegrees": 85
    },
    {
      "type": "ARRIVAL",
      "sampleAction": "도착",
      "instruction": "Arrive at destination",
      "shapeIndex": 9,
      "cumulativeDistanceMeters": 132.8
    }
  ],
  "totalDistanceMeters": 132.8,
  "metadata": {
    "generatedAt": "2026-03-27T05:00:00Z",
    "generator": "polyline-navigation-bundle",
    "algorithmVersion": "v1",
    "language": "ko-KR",
    "source": "course.coordinates",
    "shapePointCount": 10,
    "segmentCount": 9
  }
}
```

## Default Detection Thresholds

- `noiseFloorMeters = 6.0` (`5-8m` allowed during tuning)
- `anchorWindowMeters = 15.0` (`12-20m` allowed during tuning)
- `mergeDistanceMeters = 24.0` (`18-30m` allowed during tuning)
- `straightThresholdDeg = 30`
- `mildThresholdDeg = 60`
- `normalThresholdDeg = 125`
- `uTurnThresholdDeg = 170`

These are running-friendly defaults, not permanent truth. Validate them against real route sets before finalizing.

## Target Package Layout

- Modify: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleGenerationService.java`
- Modify: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleDocumentValidator.java`
- Modify: `was/src/main/java/kr/withrun/was/domain/course/util/HaversineDistanceCalculator.java`
- Modify: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleDocument.java`
- Modify: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleManeuver.java`
- Modify: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleMetadata.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleShapePoint.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleSegment.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleManeuverType.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleTurnStrength.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineNavigationProperties.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineShapePreprocessor.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineManeuverDetector.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineBundleDocumentFactory.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineInstructionFormatter.java`
- Modify: `was/src/main/resources/application.yml`
- Modify: `was/src/main/resources/application-test.yml`

### Task 1: Introduce the New Bundle Contract and Validator Expectations

**Files:**
- Modify: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleDocument.java`
- Modify: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleManeuver.java`
- Modify: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleMetadata.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleShapePoint.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleSegment.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleManeuverType.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle/NavigationBundleTurnStrength.java`
- Test: `was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleDocumentValidatorTest.java`

**Step 1: Write the failing validator/schema tests**

- Rewrite `NavigationBundleDocumentValidatorTest` around the new bundle contract.
- Add tests for:
  - at least 2 shape points required
  - `segments.size() == shape.size() - 1`
  - contiguous `startShapeIndex` / `endShapeIndex`
  - monotonic cumulative distances
  - `totalDistanceMeters` matches the last segment end within tolerance
  - maneuvers sorted by `cumulativeDistanceMeters`
  - final maneuver must be `ARRIVAL` at the last shape index

**Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.NavigationBundleDocumentValidatorTest"
```

Expected: FAIL because the current DTO and validator still enforce Valhalla-shaped assumptions.

**Step 3: Implement the minimal contract changes**

- Add top-level `schemaVersion`, `shape`, `segments`, `maneuvers`, `totalDistanceMeters`, `metadata`.
- Remove `edges`, `matchedPoints`, `shapeLengths` from the persisted contract.
- Convert maneuvers from route-covering Valhalla steps into discrete maneuver events.
- Move total route distance out of metadata and into `totalDistanceMeters`.

**Step 4: Rewrite validator minimally**

- Validate shape and segment continuity instead of maneuver route coverage.
- Keep validation strict on geometry consistency, but do not require synthetic straight maneuvers.

**Step 5: Re-run tests**

Run the same validator test command.

Expected: PASS.

**Step 6: Commit**

```bash
git add was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/bundle was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleDocumentValidator.java was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleDocumentValidatorTest.java
git commit -m "test(was) : add polyline bundle schema validation"
```

### Task 2: Build O(n) Polyline Shape Preprocessing

**Files:**
- Modify: `was/src/main/java/kr/withrun/was/domain/course/util/HaversineDistanceCalculator.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineShapePreprocessor.java`
- Test: `was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineShapePreprocessorTest.java`

**Step 1: Write the failing preprocessor tests**

- Cover:
  - shape order and coordinate values preserved exactly
  - elevation passes through unchanged
  - duplicate points remain in shape and create `0.0` distance segments
  - distance and cumulative totals are correct
  - fewer than 2 valid points fail
  - null lat/lon fails

**Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.polyline.PolylineShapePreprocessorTest"
```

Expected: FAIL because the preprocessor does not exist.

**Step 3: Implement the minimal preprocessor**

- Extend `HaversineDistanceCalculator` with a public double-returning point-to-point API.
- Build one-pass segment generation over adjacent source points.
- Compute:
  - per-segment haversine distance
  - initial bearing
  - start/end cumulative distances
  - total route distance
- Return an immutable preprocessing result used later by bundle assembly and maneuver detection.

**Step 4: Re-run tests**

Run the same preprocessor test command.

Expected: PASS.

**Step 5: Commit**

```bash
git add was/src/main/java/kr/withrun/was/domain/course/util/HaversineDistanceCalculator.java was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineShapePreprocessor.java was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineShapePreprocessorTest.java
git commit -m "feat(was) : add polyline shape preprocessing"
```

### Task 3: Implement Running-Friendly Maneuver Detection

**Files:**
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineManeuverDetector.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineNavigationProperties.java`
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineInstructionFormatter.java`
- Test: `was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineManeuverDetectorTest.java`

**Step 1: Write the failing maneuver tests**

- Cover:
  - straight route -> only `ARRIVAL`
  - slight left / slight right
  - normal left / right
  - sharp turn
  - u-turn
  - micro-segment jitter suppression
  - same-direction merge within `24m`
  - true S-turn preserved as two maneuvers
  - arrival inserted exactly once

**Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.polyline.PolylineManeuverDetectorTest"
```

Expected: FAIL because the detector and properties do not exist.

**Step 3: Implement the minimal detector**

- Use anchor-window bearings instead of immediate neighbor bearings.
- Use cumulative-distance-based anchor lookup so complexity stays O(n).
- Apply transient noise suppression only for detection.
- Classify deltas with the default thresholds.
- Merge nearby same-direction turns.
- Append one terminal `ARRIVAL` event.

**Step 4: Add deterministic instruction formatting**

- Map maneuver type and configured language to simple persisted instruction strings.
- Keep `sampleAction` stable for compatibility.

**Step 5: Re-run tests**

Run the same detector test command.

Expected: PASS.

**Step 6: Commit**

```bash
git add was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineManeuverDetectorTest.java
git commit -m "feat(was) : add engine-free maneuver detection"
```

### Task 4: Assemble the New Bundle Document

**Files:**
- Create: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineBundleDocumentFactory.java`
- Test: `was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineBundleDocumentFactoryTest.java`

**Step 1: Write the failing factory tests**

- Assert:
  - `shape` is unchanged from source coordinates
  - `segments` reflect preprocessor output exactly
  - maneuvers are sorted
  - `ARRIVAL` is last
  - engine-only fields are gone
  - metadata fields are populated correctly

**Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.polyline.PolylineBundleDocumentFactoryTest"
```

Expected: FAIL because the factory does not exist.

**Step 3: Implement the minimal factory**

- Orchestrate preprocessor + detector.
- Assemble the versioned bundle DTO.
- Populate metadata including `generatedAt`, generator name, algorithm version, language, source, point count, and segment count.

**Step 4: Re-run tests**

Run the same factory test command.

Expected: PASS.

**Step 5: Commit**

```bash
git add was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineBundleDocumentFactory.java was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/polyline/PolylineBundleDocumentFactoryTest.java
git commit -m "feat(was) : add polyline bundle document factory"
```

### Task 5: Switch the Service Pipeline to Engine-Free Generation

**Files:**
- Modify: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleGenerationService.java`
- Modify: `was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleGenerationServiceTest.java`
- Modify: `was/src/main/resources/application.yml`
- Modify: `was/src/main/resources/application-test.yml`

**Step 1: Write the failing generation service tests**

- Assert:
  - the service reads `Course.coordinates`
  - it does not call Valhalla collaborators
  - it builds, validates, stores, and persists the new bundle key
  - invalid coordinates fail fast

**Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.NavigationBundleGenerationServiceTest"
```

Expected: FAIL because the service still depends on Valhalla collaborators and trace attributes.

**Step 3: Implement the minimal service switch**

- Remove `CourseTraceRequestMapper`, `ValhallaTraceClient`, and `traceAttributesEnabled` wiring.
- Inject `PolylineBundleDocumentFactory`.
- Generate directly from `Course.coordinates`.
- Keep existing storage and `Course.navigationBundleUrl` update behavior.

**Step 4: Add config defaults**

- Add `app.navigation.polyline.*` defaults to `application.yml` and `application-test.yml`.
- Remove or deprecate obsolete Valhalla bundle-generation flags.

**Step 5: Re-run tests**

Run the same generation service test command.

Expected: PASS.

**Step 6: Commit**

```bash
git add was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleGenerationService.java was/src/test/java/kr/withrun/was/domain/navigation/service/bundle/NavigationBundleGenerationServiceTest.java was/src/main/resources/application.yml was/src/main/resources/application-test.yml
git commit -m "refactor(was) : switch bundle generation to polyline pipeline"
```

### Task 6: Remove Valhalla Generation Dependencies and Obsolete Tests

**Files:**
- Delete or retire: `was/src/main/java/kr/withrun/was/domain/navigation/service/input/CourseTraceRequestMapper.java`
- Delete or retire: `was/src/main/java/kr/withrun/was/domain/navigation/service/input/CourseTraceResampler.java`
- Delete or retire: `was/src/main/java/kr/withrun/was/domain/navigation/service/valhalla/ValhallaTraceClient.java`
- Delete or retire: `was/src/main/java/kr/withrun/was/domain/navigation/dto/internal/valhalla/*`
- Delete or retire: `was/src/main/java/kr/withrun/was/domain/navigation/service/bundle/ShapeDistanceCalculator.java`
- Delete or replace tests that only validate engine-coupled behavior

**Step 1: Write the failing cleanup verification step**

- Search for remaining references to:
  - `ValhallaTraceClient`
  - `CourseTraceRequestMapper`
  - `CourseTraceResampler`
  - `SnappedRoutePoint`
  - `ValhallaTraceAttributes`

**Step 2: Remove dead production code and obsolete tests**

- Delete or fully bypass the old engine path once all new tests are green.
- Keep unrelated delivery paths intact: controller, S3 store, signed URL service, running session integration.

**Step 3: Run targeted search and full test suite**

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.polyline.*"
./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.*"
./gradlew test
```

Expected: PASS, with no remaining required production references to the old Valhalla generation path.

**Step 4: Commit**

```bash
git add was/src/main/java/kr/withrun/was/domain/navigation was/src/test/java/kr/withrun/was/domain/navigation
git commit -m "remove(was) : drop valhalla bundle generation path"
```

## Verification Checklist

- Run all new focused tests after each slice.
- Verify no new type or wiring errors remain in `was/`.
- Verify the generated bundle still stores through `NavigationBundleStore` and keeps the existing object-key delivery path.
- Verify `schemaVersion` is serialized in the stored bundle.
- Verify unchanged source coordinates are emitted in `shape` in the same order as `Course.coordinates.values()`.
- Verify duplicate points remain preserved in output shape and segments.

## Migration Notes

- This is a wire-contract change. Existing consumers that read Valhalla-specific fields must branch on `schemaVersion` or be updated before rollout.
- Existing S3 bundles may still be in the old format. New generation should emit `navigation-bundle/v2`; backfill only if clients require consistency.
- `NavigationBundleDocumentValidator` must stop requiring maneuvers to cover the full route. `segments` become the coverage source; maneuvers become discrete events.
- Remove Valhalla-specific configuration only after the new path is proven green in tests.
- Thresholds should be validated against real running routes with loops, hairpins, dense city turns, and shaky GPS before treating them as final.

## Final Execution Command Set

When executing this plan from a fresh implementation session:

```bash
cd was && ./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.NavigationBundleDocumentValidatorTest"
cd was && ./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.polyline.PolylineShapePreprocessorTest"
cd was && ./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.polyline.PolylineManeuverDetectorTest"
cd was && ./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.polyline.PolylineBundleDocumentFactoryTest"
cd was && ./gradlew test --tests "kr.withrun.was.domain.navigation.service.bundle.NavigationBundleGenerationServiceTest"
cd was && ./gradlew test
```
