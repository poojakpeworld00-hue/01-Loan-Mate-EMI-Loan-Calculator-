package com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.Tongue

/** One selectable answer: a row shows [glyph] (or [image] as an avatar) in its tile; a card shows [image] or [glyph] in its icon tile. */
data class PackChoice(
    @StringRes val name: Int,
    val sub: (Context) -> String = { "" },
    @DrawableRes val image: Int = 0,
    val glyph: String = "",
)

data class Phase(
    /** Stable id (OB_1…) used by Remote Config (`onboarding.order`, ad place `onboarding.<key>`) and saved answers. */
    val key: String,
    @StringRes val title: Int,
    @StringRes val sub: Int,
    /** Short name shown next to the step counter. */
    @StringRes val name: Int,
    val opts: List<PackChoice>,
    /** Header icon on the gradient tile. */
    @DrawableRes val icon: Int,
    /** Icon cards instead of rows. */
    val cards: Boolean = false,
    val columns: Int = 1,
    val multi: Boolean = false,
    val centered: Boolean = false,
    val defaultSel: Set<Int> = setOf(0),
)

private fun s(@StringRes id: Int): (Context) -> String = { it.getString(id) }

const val LANG_KEY = "OB_1"
const val CURRENCY_KEY = "OB_4"

/** Currency step options, in order; the picked code becomes the calculators' currency. */
val CURRENCIES = listOf(
    "USD" to "$", "INR" to "₹", "KWD" to "KD", "BHD" to "BD", "OMR" to "RO", "JOD" to "JD",
    "GBP" to "£", "EUR" to "€", "CHF" to "Fr", "AED" to "د.إ", "SAR" to "﷼",
)
const val DEFAULT_HOME_KEY = "default_home"
const val WELCOME_KEY = "welcome"
const val PERMISSION_HUB_KEY = "permission_hub"

/** Steps whose rows are information, not answers: a tap does what the button does. */
val INFO_STEPS = setOf(DEFAULT_HOME_KEY, WELCOME_KEY, PERMISSION_HUB_KEY)

/** Phase 1, also reused by the Settings language screen: native name first, English-language name below. */
fun tongueStep(languages: List<Tongue>) =
    Phase(
        LANG_KEY, R.string.lang_title, R.string.lang_sub, R.string.step_lang,
        languages.map { l ->
            PackChoice(l.nativeName, { c -> c.getString(l.name).takeIf { it != c.getString(l.nativeName) }.orEmpty() }, glyph = l.glyph)
        },
        icon = R.drawable.glyph_language,
    )

/** Every onboarding step (the loan questions + the launcher's set-as-default-home step); Remote Config picks and orders them. */
fun setupPhases(languages: List<Tongue>): List<Phase> =
    listOf(tongueStep(languages)) + QUESTION_STEPS + listOf(WELCOME_STEP, PERMISSION_HUB_STEP, DEFAULT_HOME_STEP)

private val WELCOME_STEP = Phase(
    WELCOME_KEY, R.string.wl_title, R.string.wl_sub, R.string.step_welcome, defaultSel = emptySet(), icon = R.drawable.glyph_star,
    opts = listOf(PackChoice(R.string.wl_c1, glyph = "★"), PackChoice(R.string.wl_c2, glyph = "◆"), PackChoice(R.string.wl_c3, glyph = "◈")),
)

private val PERMISSION_HUB_STEP = Phase(
    PERMISSION_HUB_KEY, R.string.ph_title, R.string.ph_sub, R.string.step_permissions, defaultSel = emptySet(), icon = R.drawable.glyph_bell,
    opts = listOf(PackChoice(R.string.ph_notif, { it.getString(R.string.ph_notif_sub) }, glyph = "✦")),
)

/** Set as default home: three numbered instructions (not answers), then the system "Default home app" chooser. */
private val DEFAULT_HOME_STEP = Phase(
    DEFAULT_HOME_KEY, R.string.dh_title, R.string.dh_sub, R.string.step_default_home, defaultSel = emptySet(), icon = R.drawable.glyph_home,
    opts = listOf(PackChoice(R.string.dh_c1, glyph = "1"), PackChoice(R.string.dh_c2, glyph = "2"), PackChoice(R.string.dh_c3, glyph = "3")),
)

