package com.asmr.player.smb
import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import java.io.IOException
internal class SmbProxyFileCallback(private val file:SmbFile):ProxyFileDescriptorCallback(){
 private val lock=Any();private var raf:SmbRandomAccessFile?=null
 override fun onGetSize():Long=io("size"){file.length()}
 override fun onRead(offset:Long,size:Int,data:ByteArray):Int=synchronized(lock){io("read"){val r=raf?:file.openRandomAccess("r").also{raf=it};r.seek(offset);r.read(data,0,size).coerceAtLeast(0)}}
 override fun onRelease(){synchronized(lock){runCatching{raf?.close()};raf=null;runCatching{file.close()}}}
 private fun <T> io(n:String,b:()->T):T=try{b()}catch(_:java.io.FileNotFoundException){throw ErrnoException(n,OsConstants.ENOENT)}catch(_:IOException){throw ErrnoException(n,OsConstants.EIO)}catch(_:Throwable){throw ErrnoException(n,OsConstants.EIO)}
}
