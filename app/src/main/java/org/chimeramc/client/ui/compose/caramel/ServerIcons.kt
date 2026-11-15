package org.chimeramc.client.ui.compose.caramel

import androidx.annotation.DrawableRes
import org.chimeramc.client.R

/**
 * The drawable for a server's icon key.
 *
 * <p>The mapping from an address to a key is a pure rule (see
 * [org.chimeramc.client.core.content.ServerIconCatalog]); this is the one place the key becomes a
 * resource, so the Servers list and the Quick play strip draw the same mark for the same server. A
 * key the client does not know (an empty key, or a future one) returns 0 and the caller draws its
 * generic tile rather than a wrong logo.
 */
object ServerIcons {

    @DrawableRes
    fun drawableFor(key: String): Int = when (key) {
        "hive" -> R.drawable.ic_server_hive
        "cubecraft" -> R.drawable.ic_server_cubecraft
        "galaxite" -> R.drawable.ic_server_galaxite
        "inpvp" -> R.drawable.ic_server_inpvp
        "lbsg" -> R.drawable.ic_server_lbsg
        "nethergames" -> R.drawable.ic_server_nethergames
        else -> 0
    }
}
