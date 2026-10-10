package com.example.inventariocasa.ui

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface LecturaEscaner {
    data class Leido(val codigo: String) : LecturaEscaner
    data object Cancelado : LecturaEscaner
    data class Fallo(val mensaje: String) : LecturaEscaner
}

/**
 * Abre el escáner de Google Play Services. La pantalla de cámara es suya, así que la app
 * no pide permiso de cámara ni lleva código propio para ella. A cambio, hace falta un
 * móvil con Play Services (no funciona en un emulador sin ellos).
 */
suspend fun escanearCodigo(contexto: Context): LecturaEscaner = suspendCancellableCoroutine { continuacion ->
    // Solo los formatos de producto de supermercado: así no se cuela un QR cualquiera
    val opciones = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E
        )
        .enableAutoZoom()
        .build()

    fun entregar(lectura: LecturaEscaner) {
        if (continuacion.isActive) continuacion.resume(lectura)
    }

    GmsBarcodeScanning.getClient(contexto, opciones).startScan()
        .addOnSuccessListener { codigo ->
            val valor = codigo.rawValue
            entregar(if (valor.isNullOrBlank()) LecturaEscaner.Cancelado else LecturaEscaner.Leido(valor))
        }
        .addOnCanceledListener { entregar(LecturaEscaner.Cancelado) }
        .addOnFailureListener {
            entregar(LecturaEscaner.Fallo("No se ha podido abrir el escáner. Puede que se esté descargando; prueba en un momento."))
        }
}
