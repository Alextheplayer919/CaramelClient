package org.chimeramc.client.ui.compose.caramel.launcher

import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticInfo
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Cosmetics filter chips must filter the grid.
 *
 * <p>The chips moved their own highlight but the grid kept showing every row, so picking "Capes"
 * or "Favorites" changed nothing. [filterCosmetics] is the pure rule the grid now applies; these
 * tests pin the index-to-kind mapping the chips and the helper must agree on.
 */
class CaramelCosmeticFilterTest {

    private fun info(id: String, kind: CaramelCosmeticKind, favourite: Boolean) =
        CaramelCosmeticInfo(id, id, kind, false, true, favourite)

    private val all = listOf(
        info("cape_1", CaramelCosmeticKind.Cape, true),
        info("cape_2", CaramelCosmeticKind.Cape, false),
        info("wings_1", CaramelCosmeticKind.Wings, false),
        info("hat_1", CaramelCosmeticKind.Hat, true),
        info("pet_1", CaramelCosmeticKind.Pet, false),
    )

    @Test
    fun allReturnsEverything() {
        assertEquals(all.size, filterCosmetics(all, 0).size)
    }

    @Test
    fun eachKindChipSelectsOnlyItsKind() {
        assertEquals(2, filterCosmetics(all, 1).size)
        assertTrue(filterCosmetics(all, 1).all { it.kind == CaramelCosmeticKind.Cape })
        assertTrue(filterCosmetics(all, 2).all { it.kind == CaramelCosmeticKind.Wings })
        assertTrue(filterCosmetics(all, 3).all { it.kind == CaramelCosmeticKind.Hat })
        assertTrue(filterCosmetics(all, 4).all { it.kind == CaramelCosmeticKind.Pet })
    }

    @Test
    fun theFavouritesChipSelectsOnlyTheStarred() {
        val starred = filterCosmetics(all, 5)
        assertEquals(2, starred.size)
        assertTrue(starred.all { it.favourite })
    }

    @Test
    fun aFavouriteFreeCatalogueYieldsAnEmptyFavouritesView() {
        assertTrue(filterCosmetics(listOf(info("cape_1", CaramelCosmeticKind.Cape, false)), 5).isEmpty())
    }
}
