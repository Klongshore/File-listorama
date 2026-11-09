package com.klongshore.filelistorama.ui.access

import com.klongshore.filelistorama.domain.StoragePermission
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageAccessViewModelTest {

    /** Stands in for the system's All files access switch, which a JVM test cannot flip. */
    private class FakePermission(var granted: Boolean) : StoragePermission {
        override fun isGranted() = granted
    }

    @Test
    fun `first launch without permission shows the explanation`() {
        val vm = StorageAccessViewModel(FakePermission(granted = false))

        assertEquals(StorageAccess.Explaining, vm.state.value)
    }

    @Test
    fun `a user who already granted access goes straight to the browser`() {
        val vm = StorageAccessViewModel(FakePermission(granted = true))

        assertEquals(StorageAccess.Granted, vm.state.value)
    }

    @Test
    fun `a user who already granted access never sees the explanation on later checks`() {
        val vm = StorageAccessViewModel(FakePermission(granted = true))

        vm.refresh()
        vm.refresh()

        assertEquals(StorageAccess.Granted, vm.state.value)
    }

    @Test
    fun `returning from settings with access granted shows the browser`() {
        val permission = FakePermission(granted = false)
        val vm = StorageAccessViewModel(permission)

        vm.requestAccess()
        permission.granted = true
        vm.refresh()

        assertEquals(StorageAccess.Granted, vm.state.value)
    }

    @Test
    fun `returning from settings without access shows the denied state`() {
        val vm = StorageAccessViewModel(FakePermission(granted = false))

        vm.requestAccess()
        vm.refresh()

        assertEquals(StorageAccess.Denied, vm.state.value)
    }

    @Test
    fun `resuming before ever visiting settings keeps showing the explanation`() {
        val vm = StorageAccessViewModel(FakePermission(granted = false))

        vm.refresh()

        assertEquals(StorageAccess.Explaining, vm.state.value)
    }

    @Test
    fun `retrying after a denial and then granting shows the browser`() {
        val permission = FakePermission(granted = false)
        val vm = StorageAccessViewModel(permission)
        vm.requestAccess()
        vm.refresh()
        assertEquals(StorageAccess.Denied, vm.state.value)

        vm.requestAccess()
        permission.granted = true
        vm.refresh()

        assertEquals(StorageAccess.Granted, vm.state.value)
    }

    @Test
    fun `retrying after a denial and being denied again stays denied`() {
        val vm = StorageAccessViewModel(FakePermission(granted = false))
        vm.requestAccess()
        vm.refresh()

        vm.requestAccess()
        vm.refresh()

        assertEquals(StorageAccess.Denied, vm.state.value)
    }
}
