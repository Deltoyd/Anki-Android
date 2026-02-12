# Testing Patterns

**Analysis Date:** 2026-02-12

## Test Framework

**Runner:**
- **JUnit 5** (junit-jupiter-api) - Primary framework for new tests
- **JUnit 4** (junit) - Legacy tests, still in use via `@RunWith(RobolectricTestRunner::class)`
- **Robolectric** (4.x) - Runs Android framework code without emulator (fast unit tests)
- **Espresso** - UI/integration testing on real devices/emulators

**Config:**
- Main config: `/Users/rolandharper/StudioProjects/Anki-Android/build.gradle.kts` (lines 46-78)
- Test execution: `useJUnitPlatform()` enabled
- Resource inclusion: `isIncludeAndroidResources = true`
- Memory: `-Xmx2g -Xmx1g` per test process
- Parallel execution: enabled via `maxParallelForks` and JUnit 5 concurrent settings

**Run Commands:**
```bash
./gradlew jacocoUnitTestReport --daemon              # Run all unit tests with coverage
./gradlew :AnkiDroid:testPlayDebugUnitTest           # Run AnkiDroid module tests
./gradlew :libanki:test                              # Run libanki module tests
./gradlew :AnkiDroid:testPlayDebugUnitTest --tests "com.ichi2.anki.ExampleTest"   # Single test class
./gradlew :AnkiDroid:testPlayDebugUnitTest --tests "com.ichi2.anki.ExampleTest.testMethod"  # Single method
./gradlew jacocoAndroidTestReport --daemon           # Integration tests on emulator
./gradlew connectedPlayDebugAndroidTest              # Espresso tests on device/emulator
```

**Assertion Library:**
- **Hamcrest** (org.hamcrest:hamcrest) - Primary matcher library
- **Kotlin test** (kotlin-test) - Kotlin stdlib assertions and `assertEquals()`, `assertNotNull()`, etc.
- **JUnit Jupiter** (org.junit.jupiter.api.assertThrows) - JUnit 5 exception assertions

## Test File Organization

**Location:**
- Unit tests: co-located in `src/test/java/` mirrors `src/main/java/` structure
- Integration tests: `src/androidTest/java/` mirrors main code structure
- Test utilities: `/Users/rolandharper/StudioProjects/Anki-Android/testlib/src/main/java/` (shared across modules)
- libanki test utils: `/Users/rolandharper/StudioProjects/Anki-Android/libanki/testutils/src/main/java/`

**Naming:**
- Classes: `[ClassName]Test.kt` suffix (e.g., `BadgeDrawableBuilderTest.kt`, `FinderTest.kt`)
- No other file naming conventions observed

**Structure:**
```
AnkiDroid/
├── src/
│   ├── main/
│   │   └── java/com/ichi2/...
│   ├── test/
│   │   └── java/com/ichi2/.../...Test.kt
│   └── androidTest/
│       └── java/com/ichi2/.../...Test.kt
```

## Test Structure

**Suite Organization:**

```kotlin
// Pattern from RtlCompliantActionProviderTest.kt
@RunWith(RobolectricTestRunner::class)
class RtlCompliantActionProviderTest {
    @Test
    fun test_unwrapContext_will_get_activity() {
        // Arrange
        val a = Activity()
        val c: Context = ContextWrapper(...)

        // Act
        val provider = RtlCompliantActionProvider(c)

        // Assert
        assertEquals(provider.activity, a)
    }

    @Test
    fun test_unwrapContext_will_throw_on_no_activity() {
        val a = Application()
        val c: Context = ContextWrapper(...)
        assertThrows<ClassCastException> { RtlCompliantActionProvider(c) }
    }
}
```

**Patterns:**

*Setup with Base Class:*
- Extend `InMemoryAnkiTest` from libanki testutils for database tests
- Base class provides: `setUp()`, `tearDown()`, in-memory collection via `col` property
- Automatically initializes Rust backend, time mocking, logging

```kotlin
class MetaTest : InMemoryAnkiTest() {
    @Test
    fun ensureDatabaseIsInMemory() {
        val path = col.db.queryString("select file from pragma_database_list")
        assertThat("Default test database should be in-memory.", path, equalTo(""))
    }
}
```

*Parametrized Tests:*
- Use `@ParameterizedTest` with `@EnumSource`, `@MethodSource`, `@ValueSource` (JUnit 5)
- Example from `ActivityTransitionAnimationTest.kt`:

