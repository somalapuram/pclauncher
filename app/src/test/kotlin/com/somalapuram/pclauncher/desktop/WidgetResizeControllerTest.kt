package com.somalapuram.pclauncher.desktop

import android.os.UserHandle
import com.somalapuram.pclauncher.core.apps.AppChange
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.AppInventoryRepository
import com.somalapuram.pclauncher.core.apps.AppSource
import com.somalapuram.pclauncher.core.data.layout.DesktopCell
import com.somalapuram.pclauncher.core.data.layout.DesktopLayout
import com.somalapuram.pclauncher.core.data.layout.DesktopPlacement
import com.somalapuram.pclauncher.core.data.layout.DesktopSpan
import com.somalapuram.pclauncher.core.data.layout.InMemoryDesktopLayoutStore
import com.somalapuram.pclauncher.core.data.layout.ResizeEdge
import com.somalapuram.pclauncher.core.data.layout.ResizePermission
import com.somalapuram.pclauncher.core.data.layout.widgetPlacementId
import com.somalapuram.pclauncher.core.data.pins.InMemoryPinStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * That one handle drag resizes by one cell, however many times it is reported.
 *
 * `resizedBy` is pure and exhaustively tested, but it only answers "given this span, what is the
 * next one" — it cannot know that a drag reports *cumulative* pixels. The first implementation
 * applied every report as a fresh increment against the span the previous report had just written,
 * so a single 107 px drag arrived as ten reports and compounded a 3-cell widget to 5 before the
 * finger lifted. The defect is in how the controller feeds the pure function, so that is what this
 * covers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetResizeControllerTest {

    private val scheduler = TestCoroutineScheduler()

    private val widgetId = 6
    private val id = widgetPlacementId(widgetId)

    // The Google Search widget's real constraint: horizontal only, never narrower than 3 cells.
    private val permission = ResizePermission(horizontal = true, vertical = true, minColumns = 3)

    /** The reports a real handle emits: pixels since the drag began, growing frame by frame. */
    private fun dragReports(totalPixels: Float, frames: Int) =
        (1..frames).map { totalPixels * it / frames }

    private lateinit var store: InMemoryDesktopLayoutStore

    /**
     * The controller's own scope.
     *
     * Not `runTest`'s scope — its collectors never finish and would hold the test open — and not
     * `backgroundScope` either, whose coroutines `advanceUntilIdle` does not drive, so the layout
     * would never arrive. A separate scope on the shared scheduler is both driven and cancellable.
     */
    private val shellScope = CoroutineScope(StandardTestDispatcher(scheduler))

    @After
    fun tearDown() = shellScope.cancel()

    private fun controller(): ShellController {
        store = InMemoryDesktopLayoutStore(
            DesktopLayout(listOf(DesktopPlacement(id, DesktopCell(2, 3), DesktopSpan(3, 1)))),
        )
        return ShellController(
            repository = AppInventoryRepository(
                EmptyAppSource,
                StandardTestDispatcher(scheduler),
            ),
            pinStore = InMemoryPinStore(),
            layoutStore = store,
            scope = shellScope,
            userSerial = 0L,
        ).also { it.start() }
    }

    private fun ShellController.drag(pixels: Float) = resizeWidget(
        widgetId = widgetId,
        edge = ResizeEdge.Right,
        pixels = pixels,
        cellSize = 96f,
        permission = permission,
        columnsAvailable = 19,
        rowsAvailable = 8,
    ) { _, _ -> }

    @Test
    fun `a one-cell drag reported many times grows by exactly one cell`() = runTest(scheduler) {
        val shell = controller()
        advanceUntilIdle()
        
        shell.beginResize(widgetId)
        dragReports(totalPixels = 107f, frames = 10).forEach { shell.drag(it); advanceUntilIdle() }
        shell.endResize()
        advanceUntilIdle()

        assertEquals(
            "cumulative reports compounded instead of measuring from the drag's start",
            DesktopSpan(4, 1),
            shell.layout.value.spanFor(id),
        )
    }

    @Test
    fun `each drag measures from where that drag began, not the one before`() = runTest(scheduler) {
        val shell = controller()
        advanceUntilIdle()

        repeat(2) {
            shell.beginResize(widgetId)
            dragReports(totalPixels = 100f, frames = 5).forEach { shell.drag(it); advanceUntilIdle() }
            shell.endResize()
            advanceUntilIdle()
        }

        // Two separate one-cell drags: 3 -> 4 -> 5. A base that never reset would stop at 4.
        assertEquals(DesktopSpan(5, 1), shell.layout.value.spanFor(id))
    }

    @Test
    fun `a drag the provider forbids leaves the widget alone`() = runTest(scheduler) {
        val shell = controller()
        advanceUntilIdle()

        shell.beginResize(widgetId)
        shell.drag(-96f) // 3 -> 2 columns, below the provider's minimum
        advanceUntilIdle()
        shell.endResize()

        assertEquals(DesktopSpan(3, 1), shell.layout.value.spanFor(id))
    }
}

/** No apps: this exercises layout, and an inventory would only add scheduling noise. */
private object EmptyAppSource : AppSource {
    override fun profiles(): List<UserHandle> = emptyList()
    override fun entriesFor(user: UserHandle): List<AppEntry> = emptyList()
    override fun entriesFor(packageName: String, user: UserHandle): List<AppEntry> = emptyList()
    override fun observeChanges(onChange: (AppChange) -> Unit) = AutoCloseable {}
}
