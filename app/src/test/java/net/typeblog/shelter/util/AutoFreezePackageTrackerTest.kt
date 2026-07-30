package net.typeblog.shelter.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoFreezePackageTrackerTest {
    @Test
    fun showingAllPackagesDoesNotChangeTrackedPackages() {
        val normalList = listOf(
            WorkPackageObservation("com.example.one", isSystem = false, isInstalled = true),
            WorkPackageObservation("com.example.two", isSystem = false, isInstalled = true),
        )
        val showAllList = normalList + listOf(
            WorkPackageObservation("android.system.hidden", isSystem = true, isInstalled = true),
            WorkPackageObservation("com.example.removed", isSystem = false, isInstalled = false),
        )

        assertEquals(
            AutoFreezePackageTracker.trackablePackages(normalList),
            AutoFreezePackageTracker.trackablePackages(showAllList),
        )
    }

    @Test
    fun newlyInstalledUserPackageIsTracked() {
        val packages = listOf(
            WorkPackageObservation("com.example.old", isSystem = false, isInstalled = true),
            WorkPackageObservation("com.example.new", isSystem = false, isInstalled = true),
        )

        assertEquals(
            listOf("com.example.old", "com.example.new"),
            AutoFreezePackageTracker.trackablePackages(packages),
        )
    }

    @Test
    fun clearingAllKeepsHiddenAndMissingSelectionsOptedOut() {
        val optOut = AutoFreezePackageTracker.packagesToOptOutWhenClearingAll(
            existingOptOut = listOf("com.example.already.opted.out"),
            currentPackages = listOf("com.example.visible", "android.system.hidden"),
            selectedPackages = listOf("com.example.selected.but.missing"),
            pendingPackages = listOf("com.example.pending"),
        )

        assertEquals(
            listOf(
                "com.example.already.opted.out",
                "com.example.visible",
                "android.system.hidden",
                "com.example.selected.but.missing",
                "com.example.pending",
            ),
            optOut,
        )
    }
}