/** Claude Design "Loan Cash" onboarding: currency → amount → loan type → repayment → gender → age → employment → existing EMI. */
private val QUESTION_STEPS = listOf(
    Phase(
        CURRENCY_KEY, R.string.cur_title, R.string.cur_sub, R.string.step_currency, icon = R.drawable.ob_ic_coin,
        opts = CURRENCIES.zip(
            listOf(
                R.string.cur_usd, R.string.cur_inr, R.string.cur_kwd, R.string.cur_bhd, R.string.cur_omr, R.string.cur_jod,
                R.string.cur_gbp, R.string.cur_eur, R.string.cur_chf, R.string.cur_aed, R.string.cur_sar,
            )
        ) { (code, sym), name -> PackChoice(name, { "$code · $sym" }, glyph = sym) },
    ),
    Phase(
        "OB_5", R.string.amt_title, R.string.amt_sub, R.string.step_amount, icon = R.drawable.ob_ic_cash, defaultSel = setOf(1),
        opts = listOf(
            PackChoice(R.string.amt_1, s(R.string.amt_1_sub), glyph = "💵"),
            PackChoice(R.string.amt_2, s(R.string.amt_2_sub), glyph = "💰"),
            PackChoice(R.string.amt_3, s(R.string.amt_3_sub), glyph = "🏦"),
            PackChoice(R.string.amt_4, s(R.string.amt_4_sub), glyph = "🏠"),
            PackChoice(R.string.amt_5, s(R.string.amt_5_sub), glyph = "🏢"),
        ),
    ),
    Phase(
        "OB_6", R.string.type_title, R.string.type_sub, R.string.step_type, icon = R.drawable.ob_ic_stack, cards = true, columns = 2,
        opts = listOf(
            PackChoice(R.string.type_personal, s(R.string.type_personal_sub), glyph = "🤝"),
            PackChoice(R.string.type_home, s(R.string.type_home_sub), glyph = "🏠"),
            PackChoice(R.string.type_car, s(R.string.type_car_sub), glyph = "🚗"),
            PackChoice(R.string.type_education, s(R.string.type_education_sub), glyph = "🎓"),
            PackChoice(R.string.type_business, s(R.string.type_business_sub), glyph = "💼"),
            PackChoice(R.string.type_gold, s(R.string.type_gold_sub), glyph = "🪙"),
            PackChoice(R.string.type_medical, s(R.string.type_medical_sub), glyph = "🏥"),
            PackChoice(R.string.type_travel, s(R.string.type_travel_sub), glyph = "✈️"),
        ),
    ),
    Phase(
        "OB_7", R.string.rep_title, R.string.rep_sub, R.string.step_repay, icon = R.drawable.ob_ic_calendar, defaultSel = setOf(3),
        opts = listOf(
            PackChoice(R.string.rep_6m, s(R.string.rep_short), glyph = "6M"),
            PackChoice(R.string.rep_1y, s(R.string.rep_short), glyph = "1Y"),
            PackChoice(R.string.rep_2y, s(R.string.rep_balanced), glyph = "2Y"),
            PackChoice(R.string.rep_3y, s(R.string.rep_balanced), glyph = "3Y"),
            PackChoice(R.string.rep_5y, s(R.string.rep_long), glyph = "5Y"),
        ),
    ),
    Phase(
        "OB_2", R.string.gender_title, R.string.gender_sub, R.string.step_gender, icon = R.drawable.glyph_person,
        cards = true, columns = 2, centered = true, defaultSel = setOf(2),
        opts = listOf(
            PackChoice(R.string.gender_male, glyph = "♂"),
            PackChoice(R.string.gender_female, glyph = "♀"),
            PackChoice(R.string.gender_other, glyph = "⊘"),
        ),
    ),
    Phase(
        "OB_3", R.string.age_title, R.string.age_sub, R.string.step_age, icon = R.drawable.glyph_cake,
        cards = true, columns = 2, centered = true, defaultSel = setOf(1),
        opts = listOf(
            PackChoice(R.string.age_r1, s(R.string.age_years), glyph = "🎒"),
            PackChoice(R.string.age_r2, s(R.string.age_years), glyph = "💼"),
            PackChoice(R.string.age_r3, s(R.string.age_years), glyph = "🏠"),
            PackChoice(R.string.age_r4, s(R.string.age_years), glyph = "🏅"),
            PackChoice(R.string.age_r5, s(R.string.age_years), glyph = "🪑"),
        ),
    ),
    Phase(
        "OB_8", R.string.emp_title, R.string.emp_sub, R.string.step_emp, icon = R.drawable.ob_ic_briefcase, cards = true, columns = 2,
        opts = listOf(
            PackChoice(R.string.emp_salaried, s(R.string.emp_salaried_sub), glyph = "💼"),
            PackChoice(R.string.emp_self, s(R.string.emp_self_sub), glyph = "🧰"),
            PackChoice(R.string.emp_business, s(R.string.emp_business_sub), glyph = "🏪"),
            PackChoice(R.string.emp_freelancer, s(R.string.emp_freelancer_sub), glyph = "💻"),
            PackChoice(R.string.emp_student, s(R.string.emp_student_sub), glyph = "🎓"),
            PackChoice(R.string.emp_unemployed, s(R.string.emp_unemployed_sub), glyph = "⏳"),
        ),
    ),
    Phase(
        "OB_9", R.string.emi_title, R.string.emi_sub, R.string.step_emi, icon = R.drawable.ob_ic_receipt,
        cards = true, columns = 2, centered = true, defaultSel = setOf(1),
        opts = listOf(
            PackChoice(R.string.emi_yes, glyph = "🧾"),
            PackChoice(R.string.emi_no, glyph = "✅"),
        ),
    ),
)
