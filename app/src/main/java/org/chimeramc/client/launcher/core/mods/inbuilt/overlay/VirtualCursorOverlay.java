package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;

public class VirtualCursorOverlay extends BaseOverlayButton {

    public VirtualCursorOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.VIRTUAL_CURSOR;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_cursor_overlay;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.overlay_virtual_cursor;
    }

    @Override
    protected float getWidthScale() {
        return 1.8f;
    }

    @Override
    protected float getHeightScale() {
        return 0.65f;
    }

    @Override
    protected void onButtonClick() {
        boolean currentState = VirtualCursorMod.isActive();
        VirtualCursorMod.setActive(!currentState, activity);
        
        if (!currentState) {
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, activity.getString(R.string.virtual_cursor_enabled));
        } else {
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, activity.getString(R.string.virtual_cursor_disabled));
        }
    }

    @Override
    public void hide() {
        if (VirtualCursorMod.isActive()) {
            VirtualCursorMod.setActive(false, activity);
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, activity.getString(R.string.virtual_cursor_disabled));
        }
        super.hide();
    }
}