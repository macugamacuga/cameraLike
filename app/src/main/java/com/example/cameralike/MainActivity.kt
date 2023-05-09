package com.example.cameralike

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.number.NumberFormatter.with
import android.icu.number.NumberRangeFormatter.with
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.cameralike.databinding.ActivityMainBinding

import com.google.android.gms.cast.framework.media.ImagePicker
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    //if uding camera controller
   private lateinit var cameraController: LifecycleCameraController
    //if using cameraProvider
    //private var imageCapture:ImageCapture? = null
    private lateinit var imageAnalysis: ImageAnalysis
private lateinit var cameraSelector:CameraSelector
    private lateinit var imageView: ImageView
    private lateinit var cameraPreview: Preview
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if(!hasPermissions(baseContext)) {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }
        else{
            //lifecycleScope.launch { startCamera() }
            startCamera()

        }
imageView = viewBinding.filterView
        viewBinding.takePhoto.setOnClickListener{takephoto()}
        val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val selectedImage = result.data?.data
                Glide.with(this)
                    .load(selectedImage)
                    .into(imageView)
            }
        }

        viewBinding.GalleryProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)

        }
    }

    private fun startCamera(){
        val  preview:PreviewView= viewBinding.viewFinder
        cameraController = LifecycleCameraController(baseContext)


        cameraController.bindToLifecycle(this)
        bindInputeAnaliser()
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        preview.controller= cameraController


    }
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindInputeAnaliser(){
        val barcodeScanner:BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        )
        imageAnalysis= ImageAnalysis.Builder()
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor){imageProxy ->

            val inputImage = InputImage.fromMediaImage(imageProxy.image!!,imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(inputImage)

                .addOnSuccessListener { barcodes ->
                Toast.makeText(this,"Scanned code",Toast.LENGTH_LONG).show()

                }.addOnFailureListener{
it.printStackTrace()
                }.addOnCompleteListener {
                    imageProxy.close()
                }
        }
        cameraController.bindToLifecycle(this)

    }
   /* private suspend fun startCamera(){
        val cameraProvider =ProcessCameraProvider.getInstance(this).await()

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)

        imageCapture = ImageCapture.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            var camera = cameraProvider.bindToLifecycle(
                this,cameraSelector,preview,imageCapture
            )
        }catch (e:Exception){
            Log.e(TAG,"UseCase binding failed",e)
        }
    }*/

 private fun takephoto(){
     val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
         .format(System.currentTimeMillis())
     val contentValues = ContentValues().apply {
         put(MediaStore.MediaColumns.DISPLAY_NAME,name)
         put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg")
         if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
             put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/cameraLike-image ")
         }
     }
     //create output options object which contains file +  metadata
     val  outputOptions = ImageCapture.OutputFileOptions
         .Builder(contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
         contentValues)
         .build()
//set up image capture listener
     cameraController.takePicture(
         outputOptions,
         ContextCompat.getMainExecutor(this),
         object :ImageCapture.OnImageSavedCallback{
             override fun onError(exc: ImageCaptureException) {
                 Log.e(TAG,"Photo capture failed: ${exc.message}",exc)
             }

             override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                 val msg = "photo capture succeeded${outputFileResults.savedUri}"
                 Toast.makeText(baseContext,msg,Toast.LENGTH_SHORT).show()
                 Log.d(TAG,msg)
             }

         }
     )
 }
    private val  activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            //hundle permissiongranted/rejcted
            var permissionGranted =true
            permissions.entries.forEach{
                if(it.key in REQUIRED_PERMISSIONS && it.value==false)
                    permissionGranted=false
            }
            if(!permissionGranted) {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
            else {
               // lifecycleScope.launch { startCamera() }
                startCamera()
            }
        }
    companion object{
        private const val  TAG ="CameraXApp"
        private const val   FILENAME_FORMAT = "YYYY-MM-dd-HH-mm-ss-SSS"
        private val  REQUIRED_PERMISSIONS =
            mutableListOf (
                android.Manifest.permission.CAMERA
                    ).apply {
                        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
            }.toTypedArray()
        fun  hasPermissions(context: Context) = REQUIRED_PERMISSIONS.all{
            ContextCompat.checkSelfPermission(context,it)== PackageManager.PERMISSION_GRANTED
        }
    }

   fun Grauscale(image: ImageProxy) {

       val  bitmap=
       image.close()
   }
}