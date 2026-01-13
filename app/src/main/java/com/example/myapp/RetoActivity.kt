package com.example.myapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.myapp.ui.theme.MyAppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RetoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                RetoFormScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RetoFormScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- VARIABLES DEL FORMULARIO ---
    var tipoAccidente by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var matricula by remember { mutableStateOf("") }
    var nombreConductor by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

    // Variables para Cámara y GPS
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var ubicacion by remember { mutableStateOf<Location?>(null) }

    // --- CONFIGURACIÓN CÁMARA ---
    val launcherCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    fotoBitmap = android.graphics.BitmapFactory.decodeStream(stream)
                }
            }
        }
    }

    fun crearArchivoImagen(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", context.getExternalFilesDir(null))
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // --- CONFIGURACIÓN GPS ---
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permisosUbicacion = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    @SuppressLint("MissingPermission")
    fun obtenerUbicacion() {
        if (permisosUbicacion.allPermissionsGranted) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location -> ubicacion = location }
        } else {
            permisosUbicacion.launchMultiplePermissionRequest()
        }
    }

    // --- FUNCIÓN DE VIBRACIÓN (5 SEGUNDOS) ---
    fun vibrarCelular() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(5000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(5000)
        }
        Toast.makeText(context, "Guardando y Vibrando...", Toast.LENGTH_SHORT).show()
    }

    // --- UI DEL FORMULARIO ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Reporte de Accidente") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState), // Habilitar scroll
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. CAMPOS DE TEXTO
            OutlinedTextField(
                value = tipoAccidente,
                onValueChange = { tipoAccidente = it },
                label = { Text("Tipo (Choque, Colisión, Atropello)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fecha,
                onValueChange = { fecha = it },
                label = { Text("Fecha del Siniestro") },
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = matricula, onValueChange = { matricula = it }, label = { Text("Matrícula") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = nombreConductor, onValueChange = { nombreConductor = it }, label = { Text("Nombre Conductor") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = cedula, onValueChange = { cedula = it }, label = { Text("Cédula") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = observaciones,
                onValueChange = { observaciones = it },
                label = { Text("Observaciones") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 2. BOTONES DE EVIDENCIA
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = {
                    imageUri = crearArchivoImagen()
                    launcherCamara.launch(imageUri)
                }) {
                    Text("📷 Tomar Foto")
                }

                Button(onClick = { obtenerUbicacion() }) {
                    Text("📍 Obtener GPS")
                }
            }

            // 3. MOSTRAR RESULTADOS EVIDENCIA
            Spacer(modifier = Modifier.height(16.dp))

            if (fotoBitmap != null) {
                Text("Foto adjunta:", style = MaterialTheme.typography.labelLarge)
                Image(
                    bitmap = fotoBitmap!!.asImageBitmap(),
                    contentDescription = "Foto",
                    modifier = Modifier.height(200.dp).fillMaxWidth()
                )
            }

            if (ubicacion != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Ubicación registrada:")
                        Text("Lat: ${ubicacion!!.latitude}, Long: ${ubicacion!!.longitude}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. BOTÓN GUARDAR (VIBRAR)
            Button(
                onClick = { vibrarCelular() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("GUARDAR REPORTE", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}