package com.zzcargo.flightboard.data.remote

import java.io.IOException

class ApiException(message: String, cause: Throwable? = null) : IOException(message, cause)
