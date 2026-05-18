package com.octarahq.beamshare

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class BeamShareTileService : TileService() {

    companion object {
        fun requestUpdate(context: Context) {
            requestListeningState(context, android.content.ComponentName(context, BeamShareTileService::class.java))
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        val settingsManager = SettingsManager(this)
        val tile = qsTile ?: return
        
        val mode = settingsManager.visibilityMode
        
        if (mode == VisibilityMode.DISABLED) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Beam Share : Off"
        } else {
            tile.state = Tile.STATE_ACTIVE
            tile.label = when(mode) {
                VisibilityMode.TRUSTED -> "Beam Share : Confiance"
                else -> "Beam Share : Ouvert"
            }
        }
        
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "receive")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
