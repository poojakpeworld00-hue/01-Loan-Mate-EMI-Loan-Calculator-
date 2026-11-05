package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.ToolEntry

class ToolsRepo {

    private val tools = listOf(
        ToolEntry("length", R.string.ln_tools_length_title, R.string.ln_tools_length_subtitle, R.drawable.ln_ic_tool_length, R.drawable.ln_bg_oval_blue, R.color.ln_grid_blue_fg),
        ToolEntry("speed", R.string.ln_tools_speed_title, R.string.ln_tools_speed_subtitle, R.drawable.ln_ic_tool_speed, R.drawable.ln_bg_oval_pink, R.color.ln_grid_pink_fg),
        ToolEntry("temperature", R.string.ln_tools_temperature_title, R.string.ln_tools_temperature_subtitle, R.drawable.ln_ic_tool_temperature, R.drawable.ln_bg_oval_red, R.color.ln_grid_red_fg),
        ToolEntry("age", R.string.ln_tools_age_title, R.string.ln_tools_age_subtitle, R.drawable.ln_ic_tool_age, R.drawable.ln_bg_oval_green, R.color.ln_grid_green_fg),
        ToolEntry("mass", R.string.ln_tools_mass_title, R.string.ln_tools_mass_subtitle, R.drawable.ln_ic_tool_mass, R.drawable.ln_bg_oval_purple, R.color.ln_grid_purple_fg),
    )

    fun getAll(): List<ToolEntry> = tools
}
