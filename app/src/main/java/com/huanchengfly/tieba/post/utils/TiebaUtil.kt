package com.huanchengfly.tieba.post.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.os.Build
import android.os.PersistableBundle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.WebViewActivity
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CheckReportBean
import com.huanchengfly.tieba.post.components.dialogs.LoadingDialog
import com.huanchengfly.tieba.post.components.workers.OKSignWork
import com.huanchengfly.tieba.post.pendingIntentFlagImmutable
import com.huanchengfly.tieba.post.receivers.AutoSignAlarm
import com.huanchengfly.tieba.post.toastShort
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

object TiebaUtil {
    private fun ClipData.setIsSensitive(isSensitive: Boolean): ClipData {
        description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, isSensitive)
        }
        return this
    }

    @JvmStatic
    @JvmOverloads
    fun copyText(
        context: Context,
        text: String?,
        toast: String = context.getString(R.string.toast_copy_success),
        isSensitive: Boolean = false
    ) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Tieba Lite", text).setIsSensitive(isSensitive)
        cm.setPrimaryClip(clipData)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            context.toastShort(toast)
        }
    }

    fun initAutoSign(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val autoSign = context.appPreferences.autoSign
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AutoSignAlarm::class.java),
            pendingIntentFlagImmutable()
        )
        if (autoSign) {
            val autoSignTimeStr = context.appPreferences.autoSignTime!!
            val time = autoSignTimeStr.split(":".toRegex()).toTypedArray()
            val hour = time[0].toInt()
            val minute = time[1].toInt()
            val calendar = Calendar.getInstance()
            calendar[Calendar.HOUR_OF_DAY] = hour
            calendar[Calendar.MINUTE] = minute
            if (calendar.timeInMillis >= System.currentTimeMillis()) {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } else {
            alarmManager.cancel(pendingIntent)
        }
    }

    @JvmStatic
    fun startSign(context: Context) {
        context.appPreferences.signDay = Calendar.getInstance()[Calendar.DAY_OF_MONTH]
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            try {
//                context.startForegroundService(
//                    Intent()
//                        .setClassName(
//                            context.packageName,
//                            "${context.packageName}.services.OKSignService"
//                        )
//                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                        .setAction(OKSignService.ACTION_START_SIGN)
//                )
//            } catch (e: IllegalStateException) {
//                e.printStackTrace()
//            }
//        } else {
//            context.startService(
//                Intent()
//                    .setClassName(
//                        context.packageName,
//                        "${context.packageName}.services.OKSignService"
//                    )
//                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    .setAction(OKSignService.ACTION_START_SIGN)
//            )
//        }
        val okSignWork = OneTimeWorkRequestBuilder<OKSignWork>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("OKSign", ExistingWorkPolicy.KEEP, okSignWork)
    }

    @JvmStatic
    @JvmOverloads
    fun shareText(context: Context, text: String, title: String? = null) {
        context.startActivity(Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "${if (title != null) "「$title」\n" else ""}$text\n（分享自贴吧 Lite）"
            )
        })
    }

    @JvmStatic
    fun reportPost(context: Context, postId: String) {
        val dialog = LoadingDialog(context).apply { show() }
        TiebaApi.getInstance()
            .checkReportPost(postId)
            .enqueue(object : Callback<CheckReportBean> {
                override fun onResponse(
                    call: Call<CheckReportBean>,
                    response: Response<CheckReportBean>
                ) {
                    dialog.dismiss()
                    WebViewActivity.launch(context, response.body()!!.data.url)
                }

                override fun onFailure(call: Call<CheckReportBean>, t: Throwable) {
                    dialog.dismiss()
                    context.toastShort(R.string.toast_load_failed)
                }
            })
    }
}