package com.asmr.player.smb
import android.net.Uri
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import java.util.Properties
internal object SmbClientFactory {
 @Volatile private var cachedKey:String?=null
 @Volatile private var cachedContext:CIFSContext?=null
 fun rootFile(c:SmbConfig)=SmbFile(baseUrl(c),context(c))
 fun file(c:SmbConfig,p:String,d:Boolean):SmbFile { val clean=p.trim('/'); val u=if(clean.isBlank()) baseUrl(c) else baseUrl(c)+encodePath(clean)+if(d)"/" else ""; return SmbFile(u,context(c)) }
 fun baseUrl(c:SmbConfig):String { val x=c.normalized(); val prefix="smb://"+x.host+"/"+encodeSegment(x.share)+"/"; return if(x.basePath.isBlank())prefix else prefix+encodePath(x.basePath)+"/" }
 private fun context(c:SmbConfig):CIFSContext {
  val x=c.normalized(); val key=listOf(x.host,x.share,x.basePath,x.domain,x.username,x.password).joinToString("\u0000")
  cachedContext?.takeIf{cachedKey==key}?.let{return it}
  return synchronized(this){ cachedContext?.takeIf{cachedKey==key}?:run{
   val p=Properties().apply{
    setProperty("jcifs.smb.client.minVersion","SMB202"); setProperty("jcifs.smb.client.maxVersion","SMB311")
    setProperty("jcifs.smb.client.useSMB2Negotiation","true"); setProperty("jcifs.smb.client.signingPreferred","true")
    setProperty("jcifs.smb.client.ipcSigningEnforced","false"); setProperty("jcifs.smb.client.connTimeout","12000")
    setProperty("jcifs.smb.client.responseTimeout","20000"); setProperty("jcifs.smb.client.soTimeout","45000")
   }
   val b=BaseContext(PropertyConfiguration(p))
   val a=if(x.username.isBlank())b.withGuestCredentials() else b.withCredentials(NtlmPasswordAuthenticator(x.domain,x.username,x.password))
   cachedKey=key;cachedContext=a;a
  }}
 }
 private fun encodePath(p:String)=p.split('/').filter{it.isNotEmpty()}.joinToString("/"){encodeSegment(it)}
 private fun encodeSegment(s:String)=Uri.encode(s)
}