```kotlin
@ParameterizedTest
@EnumSource(Direction::class)
fun getInverseTransition_returns_same_input(direction: Direction) {
    assertThat(getInverseTransition(direction), equalTo(direction))
}

@ParameterizedTest
@MethodSource("getInverseTransition_returns_inverse_direction_args")
fun getInverseTransition_returns_inverse_direction(
    first: Direction,
    second: Direction,
) {
    assertThat(getInverseTransition(first), equalTo(second))
}

companion object {
    @JvmStatic
    fun getInverseTransition_returns_inverse_direction_args(): Stream<Arguments> =
        Stream.of(
            Arguments.of(Direction.START, Direction.END),
            Arguments.of(Direction.UP, Direction.DOWN),
        )
}
```

**Teardown:**
- `@After` methods automatically close database, reset dispatchers, clean Timber
- See `InMemoryAnkiTest.tearDown()` at `/Users/rolandharper/StudioProjects/Anki-Android/libanki/testutils/src/main/java/com/ichi2/anki/libanki/testutils/InMemoryAnkiTest.kt` (lines 88-107)
- Catches `BackendException` with "CollectionNotOpen" message (already disposed)

## Mocking

**Framework:**
- **MockK** (io.mockk:mockk) - Kotlin-native mocking
- **Mockito** - Java mocking (legacy)
- **Manual mocks** - Simple implementations for test doubles

**Patterns:**

*Collection Mocking:*
- Tests use `InMemoryAnkiTest` base class which provides real in-memory collection
- For UI tests isolating from collection: inject mock `CollectionManager` or use `TestCollectionManager`
- Example: `CollectionOnDiskTest` overrides `collectionManager` property (line 32)

```kotlin
class CollectionOnDiskTest : InMemoryAnkiTest() {
    @get:Rule
    var directory = TemporaryFolder()

    override val collectionManager =
        object : InMemoryCollectionManager() {
            override val collectionFiles: CollectionFiles
                get() = CollectionFiles.InMemoryWithMedia(directory.newFolder())
        }
}
```

*Time Mocking:*
- `TimeManager` with `MockTime` (see `InMemoryAnkiTest.setUp()` line 59)
- Freezes time at specific value: `MockTime(2020, 7, 7, 7, 0, 0, 0, 10)`
- Reset after test: `TimeManager.reset()` in tearDown

*Android Context Mocking:*
- Robolectric provides fake Android framework
- Use `ApplicationProvider.getApplicationContext<Application>()` for test context
- No need to mock Context, Android, or Resources in Robolectric tests

**What to Mock:**
- External services (network calls, file I/O)
- Backend calls when testing UI logic in isolation
- Time-dependent operations (use MockTime instead)

**What NOT to Mock:**
- Collection and database operations (use real in-memory collection)
- Android framework classes (Robolectric provides fakes)
- Coroutine dispatchers when testing async code (use `runTest` or real dispatchers)

## Fixtures and Factories

**Test Data:**

```kotlin
// From FinderTest.kt - creating test cards
super.addNoteUsingNoteTypeName("Basic (and reversed card)", "Front", "Back")
val toAnswer: Card = col.sched.card!!

// Helper functions in test base classes
private fun burySiblings(sched: Scheduler, toManuallyBury: Card): Card {
    sched.answerCard(toManuallyBury, Rating.AGAIN)
    val siblingBuried = Note(col, toManuallyBury.nid).cards()[1]
    assertThat(siblingBuried.queue, equalTo(QueueType.SiblingBuried))
    return siblingBuried
}
```

**Location:**
- Test data creation: test class methods or test base class
- Extension functions in `libanki/testutils/src/main/java/com/ichi2/anki/libanki/testutils/ext/`
  - Example: `InMemoryAnkiTest` extends `AnkiTest` which provides `addNoteUsingNoteTypeName()`
- Shared test utilities in `testlib/src/main/java/com/ichi2/testutils/`
  - `AnkiAssert` - custom assertions
  - `FailOnUnhandledExceptionRule` - catches uncaught exceptions
  - `IgnoreFlakyTestsInCIRule` - skips flaky tests on CI

## Coverage

**Requirements:**
- Not enforced per-module (code review-based)
- JaCoCo reports generated via CI
- Target: reasonable coverage for business logic, not 100% (pragmatic approach)

**View Coverage:**
```bash
./gradlew jacocoUnitTestReport --daemon
# Report: build/reports/jacoco/jacocoUnitTestReport/html/index.html
```

**CI Coverage:**
- Generated on GitHub Actions for all modules
- Available in build artifacts

## Test Types

