package com.zzcargo.flightboard.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.zzcargo.flightboard.data.model.Flight
import com.zzcargo.flightboard.util.DateTimes
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportManager(private val context: Context) : FlightExporter {

    override suspend fun export(flights: List<Flight>, nowMillis: Long): ExportedFile =
        withContext(Dispatchers.IO) {
            val bytes = XlsxWriter.write(flights)
            val displayName = "郑州机场货运航班动态_${DateTimes.fileStamp(nowMillis)}.xlsx"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, XLSX_MIME)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/郑州机场航班动态"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("无法创建下载文件")
            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IOException("无法写入下载文件")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                ExportedFile(uri.toString(), displayName)
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            }
        }

    override fun open(file: ExportedFile): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(file.uri), XLSX_MIME)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    } catch (e: Exception) {
        false
    }

    override fun share(file: ExportedFile): Boolean = try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = XLSX_MIME
            putExtra(Intent.EXTRA_STREAM, Uri.parse(file.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "分享航班表")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (e: Exception) {
        false
    }

    companion object {
        const val XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}
