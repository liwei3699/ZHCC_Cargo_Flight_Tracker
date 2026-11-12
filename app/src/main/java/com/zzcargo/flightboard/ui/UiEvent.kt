package com.zzcargo.flightboard.ui

import com.zzcargo.flightboard.export.ExportedFile

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class ExportReady(val file: ExportedFile) : UiEvent
    data class ExportFailed(val text: String) : UiEvent
}
