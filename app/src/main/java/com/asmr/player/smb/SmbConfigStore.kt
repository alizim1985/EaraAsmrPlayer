package com.asmr.player.smb
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
internal class SmbConfigStore(context:Context){
 private val prefs=context.getSharedPreferences("smb_config",Context.MODE_PRIVATE)
 fun save(c:SmbConfig){val x=c.normalized();val j=JSONObject().put("displayName",x.displayName).put("host",x.host).put("share",x.share).put("basePath",x.basePath).put("domain",x.domain).put("username",x.username).put("password",x.password).toString();prefs.edit().putString("encrypted_config_v1",encrypt(j)).apply()}
 fun load():SmbConfig?{val b=prefs.getString("encrypted_config_v1",null)?:return null;return runCatching{val j=JSONObject(decrypt(b));SmbConfig(j.optString("displayName","NAS"),j.getString("host"),j.getString("share"),j.optString("basePath",""),j.optString("domain",""),j.optString("username",""),j.optString("password","")).normalized()}.getOrNull()}
 private fun encrypt(s:String):String{val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());val e=c.doFinal(s.toByteArray());val iv=c.iv;return Base64.encodeToString(ByteBuffer.allocate(1+iv.size+e.size).put(iv.size.toByte()).put(iv).put(e).array(),Base64.NO_WRAP)}
 private fun decrypt(s:String):String{val b=ByteBuffer.wrap(Base64.decode(s,Base64.NO_WRAP));val n=b.get().toInt() and 255;require(n in 12..32);val iv=ByteArray(n).also(b::get);val e=ByteArray(b.remaining()).also(b::get);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv));return c.doFinal(e).toString(Charsets.UTF_8)}
 private fun key():SecretKey{val k=KeyStore.getInstance("AndroidKeyStore").apply{load(null)};(k.getKey("eara_smb_config_key_v1",null) as? SecretKey)?.let{return it};val g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(KeyGenParameterSpec.Builder("eara_smb_config_key_v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey()}
}
