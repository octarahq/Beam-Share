package beamshare.octarahq.com

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TransferReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settingsManager = SettingsManager(context)
        val senderId = intent.getStringExtra("sender_id")

        when (intent.action) {
            "beamshare.octarahq.com.ACTION_ACCEPT" -> {
                IncomingTransferManager.decide(true)
                val popupIntent = Intent(context, IncomingTransferActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(popupIntent)
            }
            "beamshare.octarahq.com.ACTION_DECLINE" -> {
                IncomingTransferManager.decide(false)
            }
            "beamshare.octarahq.com.ACTION_BLOCK" -> {
                IncomingTransferManager.decide(false)
                senderId?.let {
                    settingsManager.blacklistDevice(it)
                }
            }
        }
    }
}
