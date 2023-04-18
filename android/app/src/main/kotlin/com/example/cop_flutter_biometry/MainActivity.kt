package com.example.cop_flutter_biometry

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.RSAKeyGenParameterSpec

class MainActivity: FlutterFragmentActivity() {
  private val channel = "com.example.cop_flutter_biometry"
  private lateinit var biometricManager : BiometricManager

  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    biometricManager = BiometricManager.from(this)
    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channel).setMethodCallHandler {
      call, result ->
      if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !deviceHasHardware()) {
        result.error("NotSupported", "This device can not support biometrics", null)
      } else {
        when(call.method) {
          "generate_public_key" ->  handleGeneratePublicKey(call, result)
          "generate_signature_for_key" ->  handleGenerateSignatureForKey(call, result)
          else -> result.notImplemented()
        }
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  fun handleGeneratePublicKey(call : MethodCall, result : MethodChannel.Result) {
    val keyName = call.argument("keyName") as String?

    val keyGenParams = KeyGenParameterSpec.Builder(keyName!!, KeyProperties.PURPOSE_SIGN)
      .setDigests(KeyProperties.DIGEST_SHA256)
      .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
      .setAlgorithmParameterSpec(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
      .setUserAuthenticationRequired(true) // important!
      .build()

    val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
    keyPairGenerator.initialize(keyGenParams)

    val keyPair = keyPairGenerator.generateKeyPair()
    val pubKey = Base64.encodeToString(keyPair.public.encoded!!, Base64.DEFAULT)

    result.success(pubKey)
  }

  private fun handleGenerateSignatureForKey(call : MethodCall, result : MethodChannel.Result) {
    val keyName = call.argument("keyName") as String?
    val payload = call.argument("payload") as String?

    val signature = Signature.getInstance("SHA256withRSA")
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    val privateKey = keyStore.getKey(keyName!!, null) as PrivateKey
    signature.initSign(privateKey)

    val cryptoObject = BiometricPrompt.CryptoObject(signature)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(authResult: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(authResult)
        sendSignature(payload!!, result, authResult)
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        result.error("PromptError", "Authentication failed", null)
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        result.error("PromptError", "Authentication failed", null)
      }
    }

    val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle("Autenticação por biometria")
      .setDescription("Sua autenticação é necessária para o acesso ao sistema")
      .setNegativeButtonText("Cancelar")
      .setConfirmationRequired(true)
      .build()

    biometricPrompt.authenticate(promptInfo, cryptoObject)
  }

  fun sendSignature(payload : String, result : MethodChannel.Result, authResult: BiometricPrompt.AuthenticationResult) {
    val signature = authResult.cryptoObject!!.signature!!
    signature.update(payload.encodeToByteArray())
    val signatureBytes = signature.sign()
    val signatureString = Base64.encodeToString(signatureBytes, Base64.DEFAULT)
      .replace("\n", "")
      .replace("\r", "")
    result.success(signatureString)
  }

  private fun deviceHasHardware() : Boolean {
    return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
  }

  companion object {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val SIGNATURE_ALGORITHM = "SHA256withRSA"
  }
}
