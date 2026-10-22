package com.example.nutrigajo.ui.shared

import androidx.annotation.StringRes
import com.example.nutrigajo.R
import com.google.firebase.firestore.FirebaseFirestoreException

// Mensaje para un fallo de Firestore: sin conexión o genérico. Los casos propios de cada pantalla los resuelve su ViewModel.
@StringRes
fun mensajeDeFirestore(e: Exception): Int =
    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        R.string.error_red
    } else {
        R.string.error_generico
    }
