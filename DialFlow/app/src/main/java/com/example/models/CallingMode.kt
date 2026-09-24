package com.example.models

enum class CallingMode(
    val title: String,
    val description: String
) {
    AUTO_CALL(
        title = "Auto",
        description = "Automatically proceed to the next queued number after the current call ends and the configured cooldown has finished."
    ),
    MANUAL_NEXT(
        title = "Manual",
        description = "Wait for the user to explicitly press the \"Begin Call\" button after the current call ends."
    )
}
