package com.rajiv.paisatrack.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.rajiv.paisatrack.MainActivity
import com.rajiv.paisatrack.R
import com.rajiv.paisatrack.data.Store
import com.rajiv.paisatrack.logic.Summary
import com.rajiv.paisatrack.ui.inr

/** Home-screen widget: shows card dues + this month's spend. */
class SpendWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> render(context, mgr, id) }
    }

    companion object {
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, SpendWidget::class.java))
            ids.forEach { render(context, mgr, it) }
        }

        private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
            val s = Summary.compute(Store.load(context))
            val v = RemoteViews(context.packageName, R.layout.widget_spend)
            v.setTextViewText(R.id.w_card_dues, inr(s.cardDues))
            v.setTextViewText(R.id.w_month_spend, inr(s.acctSpend))
            val saved = s.netSaved
            v.setTextViewText(
                R.id.w_saved,
                if (saved >= 0) "Saved ${inr(saved)} this month" else "Overspent ${inr(-saved)} this month"
            )
            val pi = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.w_root, pi)
            mgr.updateAppWidget(id, v)
        }
    }
}
