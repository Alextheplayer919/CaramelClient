package org.chimeramc.client.ui.compose.caramel.launcher

import org.chimeramc.client.core.changelog.LaunchChangelog
import org.chimeramc.client.core.changelog.ReleaseInfo
import org.chimeramc.client.ui.compose.caramel.CaramelReleaseNotes
import org.chimeramc.client.ui.compose.caramel.CaramelReleaseSection

/**
 * Turns the bundled release notes into the shape the Release notes popup draws.
 *
 * <p>[LaunchChangelog] groups the document by the contributor-facing theme it was written in
 * ("Module Catalogue — Phase A"); a player reads a release as Added, Changed and Fixed. This folds
 * each section into that bucket and keeps the three buckets in that order, so the popup is the same
 * order every release.
 *
 * <p>Pure and Android-free: the section titles are kind keys the UI resolves to a string resource,
 * so this part is unit-tested without a device. A missing, blank or wrong-schema document yields
 * notes with nothing to show.
 */
object CaramelReleaseNotesLoader {

    const val KIND_ADDED = "added"
    const val KIND_CHANGED = "changed"
    const val KIND_FIXED = "fixed"
    const val KIND_OTHER = "other"

    private val ORDER = listOf(KIND_ADDED, KIND_CHANGED, KIND_FIXED, KIND_OTHER)

    /** Builds the notes for [version] from the release-notes JSON; never throws. */
    @JvmStatic
    fun fromJson(json: String?, version: String): CaramelReleaseNotes {
        val changelog = LaunchChangelog.parse(json)
        val dateLine = ReleaseInfo(changelog.releasedAt, changelog.supportUntil).summaryLine()
        if (changelog.sections.isEmpty() && changelog.headline.isEmpty()
            && changelog.summary.isEmpty() && dateLine.isEmpty()
        ) {
            return CaramelReleaseNotes(version, "", "", "", emptyList())
        }
        val byKind = LinkedHashMap<String, MutableList<String>>()
        for (section in changelog.sections) {
            val key = kindKey(section.kind)
            val lines = byKind.getOrPut(key) { mutableListOf() }
            for (entry in section.entries) {
                if (entry.text.isNotEmpty()) lines.add(entry.text)
            }
        }
        val sections = ORDER.mapNotNull { key ->
            val lines = byKind[key] ?: return@mapNotNull null
            if (lines.isEmpty()) null else CaramelReleaseSection(key, lines)
        }
        return CaramelReleaseNotes(version, changelog.headline, changelog.summary, dateLine, sections)
    }

    private fun kindKey(kind: LaunchChangelog.Kind): String = when (kind) {
        LaunchChangelog.Kind.ADDED -> KIND_ADDED
        LaunchChangelog.Kind.CHANGED -> KIND_CHANGED
        LaunchChangelog.Kind.FIXED -> KIND_FIXED
        else -> KIND_OTHER
    }
}
