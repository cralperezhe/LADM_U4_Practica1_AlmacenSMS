package com.example.ladm_u4_practica1_almacensms

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    var baseDatos = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mostrarTodos()

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS), 111)
        }else receiveMsg()

        botonEnviar.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                textoFecha.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).toString())
            val sms = SmsManager.getDefault()
            sms.sendTextMessage(textoDestinatario.text.toString(), "ME", textoMensaje.text.toString(), null, null)
            val datos = hashMapOf<String, String>()
            datos["Destinatario"] = textoDestinatario.text.toString()
            datos["Mensaje"] = textoMensaje.text.toString()
            datos["Fecha"] = textoFecha.text.toString()
            baseDatos.collection("smsenviados")
                .add(datos)
                .addOnSuccessListener {
                    AlertDialog.Builder(this)
                        .setTitle("ATENCION")
                        .setMessage("Se envió y guardó correctamente el mensaje al destinatario en la nubede Firestore.")
                        .show()
                }
                .addOnFailureListener {
                    AlertDialog.Builder(this)
                        .setTitle("ATENCION")
                        .setMessage("No se pudo enviar correctamente.")
                        .show()
                }
            mostrarTodos()
        }
    }
    private fun mostrarTodos() {
        val lista = ArrayList<String>()
        baseDatos.collection("smsenviados").addSnapshotListener { value, error ->
            if(error != null){
                AlertDialog.Builder(this)
                    .setTitle("ATENCIÓN")
                    .setMessage("No se pudo realizar la consulta")
                    .setPositiveButton("Ok"){_,_ ->}
                    .show()
            }
            for(documento in value!!){
                val cadena = "Destinatario: ${documento.getString("Destinatario")}\nMensaje:\n${documento.getString("Mensaje")}\nFecha: ${documento.getString("Fecha")}"
                lista.add(cadena)
            }
            listaMensajesEnviados.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, lista)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==111 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            receiveMsg()
    }

    private fun receiveMsg() {
        val br = object : BroadcastReceiver(){
            override fun onReceive(p0: Context?, p1: Intent?) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    for(sms in Telephony.Sms.Intents.getMessagesFromIntent(p1)) {
                        textoDestinatario.setText(sms.originatingAddress)
                        textoMensaje.setText(sms.displayMessageBody)

                    }
            }
        }
        registerReceiver(br, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
    }
}