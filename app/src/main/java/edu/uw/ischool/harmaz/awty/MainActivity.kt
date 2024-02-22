package edu.uw.ischool.harmaz.awty

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

const val ALARM_ACTION = "edu.uw.ischool.harmaz.ALARM"

class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var messageInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var minutesInput: EditText
    private var receiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.start_button)
        messageInput = findViewById(R.id.message_input)
        phoneInput = findViewById(R.id.phone_input)
        minutesInput = findViewById(R.id.minutes_input)

        button.setOnClickListener {
            val message = messageInput.text.toString()
            val phone = phoneInput.text.toString()
            val minutes = minutesInput.text.toString().toIntOrNull()

            if (message.isNotEmpty() && phone.isNotEmpty() && minutes != null && minutes > 0) {
                validateAndToggleService(message, phone, minutes)
            } else {
                showToast("All fields must be correctly filled and minutes must be greater than 0")
            }
        }
    }

    private fun validateAndToggleService(message: String, phone: String, minutes: Int) {
        val regexPhone = Regex("^(\\+\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}\$")
        if (!regexPhone.matches(phone)) {
            showToast("Phone number is not in correct format")
            return
        }

        if (button.text.toString().equals("Start", ignoreCase = true)) {
            startService(message, phone, minutes)
            button.text = getString(R.string.stop)
        } else {
            stopService()
            button.text = getString(R.string.start)
        }
    }

    private fun startService(message: String, phone: String, minutes: Int) {
        toggleInputs(false)

        if (receiver == null) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
                }
            }
            registerReceiver(receiver, IntentFilter(ALARM_ACTION))
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ALARM_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val triggerAtMillis = SystemClock.elapsedRealtime() + minutes * 60000
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, minutes * 60000L, pendingIntent)
    }

    private fun stopService() {
        toggleInputs(true)

        if (receiver != null) {
            unregisterReceiver(receiver)
            receiver = null
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(ALARM_ACTION)
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun toggleInputs(enabled: Boolean) {
        messageInput.isEnabled = enabled
        phoneInput.isEnabled = enabled
        minutesInput.isEnabled = enabled
    }

    private fun showToast(message: String) {
        // Using Toast for simple feedback
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
