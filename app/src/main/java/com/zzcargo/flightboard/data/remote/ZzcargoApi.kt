package com.zzcargo.flightboard.data.remote

import com.zzcargo.flightboard.data.model.Airline
import com.zzcargo.flightboard.data.model.Flight
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class ZzcargoApi(
    private val client: OkHttpClient = defaultHttpClient(),
    private val baseUrl: String = "https://www.zzcargo.com",
    private val retryDelayMillis: (Int) -> Long = { attempt -> if (attempt == 1) 500L else 1_500L }
) : FlightSource {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun fetchAll(onProgress: (Int, Int) -> Unit): List<Flight> = coroutineScope {
        val first = fetchPageWithRetry(1)
        val total = first.pages
        if (total < 1) throw ApiException("服务器返回的页数无效")
        onProgress(1, total)
        if (total == 1) {
            return@coroutineScope first.records
        }

        val all = first.records.toMutableList()
        val completed = AtomicInteger(1)
        val semaphore = Semaphore(3)
        (2..total).map { page ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val data = fetchPageWithRetry(page)
                    synchronized(all) { all.addAll(data.records) }
                    onProgress(completed.incrementAndGet(), total)
                }
            }
        }.awaitAll()
        all.toList()
    }

    suspend fun fetchPage(page: Int): FlightPageData = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("current", page.toString())
            .add("flightNo", "")
            .add("execDate", "")
            .add("ioAttr", "")
            .add("agency", "")
            .add("startStationCn", "")
            .add("terminalStationCn", "")
            .build()

        val request = Request.Builder()
            .url("$baseUrl/flightDynamic/skipTo?jstime=${System.currentTimeMillis()}")
            .post(body)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "$baseUrl/airline/1")
            .header("Origin", baseUrl)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ApiException("服务器响应异常（HTTP ${response.code}）")
            }
            val text = response.body?.string() ?: throw ApiException("服务器返回为空")
            val page = try {
                json.decodeFromString(FlightPage.serializer(), text)
            } catch (e: Exception) {
                throw ApiException("数据解析失败", e)
            }
            if (page.code != 200) throw ApiException("服务器返回错误（code=${page.code}）")
            val data = page.data ?: throw ApiException("服务器返回数据为空")
            if (data.pages < 1) throw ApiException("服务器返回的页数无效")
            data
        }
    }

    override suspend fun fetchAirlines(): List<Airline> = coroutineScope {
        val first = fetchAirlinePageWithRetry(1)
        val total = first.pages
        if (total < 1) {
            return@coroutineScope emptyList()
        }
        val rest = if (total > 1) {
            val semaphore = Semaphore(3)
            (2..total).map { page ->
                async(Dispatchers.IO) {
                    semaphore.withPermit { fetchAirlinePageWithRetry(page).records }
                }
            }.awaitAll().flatten()
        } else {
            emptyList()
        }
        (first.records + rest)
            .filter { it.code.isNotBlank() }
            .distinctBy { it.code }
    }

    private suspend fun fetchAirlinePage(page: Int): AirlinePageData =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder()
                .add("current", page.toString())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/airline/airlineTwo?jstime=${System.currentTimeMillis()}")
                .post(body)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "$baseUrl/airline/1")
                .header("Origin", baseUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ApiException("航司列表响应异常（HTTP ${response.code}）")
                }
                val text = response.body?.string() ?: throw ApiException("航司列表返回为空")
                val page = try {
                    json.decodeFromString(AirlinePage.serializer(), text)
                } catch (e: Exception) {
                    throw ApiException("航司列表解析失败", e)
                }
                if (page.code != 200) {
                    throw ApiException("航司列表返回错误（code=${page.code}）")
                }
                page.data ?: throw ApiException("航司列表数据为空")
            }
        }

    private suspend fun fetchAirlinePageWithRetry(page: Int): AirlinePageData {
        var last: Exception? = null
        repeat(3) { index ->
            try {
                return fetchAirlinePage(page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                last = e
                if (index < 2) delay(retryDelayMillis(index + 1))
            }
        }
        throw ApiException("航司列表第 $page 页获取失败：${last?.message ?: "未知错误"}", last)
    }

    private suspend fun fetchPageWithRetry(page: Int): FlightPageData {
        var last: Exception? = null
        repeat(3) { index ->
            try {
                return fetchPage(page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                last = e
                if (index < 2) delay(retryDelayMillis(index + 1))
            }
        }
        throw ApiException("第 $page 页获取失败：${last?.message ?: "未知错误"}", last)
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/156.0.0.0 Safari/537.36 Edg/156.0.0.0"
    }
}

private fun defaultHttpClient(): OkHttpClient {
    val trustManager = GdcaTrust.trustManager()
    val sslContext = GdcaTrust.sslContext(trustManager)
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustManager)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()
}
