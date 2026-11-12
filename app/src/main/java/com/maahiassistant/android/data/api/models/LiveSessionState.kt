package com.maahiassistant.android.data.api.models

enum class SessionOrbState {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    MUTED,
    ERROR
}

enum class ConnectionStatus(val displayText: String) {
    DISCONNECTED("Tap orb to start"),
    CONNECTING("Connecting…"),
    LISTENING("Listening"),
    SPEAKING("Speaking"),
    MUTED("Muted"),
    RECONNECTING("Reconnecting…"),
    NO_NETWORK("No connection, retrying…"),
    ERROR("Session failed")
}
