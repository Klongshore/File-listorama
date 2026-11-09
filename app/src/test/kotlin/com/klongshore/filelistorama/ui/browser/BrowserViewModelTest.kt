package com.klongshore.filelistorama.ui.browser

import com.klongshore.filelistorama.data.local.LocalFileSource
import com.klongshore.filelistorama.domain.BrowserPreferences
import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.SortKey
import com.klongshore.filelistorama.domain.ViewSettings
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

    private fun viewModel(prefs: BrowserPreferences = InMemoryBrowserPreferences()) = BrowserViewModel(
        LocalFileSource(root, displayName = "Internal storage", io = Dispatchers.Unconfined),
        prefs,
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

    // --- sorting and hidden files ---

    private fun BrowserViewModel.names() = entries().map { it.name }

    private fun fileOfSize(path: String, bytes: Int) = file(path, "x".repeat(bytes))

    private fun touch(path: String, millis: Long) {
        File(root, path).setLastModified(millis)
    }

    @Test
    fun `sorting by name descending reverses each group but keeps folders first`() {
        file("a.txt")
        file("b.txt")
        file("alpha/x")
        file("zeta/x")
        val vm = viewModel()

        vm.setSort(SortKey.Name, ascending = false)

        assertEquals(listOf("zeta", "alpha", "b.txt", "a.txt"), vm.names())
    }

    @Test
    fun `name sorting ignores case`() {
        file("banana.txt")
        file("Apple.txt")
        file("cherry.txt")

        assertEquals(listOf("Apple.txt", "banana.txt", "cherry.txt"), viewModel().names())
    }

    @Test
    fun `sorting by date orders files oldest first or newest first`() {
        file("mid.txt")
        file("old.txt")
        file("new.txt")
        touch("old.txt", 1_000_000_000_000L)
        touch("mid.txt", 1_100_000_000_000L)
        touch("new.txt", 1_200_000_000_000L)
        val vm = viewModel()

        vm.setSort(SortKey.Date, ascending = true)
        assertEquals(listOf("old.txt", "mid.txt", "new.txt"), vm.names())

        vm.setSort(SortKey.Date, ascending = false)
        assertEquals(listOf("new.txt", "mid.txt", "old.txt"), vm.names())
    }

    @Test
    fun `sorting by size orders files smallest first or largest first and folders stay first`() {
        fileOfSize("big.bin", 300)
        fileOfSize("small.bin", 10)
        fileOfSize("medium.bin", 100)
        file("folder/x")
        val vm = viewModel()

        vm.setSort(SortKey.Size, ascending = true)
        assertEquals(listOf("folder", "small.bin", "medium.bin", "big.bin"), vm.names())

        vm.setSort(SortKey.Size, ascending = false)
        assertEquals(listOf("folder", "big.bin", "medium.bin", "small.bin"), vm.names())
    }

    @Test
    fun `hidden files are not shown by default`() {
        file(".secret")
        file(".hiddenfolder/x")
        file("visible.txt")

        assertEquals(listOf("visible.txt"), viewModel().names())
    }

    @Test
    fun `turning on show hidden reveals them and turning it off hides them again`() {
        file(".secret")
        file("visible.txt")
        val vm = viewModel()

        vm.setShowHidden(true)
        assertEquals(listOf(".secret", "visible.txt"), vm.names())

        vm.setShowHidden(false)
        assertEquals(listOf("visible.txt"), vm.names())
    }

    @Test
    fun `changing a setting updates the visible list without reopening the folder`() {
        file("a.txt")
        file("b.txt")
        val vm = viewModel()
        assertEquals(listOf("a.txt", "b.txt"), vm.names())

        vm.setSort(SortKey.Name, ascending = false)

        assertEquals(listOf("b.txt", "a.txt"), vm.names())
        assertEquals(SortKey.Name, vm.state.value.settings.sortKey)
        assertFalse(vm.state.value.settings.ascending)
    }

    @Test
    fun `settings chosen in one session are applied in the next`() {
        file(".secret")
        file("a.txt")
        file("b.txt")
        val prefs = InMemoryBrowserPreferences()
        val first = viewModel(prefs)
        first.setSort(SortKey.Name, ascending = false)
        first.setShowHidden(true)

        val second = viewModel(prefs)

        assertEquals(listOf("b.txt", "a.txt", ".secret"), second.names())
        assertEquals(ViewSettings(SortKey.Name, ascending = false, showHidden = true), second.state.value.settings)
    }

    @Test
    fun `sorting and hidden settings apply inside sub folders too`() {
        file("sub/.hidden")
        file("sub/a.txt")
        file("sub/b.txt")
        val vm = viewModel()
        vm.setSort(SortKey.Name, ascending = false)
        vm.setShowHidden(true)

        vm.open(vm.entry("sub"))

        assertEquals(listOf("b.txt", "a.txt", ".hidden"), vm.names())
    }
}
