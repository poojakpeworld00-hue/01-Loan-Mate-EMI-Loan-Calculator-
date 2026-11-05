package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.ToolEntry

class ToolsRepo {

    private val tools = listOf(
        ToolEntry("length", R.string.ln_tools_length_title, R.string.ln_tools_length_subtitle, R.drawable.ln_ic_tool_length),
        ToolEntry("speed", R.string.ln_tools_speed_title, R.string.ln_tools_speed_subtitle, R.drawable.ln_ic_tool_speed),
        ToolEntry("temperature", R.string.ln_tools_temperature_title, R.string.ln_tools_temperature_subtitle, R.drawable.ln_ic_tool_temperature),
        ToolEntry("age", R.string.ln_tools_age_title, R.string.ln_tools_age_subtitle, R.drawable.ln_ic_tool_age),
        ToolEntry("mass", R.string.ln_tools_mass_title, R.string.ln_tools_mass_subtitle, R.drawable.ln_ic_tool_mass),
    )

    fun getAll(): List<ToolEntry> = tools
}
