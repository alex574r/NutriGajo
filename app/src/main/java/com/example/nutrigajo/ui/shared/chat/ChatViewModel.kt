package com.example.nutrigajo.ui.shared.chat

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.ChatRepository
import com.example.nutrigajo.data.repository.FotoPerfilRepository
import com.example.nutrigajo.data.repository.RechazoDelWorker
import com.example.nutrigajo.domain.model.Mensaje
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// La foto que se va a mandar: el JPEG ya reducido y una miniatura para verla en el campo de escribir.
class FotoElegida(val jpeg: ByteArray, val previa: Bitmap)

data class ChatEstado(
    val cargando: Boolean = true,
    // del más nuevo al más viejo
    val mensajes: List<Mensaje> = emptyList(),
    val borrador: String = "",
    val foto: FotoElegida? = null,
    // se está subiendo una foto (el texto sale al instante y no hace esperar)
    val enviando: Boolean = false,
    @param:StringRes val error: Int? = null,
)

private const val LADO_FOTO = 1280
private const val MAX_TEXTO = 2000

// ponytail: repositorios por defecto en el constructor; pasan a inyección (Hilt) cuando haya más ViewModels.
class ChatViewModel(
    private val repo: ChatRepository = ChatRepository(),
    private val fotos: FotoPerfilRepository = FotoPerfilRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(ChatEstado())
    val estado: StateFlow<ChatEstado> = _estado.asStateFlow()

    // la cuenta que escribe, para saber cuáles mensajes son suyos
    val yo: String get() = repo.uid

    private var abierto: String? = null
    private var escucha: Job? = null

    // Empieza a escuchar la conversación de ese paciente. Lo ya leído se conserva al volver.
    fun abrir(pacienteId: String) {
        if (abierto == pacienteId) return
        abierto = pacienteId
        _estado.update { it.copy(cargando = it.mensajes.isEmpty(), error = null) }
        escucha = viewModelScope.launch {
            try {
                repo.mensajes(pacienteId).collect { lista -> _estado.update { it.copy(cargando = false, mensajes = lista) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    // Al salir del chat deja de escuchar (y de leer del servidor).
    fun cerrar() {
        escucha?.cancel()
        escucha = null
        abierto = null
    }

    fun escribir(texto: String) = _estado.update { it.copy(borrador = texto.take(MAX_TEXTO), error = null) }

    // Reduce la foto elegida y la deja lista; se manda con el siguiente envío, con el texto como pie.
    fun elegirFoto(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val jpeg = fotos.preparar(resolver, uri, LADO_FOTO, cuadrada = false)
                val previa = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, BitmapFactory.Options().apply { inSampleSize = 4 })
                } ?: throw IOException("La foto no es una imagen")
                _estado.update { it.copy(foto = FotoElegida(jpeg, previa), error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(error = R.string.chat_error_foto) }
            }
        }
    }

    fun quitarFoto() = _estado.update { it.copy(foto = null) }

    // Lo escrito sale del campo al instante y el mensaje ya se ve en el chat; si no se puede enviar, vuelve al campo con su error.
    fun enviar(pacienteId: String, cache: File) {
        val actual = _estado.value
        val texto = actual.borrador.trim()
        val foto = actual.foto
        if (actual.enviando || (texto.isEmpty() && foto == null)) return
        _estado.update { it.copy(borrador = "", foto = null, enviando = foto != null, error = null) }
        viewModelScope.launch {
            try {
                val id = repo.enviar(pacienteId, texto, foto?.jpeg, cache)
                // el aviso a la otra persona es lo de menos: si falla, el mensaje ya está enviado
                try {
                    repo.avisar(pacienteId, id)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = when (e) {
                    is RechazoDelWorker -> R.string.chat_error_servidor
                    is IOException -> R.string.chat_error_red
                    else -> mensajeDeFirestore(e)
                }
                _estado.update { it.copy(borrador = it.borrador.ifEmpty { texto }, foto = it.foto ?: foto, error = error) }
            } finally {
                _estado.update { it.copy(enviando = false) }
            }
        }
    }

    suspend fun cargarFoto(pacienteId: String, mensajeId: String, cache: File): Bitmap = repo.foto(pacienteId, mensajeId, cache)

    // La foto de perfil de la otra persona del chat, o null si no tiene.
    suspend fun fotoDeLaOtra(uid: String, cache: File): Bitmap? = fotos.avatarAjeno(cache, uid)
}
