package com.klongshore.filelistorama.ui.browser

import com.klongshore.filelistorama.data.local.LocalFileSource
import com.klongshore.filelistorama.domain.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var root: File

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        root = temp.newFolder("storage")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun file(path: String, content: String = "x") {
        File(root, path).apply { parentFile?.mkdirs() }.writeText(content)
    }

    private fun viewModel() = BrowserViewModel(
        LocalFileSource(root, displayName = "Internal storage", io = Dispatchers.Unconfined),
    )

    private fun BrowserViewModel.entries(): List<Entry> =
        (state.value.listing as Listing.Loaded).entries

    private fun BrowserViewModel.entry(name: String): Entry = entries().single { it.name == name }

    private fun BrowserViewModel.crumbNames() = state.value.crumbs.map { it.name }

    @Test
    fun `starts at the root showing its contents with a single breadcrumb`() {
        file("notes.txt")
        file("photos/a.jpg")

        val vm = viewModel()

        assertEquals(listOf("Internal storage"), vm.crumbNames())
        assertEquals(setOf("notes.txt", "photos"), vm.entries().map { it.name }.toSet())
    }

    @Test
    fun `folders are listed before files, each group in name order`() {
        file("b.txt")
        file("a.txt")
        file("zeta/x")
        file("alpha/x")

        val vm = viewModel()

        assertEquals(listOf("alpha", "zeta", "a.txt", "b.txt"), vm.entries().map { it.name })
    }

    @Test
    fun `opening a folder shows its contents and extends the breadcrumb`() {
        file("photos/a.jpg")
        file("photos/b.jpg")
        val vm = viewModel()

        vm.open(vm.entry("photos"))

        assertEquals(listOf("Internal storage", "photos"), vm.crumbNames())
        assertEquals(listOf("a.jpg", "b.jpg"), vm.entries().map { it.name })
    }

    @Test
    fun `opening a file does not navigate`() {
        file("notes.txt")
        val vm = viewModel()

        vm.open(vm.entry("notes.txt"))

        assertEquals(listOf("Internal storage"), vm.crumbNames())
    }

    @Test
    fun `tapping an earlier breadcrumb jumps up to that level`() {
        file("a/b/c/deep.txt")
        val vm = viewModel()
        vm.open(vm.entry("a"))
        vm.open(vm.entry("b"))
        vm.open(vm.entry("c"))

        vm.navigateTo(1)

        assertEquals(listOf("Internal storage", "a"), vm.crumbNames())
        assertEquals(listOf("b"), vm.entries().map { it.name })
    }

    @Test
    fun `navigating up goes one level and reports it handled`() {
        file("a/b/x.txt")
        val vm = viewModel()
        vm.open(vm.entry("a"))
        vm.open(vm.entry("b"))

        val handled = vm.navigateUp()

        assertTrue(handled)
        assertEquals(listOf("Internal storage", "a"), vm.crumbNames())
    }

    @Test
    fun `navigating up from the top level is not handled so the app can exit`() {
        file("a.txt")
        val vm = viewModel()

        val handled = vm.navigateUp()

        assertFalse(handled)
        assertEquals(listOf("Internal storage"), vm.crumbNames())
    }

    @Test
    fun `a folder that disappears shows not found and the user can still go up`() {
        file("gone/x.txt")
        val vm = viewModel()
        val gone = vm.entry("gone")
        File(root, "gone").deleteRecursively()

        vm.open(gone)

        assertEquals(Listing.Failed(Failure.NotFound), vm.state.value.listing)
        assertTrue(vm.navigateUp())
    }

    @Test
    fun `an unreadable folder shows permission denied`() {
        file("locked/x.txt")
        val vm = viewModel()
        val locked = vm.entry("locked")
        File(root, "locked").setReadable(false)
        assumeTrue("running as a user who can bypass file permissions", !File(root, "locked").canRead())

        vm.open(locked)

        File(root, "locked").setReadable(true)
        assertEquals(Listing.Failed(Failure.PermissionDenied), vm.state.value.listing)
    }
}
