package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class PeriodFilter(val label: String) {
    AUJOURDHUI("Aujourd'hui"),
    CETTE_SEMAINE("Cette semaine"),
    CE_MOIS("Ce mois"),
    CETTE_ANNEE("Cette année"),
    PERSONNALISE("Plage perso.")
}

enum class StatutVenteFilter(val code: String, val label: String) {
    TOUS("TOUS", "Tous"),
    PAYE("PAYE", "Soldés"),
    PARTIEL("PARTIEL", "Partiels"),
    IMPAYE("IMPAYE", "Impayés")
}

enum class ModePaiement(val code: String, val label: String) {
    ESPECES("ESPECES", "Espèces"),
    CHEQUE("CHEQUE", "Chèque"),
    VIREMENT("VIREMENT", "Virement"),
    AUTRE("AUTRE", "Autre");

    companion object {
        fun fromCode(code: String): ModePaiement =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ESPECES
    }
}

object Formatters {
    private val dzdSymbols = DecimalFormatSymbols(Locale.FRENCH).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }
    private val integerFormatter = DecimalFormat("#,##0", dzdSymbols)
    private val decimalFormatter = DecimalFormat("#,##0.00", dzdSymbols)

    fun formatDzd(amount: java.math.BigDecimal, compact: Boolean = false): String {
        return formatDzd(amount.toDouble(), compact)
    }

    fun formatDzd(amount: Double, compact: Boolean = false): String {
        if (compact && abs(amount) >= 1_000_000) {
            val millions = amount / 1_000_000.0
            return "${DecimalFormat("#,##0.0", dzdSymbols).format(millions)} M DA"
        }
        val hasDecimals = abs(amount - amount.toLong()) >= 0.01
        val formatted = if (hasDecimals) {
            decimalFormatter.format(amount)
        } else {
            integerFormatter.format(amount)
        }
        return "$formatted DA"
    }

    fun formatDateShort(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.FRENCH)
        return sdf.format(Date(millis))
    }

    fun formatDayMonth(periodeLabel: String): String {
        // Expects YYYY-MM-DD or YYYY-MM
        val parts = periodeLabel.split("-")
        return when (parts.size) {
            3 -> "${parts[2]}/${parts[1]}"
            2 -> {
                val monthNames = arrayOf(
                    "Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
                    "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"
                )
                val m = parts[1].toIntOrNull() ?: 1
                monthNames.getOrElse(m - 1) { parts[1] }
            }
            else -> periodeLabel
        }
    }

    fun getTemporalGroupLabel(dateVenteMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val startOfToday = (calNow.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = (calNow.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.SATURDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis

        val startOfMonth = (calNow.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return when {
            dateVenteMillis >= startOfToday -> "Aujourd'hui"
            dateVenteMillis >= startOfWeek -> "Cette semaine"
            dateVenteMillis >= startOfMonth -> "Ce mois"
            else -> "Plus ancien"
        }
    }

    fun computePeriodBounds(
        filter: PeriodFilter,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val endOfToday = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return when (filter) {
            PeriodFilter.AUJOURDHUI -> {
                val start = (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to endOfToday
            }
            PeriodFilter.CETTE_SEMAINE -> {
                val start = (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, -6)
                }.timeInMillis
                start to endOfToday
            }
            PeriodFilter.CE_MOIS -> {
                val start = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to endOfToday
            }
            PeriodFilter.CETTE_ANNEE -> {
                val start = (cal.clone() as Calendar).apply {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to endOfToday
            }
            PeriodFilter.PERSONNALISE -> {
                val defaultStart = (cal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val start = customStartMillis ?: defaultStart
                val end = if (customEndMillis != null) {
                    Calendar.getInstance().apply {
                        timeInMillis = customEndMillis
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                } else {
                    endOfToday
                }
                minOf(start, end) to maxOf(start, end)
            }
        }
    }

    fun dialClient(context: Context, phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(context, "Aucun numéro de téléphone renseigné", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phone.trim()}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible d'ouvrir le composeur téléphonique", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareClientStatement(
        context: Context,
        clientNom: String,
        telephone: String,
        totalCommande: Double,
        totalPaye: Double,
        soldeDebiteur: Double
    ) {
        val dateStr = formatDateShort(System.currentTimeMillis())
        val message = buildString {
            appendLine("🍄 *MycoConserve Pro — Relevé de Compte*")
            appendLine("Client : *$clientNom*")
            appendLine("Date : $dateStr")
            appendLine("────────────────────")
            appendLine("• Total commandé : ${formatDzd(totalCommande)}")
            appendLine("• Total réglé : ${formatDzd(totalPaye)}")
            appendLine("• *Reste à payer : ${formatDzd(soldeDebiteur)}*")
            appendLine("────────────────────")
            append("Merci pour votre confiance.")
        }

        try {
            val cleanPhone = telephone.replace(Regex("[^0-9+]"), "")
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Relevé de compte - $clientNom")
                putExtra(Intent.EXTRA_TEXT, message)
                if (cleanPhone.isNotEmpty()) {
                    putExtra("jid", "${cleanPhone.removePrefix("+")}@s.whatsapp.net")
                }
            }
            val chooser = Intent.createChooser(sendIntent, "Envoyer le relevé via WhatsApp / SMS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible de partager le relevé", Toast.LENGTH_SHORT).show()
        }
    }
}
