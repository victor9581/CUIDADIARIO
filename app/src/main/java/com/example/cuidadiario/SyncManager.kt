package com.example.cuidadiario

import android.content.Context
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object SyncManager {

    const val BASE_URL = "https://cuidadiario.alwaysdata.net/api_cuidadiario"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Registro de nuevo usuario
    fun registrarUsuario(
        nombre: String,
        correo: String,
        password: String,
        onResultado: (Boolean, String, JSONObject?) -> Unit
    ) {
        val endpoint = "$BASE_URL/registro.php"
        val jsonBody = JSONObject().apply {
            put("nombre", nombre)
            put("correo", correo)
            put("password", password)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(endpoint).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResultado(false, "Sin conexión al servidor: ${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                var body = response.body?.string() ?: ""
                try {
                    // Limpiar respuesta antes del JSON
                    val jsonStart = body.indexOf('{')
                    if (jsonStart != -1) {
                        body = body.substring(jsonStart)
                    }

                    val resJson = JSONObject(body)
                    val exito = resJson.optBoolean("exito", false)
                    val mensaje = resJson.optString("mensaje", "")
                    val usuarioObj = resJson.optJSONObject("usuario")
                    onResultado(exito, mensaje, usuarioObj)
                } catch (e: Exception) {
                    onResultado(false, "Respuesta inesperada: $body", null)
                }
            }
        })
    }

    // Inicio de sesión
    fun iniciarSesion(
        correo: String,
        password: String,
        onResultado: (Boolean, String, JSONObject?) -> Unit
    ) {
        val endpoint = "$BASE_URL/login.php"
        val jsonBody = JSONObject().apply {
            put("correo", correo)
            put("password", password)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(endpoint).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResultado(false, "Sin conexión al servidor: ${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                var body = response.body?.string() ?: ""
                try {
                    // Limpiar respuesta antes del JSON
                    val jsonStart = body.indexOf('{')
                    if (jsonStart != -1) {
                        body = body.substring(jsonStart)
                    }

                    val resJson = JSONObject(body)
                    val exito = resJson.optBoolean("exito", false)
                    val mensaje = resJson.optString("mensaje", "")
                    val usuarioObj = resJson.optJSONObject("usuario")
                    onResultado(exito, mensaje, usuarioObj)
                } catch (e: Exception) {
                    onResultado(false, "Respuesta inesperada: $body", null)
                }
            }
        })
    }

    // Enviar nuevo medicamento al servidor
    fun enviarMedicamentoAlServidor(
        context: Context,
        nombre: String,
        dosis: String,
        diasTotales: Int,
        intervaloHoras: Int,
        fechaInicio: String,
        horaInicio: String,
        esMascota: Boolean,
        nombreMascota: String,
        esDePorVida: Boolean,
        onResultado: (Boolean, String) -> Unit
    ) {
        val endpoint = "$BASE_URL/guardar_medicamento.php"
        val jsonBody = JSONObject().apply {
            put("usuario_id", SessionManager.getUserId(context))
            put("nombre", nombre)
            put("dosis", dosis)
            put("dias_totales", diasTotales)
            put("intervalo_horas", intervaloHoras)
            put("fecha_inicio", fechaInicio)
            put("hora_inicio", horaInicio)
            put("es_mascota", if (esMascota) 1 else 0)
            put("nombre_mascota", nombreMascota)
            put("es_de_por_vida", if (esDePorVida) 1 else 0)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(endpoint).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResultado(false, e.message ?: "Error de red")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                try {
                    val resJson = JSONObject(body)
                    onResultado(resJson.optBoolean("exito", false), resJson.optString("mensaje"))
                } catch (e: Exception) {
                    onResultado(false, body)
                }
            }
        })
    }

    // Enviar toma auditada con GPS
    fun enviarTomaAlServidor(
        context: Context,
        medId: Int,
        nombreFarmaco: String,
        dosis: String,
        destinatario: String,
        fechaHora: String,
        latitud: Double,
        longitud: Double,
        direccion: String,
        onResultado: (Boolean, String) -> Unit
    ) {
        val endpoint = "$BASE_URL/sincronizar_toma.php"
        val jsonBody = JSONObject().apply {
            put("usuario_id", SessionManager.getUserId(context))
            put("med_id", medId)
            put("nombre_farmaco", nombreFarmaco)
            put("dosis", dosis)
            put("destinatario", destinatario)
            put("fecha_hora", fechaHora)
            put("latitud", latitud)
            put("longitud", longitud)
            put("direccion", direccion)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(endpoint).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResultado(false, e.message ?: "Sin conexión al servidor")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                try {
                    val resJson = JSONObject(body)
                    onResultado(resJson.optBoolean("exito", false), resJson.optString("mensaje"))
                } catch (e: Exception) {
                    onResultado(false, body)
                }
            }
        })
    }

    // Enviar nueva tarea
    fun enviarTareaAlServidor(
        context: Context,
        nombre: String,
        fecha: String,
        hora: String,
        onResultado: (Boolean, String) -> Unit
    ) {
        val endpoint = "$BASE_URL/guardar_tarea.php"
        val jsonBody = JSONObject().apply {
            put("usuario_id", SessionManager.getUserId(context))
            put("nombre", nombre)
            put("fecha", fecha)
            put("hora", hora)
            put("minutos_aplazado", 10)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(endpoint).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResultado(false, e.message ?: "Error de red")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                try {
                    val resJson = JSONObject(body)
                    onResultado(resJson.optBoolean("exito", false), resJson.optString("mensaje"))
                } catch (e: Exception) {
                    onResultado(false, body)
                }
            }
        })
    }

    // Actualizar tarea con GPS
    fun actualizarTareaEnServidor(
        nombre: String,
        fecha: String,
        completada: Boolean,
        latitud: Double,
        longitud: Double,
        onResultado: (Boolean, String) -> Unit
    ) {
        val endpoint = "$BASE_URL/actualizar_tarea.php"
        val jsonBody = JSONObject().apply {
            put("nombre", nombre)
            put("fecha", fecha)
            put("completada", if (completada) 1 else 0)
            put("latitud", latitud)
            put("longitud", longitud)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(endpoint).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResultado(false, e.message ?: "Error de red")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                try {
                    val resJson = JSONObject(body)
                    onResultado(resJson.optBoolean("exito", false), resJson.optString("mensaje"))
                } catch (e: Exception) {
                    onResultado(false, body)
                }
            }
        })
    }

    // Descargar y restaurar datos desde el servidor
    fun sincronizarBajadaDesdeServidor(context: Context, onTerminado: () -> Unit) {
        val userId = SessionManager.getUserId(context)
        val endpoint = "$BASE_URL/descargar_datos.php?usuario_id=$userId"

        val request = Request.Builder().url(endpoint).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Si está sin conexión, continúa con lo que tenga
                onTerminado()
            }

            override fun onResponse(call: Call, response: Response) {
                var body = response.body?.string() ?: ""
                try {
                    val jsonStart = body.indexOf('{')
                    if (jsonStart != -1) body = body.substring(jsonStart)

                    val resJson = JSONObject(body)
                    if (resJson.optBoolean("exito", false)) {
                        val dbHelper = DatabaseHelper(context)
                        // Preservar las rutas de fotos locales antes de refrescar
                        val localMeds = dbHelper.getAllMedications()
                        val localPhotosMap = localMeds.filter { it.imagePath.isNotEmpty() }
                            .associate { it.name.trim().lowercase() to it.imagePath }

                        // Limpiar SQLite local para actualizar con la información del servidor
                        dbHelper.limpiarTodaLaBaseLocal()

                        // Restaurar medicamentos preservando la foto si existe localmente
                        val medsArray = resJson.optJSONArray("medicamentos")
                        if (medsArray != null) {
                            for (i in 0 until medsArray.length()) {
                                val m = medsArray.getJSONObject(i)
                                val medName = m.optString("nombre")
                                val photoPath = localPhotosMap[medName.trim().lowercase()] ?: ""

                                dbHelper.insertMedication(
                                    name = medName,
                                    dose = m.optString("dosis"),
                                    totalDays = m.optInt("dias_totales", 7),
                                    intervalHours = m.optInt("intervalo_horas", 8),
                                    startDate = m.optString("fecha_inicio"),
                                    imagePath = photoPath,
                                    startTime = m.optString("hora_inicio", "08:00"),
                                    isPet = m.optInt("es_mascota", 0) == 1,
                                    petName = m.optString("nombre_mascota", ""),
                                    isLifetime = m.optInt("es_de_por_vida", 0) == 1
                                )
                            }
                        }

                        // Restaurar tareas
                        val tasksArray = resJson.optJSONArray("tareas")
                        if (tasksArray != null) {
                            for (i in 0 until tasksArray.length()) {
                                val t = tasksArray.getJSONObject(i)
                                dbHelper.insertTask(
                                    name = t.optString("nombre"),
                                    date = t.optString("fecha"),
                                    time = t.optString("hora")
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onTerminado()
            }
        })
    }

    // Sincronizar Tarea hacia MySQL
    fun enviarTareaServidor(context: Context, nombre: String, fecha: String, hora: String) {
        val userId = SessionManager.getUserId(context)
        val json = JSONObject().apply {
            put("usuario_id", userId)
            put("nombre", nombre)
            put("fecha", fecha)
            put("hora", hora)
            put("completada", 0)
        }

        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/guardar_tarea.php")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { e.printStackTrace() }
            override fun onResponse(call: Call, response: Response) { /* Sincronizado */ }
        })
    }

    // Sincronizar Toma con GPS hacia MySQL
    fun enviarTomaServidor(
        context: Context,
        medicamentoId: Int,
        nombreFarmaco: String,
        dosis: String,
        destinatario: String,
        latitud: Double,
        longitud: Double,
        direccion: String,
        fechaHora: String
    ) {
        val userId = SessionManager.getUserId(context)
        val json = JSONObject().apply {
            put("usuario_id", userId)
            put("medicamento_id", medicamentoId)
            put("nombre_farmaco", nombreFarmaco)
            put("dosis", dosis)
            put("destinatario", destinatario)
            put("latitud", latitud)
            put("longitud", longitud)
            put("direccion_texto", direccion)
            put("fecha_hora_texto", fechaHora)
        }

        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/guardar_toma.php")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { e.printStackTrace() }
            override fun onResponse(call: Call, response: Response) { /* Sincronizado */ }
        })
    }

    // Sincronizar tarea cumplida con GPS
    fun sincronizarCompletarTareaGPS(
        context: Context,
        tareaId: Int,
        latitud: Double,
        longitud: Double,
        direccion: String,
        onCompleto: (() -> Unit)? = null
    ) {
        val userId = SessionManager.getUserId(context)
        val endpoint = "$BASE_URL/completar_tarea.php"

        val json = JSONObject().apply {
            put("tarea_id", tareaId)
            put("usuario_id", userId)
            put("latitud", latitud)
            put("longitud", longitud)
            put("direccion_texto", direccion)
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onCompleto?.invoke()
            }
            override fun onResponse(call: Call, response: Response) {
                onCompleto?.invoke()
            }
        })
    }

    fun sincronizarTomaGPS(
        context: Context,
        medicamentoId: Int,
        nombre: String,
        dosis: String,
        destinatario: String,
        fechaHora: String,
        latitud: Double,
        longitud: Double,
        direccion: String
    ) {
        Thread {
            try {
                val url = java.net.URL("$BASE_URL/sincronizar_toma.php")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val json = JSONObject().apply {
                    put("usuario_id", SessionManager.getUserId(context))
                    put("medicamento_id", medicamentoId)
                    put("nombre_farmaco", nombre)
                    put("dosis", dosis)
                    put("destinatario", destinatario)
                    put("fecha_hora_texto", fechaHora)
                    put("latitud", latitud)
                    put("longitud", longitud)
                    put("direccion_texto", direccion)
                }

                conn.outputStream.use { os ->
                    val input = json.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = conn.responseCode
                android.util.Log.d("CuidaDiarioSync", "Respuesta sincronizar toma: $responseCode")
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // Registrar toma en el servidor
    fun registrarTomaCompleta(
        context: Context,
        medicamentoId: Int,
        nombreFarmaco: String,
        dosis: String,
        latitud: Double,
        longitud: Double,
        direccionTexto: String
    ) {
        val userId = SessionManager.getUserId(context)
        val formBody = okhttp3.FormBody.Builder()
            .add("usuario_id", userId.toString())
            .add("medicamento_id", medicamentoId.toString())
            .add("nombre_farmaco", nombreFarmaco)
            .add("dosis", dosis)
            .add("latitud", latitud.toString())
            .add("longitud", longitud.toString())
            .add("direccion_texto", direccionTexto)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/registrar_toma.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { e.printStackTrace() }
            override fun onResponse(call: Call, response: Response) { /* Sincronizado */ }
        })
    }
}