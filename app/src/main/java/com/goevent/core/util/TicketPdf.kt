package com.goevent.core.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.goevent.core.model.TicketDetail
import java.io.File
import java.io.FileOutputStream

object TicketPdf {
    fun generate(context: Context, ticket: TicketDetail): File? {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#121212")
            textSize = 28f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val headingPaint = Paint().apply {
            color = Color.parseColor("#6D28D9")
            textSize = 18f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val labelPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 14f
            isAntiAlias = true
        }

        val valuePaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 14f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 11f
            isAntiAlias = true
        }

        val ev = ticket.bookings?.events
        canvas.drawText(ev?.title ?: "Event Ticket", 48f, 72f, titlePaint)
        canvas.drawText("Official Digital Pass", 48f, 96f, headingPaint)

        var y = 140f
        val details = listOf(
            "Holder" to ticket.holderName,
            "Ticket Type" to (ticket.ticketType?.name ?: "Standard"),
            "Booking Ref" to (ticket.bookings?.bookingReference ?: ""),
            "Ticket Ref" to ticket.ticketReference,
            "Date" to Formatters.date(ev?.eventDate ?: ""),
            "Time" to Formatters.time(ev?.startTime ?: ""),
            "Venue" to "${ev?.venue}, ${ev?.city}",
            "Status" to ticket.status
        )

        for ((label, value) in details) {
            canvas.drawText(label, 48f, y, labelPaint)
            canvas.drawText(value, 200f, y, valuePaint)
            y += 28f
        }

        QRCodeGenerator.generateQRCode(ticket.qrToken, 480, 480)?.let { qr ->
            canvas.drawBitmap(qr, null, Rect(48, 420, 348, 720), null)
        }
        canvas.drawText(ticket.ticketReference, 48f, 750f, headingPaint)
        canvas.drawText("Show this QR code at the entrance. It is unique to this ticket.", 48f, 780f, footerPaint)

        doc.finishPage(page)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Ticket_${ticket.ticketReference}.pdf")
        try {
            FileOutputStream(file).use { out -> doc.writeTo(out) }
            doc.close()
            return file
        } catch (e: Exception) {
            doc.close()
            return null
        }
    }
}
