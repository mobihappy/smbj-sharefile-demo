package com.spctn.smbjsharefile

import android.Manifest
import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar.visibility = View.GONE

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            1
        )

        btnClick.setOnClickListener {
            MyUpload().execute("")
        }

    }

    @SuppressLint("StaticFieldLeak")
    inner class MyUpload : AsyncTask<String?, String?, String?>() {
        private var sambaUsername = "PC-MOBILE"
        private var sambaPass = "123456"
        private var sambaIP = "192.168.1.152"
        private var sambaSharedPath = "data"

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg p0: String?): String? {
            val fileName = getStringDateYMDHHMMSS(System.currentTimeMillis()) + ".txt"
            upload(fileName, "hoangtung".toByteArray())
            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            progressBar.visibility = View.GONE
            Toast.makeText(this@MainActivity, "Success!", Toast.LENGTH_LONG).show()
        }

        @Throws(IOException::class)
        fun upload(filename: String, bytes: ByteArray?) {
            val cfg = SmbConfig.builder().build()
            val client = SMBClient(cfg)
            val connection: Connection = client.connect(sambaIP)
            val session: Session = connection.authenticate(
                AuthenticationContext(
                    sambaUsername,
                    sambaPass.toCharArray(),
                    null
                )
            )
            val share = session.connectShare(sambaSharedPath) as DiskShare

            // this is com.hierynomus.smbj.share.File !
            var f: File? = null
            val idx = filename.lastIndexOf("/")

            // if file is in folder(s), create them first
            if (idx > -1) {
                val folder = filename.substring(0, idx)
                try {
                    if (!share.folderExists(folder)) share.mkdir(folder)
                } catch (ex: SMBApiException) {
                    throw IOException(ex)
                }
            }

            // I am creating file with flag FILE_CREATE, which will throw if file exists already
            if (!share.fileExists(filename)) {
                f = share.openFile(
                    filename,
                    HashSet(listOf(AccessMask.GENERIC_ALL)),
                    HashSet(listOf(FileAttributes.FILE_ATTRIBUTE_NORMAL)),
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_CREATE,
                    HashSet(listOf(SMB2CreateOptions.FILE_DIRECTORY_FILE))
                )
            }
            if (f == null) return
            val os = f.outputStream
            os.write(bytes)
            os.close()

            share.close()
        }
    }

    fun getStringDateYMDHHMMSS(time: Long): String {
        val date = Date()
        date.time = time
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
        return dateFormat.format(date)
    }

}