**Unit Tests:**
- Scope: Individual functions, classes, or small feature areas
- Approach: Robolectric for Android framework tests, plain JUnit for business logic
- Speed: < 1 second typical
- Example: `RtlCompliantActionProviderTest` tests context unwrapping logic
- Run: `./gradlew :module:test` or `testPlayDebugUnitTest` for main app

**Integration Tests:**
- Scope: Multiple components working together (Activity + Fragment + ViewModel)
- Approach: Robolectric or real device/emulator
- Speed: 5-30+ seconds
- Database: Real database operations via `InMemoryAnkiTest`
- Example: `FinderTest` tests card searching with actual collection state
- Run: `./gradlew connectedPlayDebugAndroidTest`

**E2E Tests:**
- Framework: **Espresso** (androidx.test.espresso)
- Scope: Full app workflows from user perspective
- Speed: 10-60+ seconds per test
- Implementation: `ActivityScenarioRule` for launching activities
- Example pattern:
  ```kotlin
  @RunWith(AndroidJUnit4::class)
  class MyIntegrationTest {
      @get:Rule
      val activityRule = ActivityScenarioRule(MyActivity::class.java)

      @Test
      fun testUserFlow() {
          onView(withId(R.id.button)).perform(click())
          onView(withId(R.id.text)).check(matches(withText("Expected")))
      }
  }
  ```

## Common Patterns

**Async Testing:**
- JUnit 4: Use Robolectric which runs tests synchronously by default
- JUnit 5: Use `runTest` from `kotlinx.coroutines.test` for suspend functions
- ViewModel coroutine testing: instantiate ViewModel and test via `viewModelScope`
- Collection access: Always suspend via `col.operation()` which uses coroutine under the hood

**Error Testing:**
- Exception assertions: `assertThrows<ExceptionType> { block() }` (JUnit 5)
- Hamcrest: not commonly used for exception testing
- Example from `RtlCompliantActionProviderTest`:
  ```kotlin
  @Test
  fun test_unwrapContext_will_throw_on_no_activity() {
      assertThrows<ClassCastException> { RtlCompliantActionProvider(nonActivityContext) }
  }
  ```

**State Assertions:**
- Hamcrest matchers: `assertThat(actual, matcher)` (preferred)
- Common matchers: `equalTo()`, `hasSize()`, `hasItem()`, `greaterThan()`, `contains()`
- Example from `FinderTest`:
  ```kotlin
  assertThat(
      "A manually buried card should be returned",
      buriedCards,
      hasItem(manuallyBuriedCard.id),
  )
  assertThat(
      "sibling and manually buried should be the only cards returned",
      buriedCards,
      hasSize(2),
  )
  ```

**Logging in Tests:**
- Use `println()` for JVM compatibility (Timber may not work)
- Timber logging configured in `InMemoryAnkiTest.setUp()` with tree that uses println
- CI: Test results logged to stdout, parsed by CI system

**Test Naming:**
- Snake_case allowed in test names: `test_unwrapContext_will_get_activity`
- Descriptive format: `verb_subject_expected_outcome`
- Example: `searchForBuriedReturnsManuallyAndSiblingBuried`

**Database Testing:**
- Real in-memory SQLite collection via `InMemoryAnkiTest`
- No mocking of collection operations needed
- Use helper methods to set up test state:
  ```kotlin
  enableBurySiblings()  // helper to modify config
  burySiblings(col.sched, toAnswer)  // helper to bury and assert
  ```

**Coroutine Testing:**
- `Dispatchers.IO` overridable via `ioDispatcher` variable (see `CoroutineHelpers.kt` line 89)
- Tests reset main dispatcher: `Dispatchers.resetMain()` in tearDown
- ViewModels tested in test scope without special setup
- Backend progress polling tested via `monitorProgress()` suspend function

## Test Environment

**CI Environment:**
- GitHub Actions: Standard_D4ads_v5 Azure compute (4 vCPU)
- Test env var: `ANKI_TEST_MODE=1` (disables rollover time, disables fuzzing)
- Parallel forks: 4 on CI, auto-detected on local (50% of cores for macOS)
- Timeout: `forkEvery = 40` (restart JVM after 40 tests for memory)
- Logging: Test results appended to GitHub Actions step summary

**Local Development:**
- Create `local.properties` to override settings:
  ```properties
  enable_coverage=false          # Faster builds
  enable_languages=false         # Only English strings
  enable_leak_canary=false       # Disable LeakCanary
  ```

---

*Testing analysis: 2026-02-12*
