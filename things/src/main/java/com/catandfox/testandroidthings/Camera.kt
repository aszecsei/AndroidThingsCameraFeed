package com.catandfox.testandroidthings

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast


class Camera {
    private var context: Context? = null
    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private var textureView: TextureView? = null

    private var captureRequest: CaptureRequest? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSessions: CameraCaptureSession? = null

    private var mBackgroundHandler : Handler? = null

    companion object {
        final val TAG = "AndroidThingCamera"
        val Instance = Camera()
    }


    fun openCamera(context: Context, textureView: TextureView, backgroundHandler: Handler?) {
        this.context = context
        this.textureView = textureView
        this.mBackgroundHandler = backgroundHandler

        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.d(TAG, "is camera open")
        try {
            this.cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            manager.openCamera(cameraId, stateCallback, null)
        } catch(e: CameraAccessException) {
            Log.e(TAG, "Camera access exception", e)
        }
        Log.d(TAG, "openCamera X")
    }

    fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null

        imageReader?.close()
        imageReader = null
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(p0: CameraDevice?) {
            Log.d(TAG, "onOpened")
            cameraDevice = p0
            createCameraPreview()
        }

        override fun onDisconnected(p0: CameraDevice?) {
            cameraDevice?.close()
        }

        override fun onError(p0: CameraDevice?, p1: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }

    }

    private fun createCameraPreview() {
        try {
            val texture = textureView?.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)
            cameraDevice?.createCaptureSession(arrayListOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(p0: CameraCaptureSession?) {
                    Toast.makeText(context!!, "Configuration change", Toast.LENGTH_SHORT).show()
                }

                override fun onConfigured(p0: CameraCaptureSession?) {
                    if(cameraDevice == null) return
                    cameraCaptureSessions = p0
                    updatePreview()
                }

            }, null)
        } catch(e: CameraAccessException) {
            Log.e(TAG, "Camera access exception", e)
        }
    }

    private fun updatePreview() {
        if(cameraDevice == null) {
            Log.e(TAG, "updatePreview error, return")
        }
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions?.setRepeatingRequest(captureRequestBuilder?.build(), null, mBackgroundHandler)
        } catch(e: CameraAccessException) {
            Log.e(TAG, "updatePreview error", e)
        }
    }
}