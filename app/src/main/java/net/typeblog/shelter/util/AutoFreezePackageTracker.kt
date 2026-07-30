package net.typeblog.shelter.util

internal data class WorkPackageObservation(
    val packageName: String,
    val isSystem: Boolean,
    val isInstalled: Boolean,
)

internal object AutoFreezePackageTracker {
    fun trackablePackages(observations: Collection<WorkPackageObservation>): List<String> =
        observations.asSequence()
            .filter { !it.isSystem && it.isInstalled }
            .map { it.packageName }
            .distinct()
            .toList()

    fun packagesToOptOutWhenClearingAll(
        existingOptOut: Collection<String>,
        currentPackages: Collection<String>,
        selectedPackages: Collection<String>,
        pendingPackages: Collection<String>,
    ): List<String> = LinkedHashSet<String>().apply {
        addAll(existingOptOut.filter { it.isNotEmpty() })
        addAll(currentPackages.filter { it.isNotEmpty() })
        addAll(selectedPackages.filter { it.isNotEmpty() })
        addAll(pendingPackages.filter { it.isNotEmpty() })
    }.toList()
}
