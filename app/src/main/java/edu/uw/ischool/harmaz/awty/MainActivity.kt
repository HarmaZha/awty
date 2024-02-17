package edu.uw.ischool.harmaz.awty

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.os.SystemClock

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
            val minutes = minutesInput.text.toString()

            // Check if any field is empty
            if (message.isNotEmpty() && phone.isNotEmpty() && minutes.isNotEmpty()) {
                try {
                    val minutesInt = minutes.toInt()
                    if (minutesInt > 0) {
                        validateAndToggleService(message, phone, minutesInt)
                    } else {
                        showToast("Minutes interval must be greater than 0")
                    }
                } catch (e: NumberFormatException) {
                    showToast("Minutes interval must be an integer")
                }
            } else {
                showToast("All fields must be filled")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun validateAndToggleService(message: String, phone: String, minutes: Int) {
        val regexPhone = Regex("^(\\+\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}\$")
        if (!regexPhone.matches(phone)) {
            showToast("Phone number is not in correct format")
            return
        }

        // Toggle Start/Stop
        if (button.text.toString() == "Start") {
            startService(message, phone, minutes)
            button.text = getString(R.string.stop)
        } else {
            stopService()
            button.text = getString(R.string.start)
        }
    }

    private fun startService(message: String, phone: String, minutes: Int) {
        // Disable input fields
        toggleInputs(false)

        // Register receiver if not already registered
        if (receiver == null) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val appContext = context?.applicationContext ?: return
                    val formattedMessage = formatMessage(phone, message)
                    Toast.makeText(appContext, formattedMessage, Toast.LENGTH_SHORT).show()
                }
            }
            registerReceiver(receiver, IntentFilter(ALARM_ACTION))
        }

        // Schedule the alarm
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ALARM_ACTION)
        val pendingIntentFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, pendingIntentFlag)

        val triggerAtMillis = SystemClock.elapsedRealtime() + minutes * 60 * 1000L
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAtMillis,
            (minutes * 60 * 1000).toLong(), // Explicitly casting the interval to Long
            pendingIntent
        )
    }

    private fun formatMessage(phone: String, message: String): String {
        // Format the phone number to be in the form (123) 456-7890
        val formattedPhone = phone.replace(Regex("^(\\d{3})(\\d{3})(\\d{4})$"), "($1) $2-$3")
        // Return the formatted message
        return "$formattedPhone: $message"
    }

    private fun stopService() {
        // Enable input fields
        toggleInputs(true)

        // Unregister receiver and cancel alarm
        if (receiver != null) {
            unregisterReceiver(receiver)
            receiver = null
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(ALARM_ACTION)
            val pendingIntentFlag =
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, pendingIntentFlag)
            alarmManager.cancel(pendingIntent)
        }
    }



    private fun toggleInputs(enabled: Boolean) {
        messageInput.isEnabled = enabled
        phoneInput.isEnabled = enabled
        minutesInput.isEnabled = enabled
    }
}
