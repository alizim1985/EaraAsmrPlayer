package com.asmr.player.smb
import android.database.Cursor
import android.database.MatrixCursor
import android.os.*
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.asmr.player.R
import jcifs.smb.SmbFile
import java.io.FileNotFoundException

class SmbDocumentsProvider:DocumentsProvider(){
 private lateinit var store:SmbConfigStore
 private lateinit var thread:HandlerThread
 private lateinit var handler:Handler
 override fun onCreate():Boolean{val c=context?:return false;store=SmbConfigStore(c);thread=HandlerThread("eara-smb").apply{start()};handler=Handler(thread.looper);return true}
 override fun queryRoots(p:Array<out String>?):Cursor{val cols=p?:ROOT_COLS;val out=MatrixCursor(cols);val c=store.load();if(c!=null&&c.validate()==null){val r=out.newRow();put(r,cols,DocumentsContract.Root.COLUMN_ROOT_ID,"eara-smb");put(r,cols,DocumentsContract.Root.COLUMN_DOCUMENT_ID,"root");put(r,cols,DocumentsContract.Root.COLUMN_TITLE,c.displayName);put(r,cols,DocumentsContract.Root.COLUMN_SUMMARY,"SMB3 · "+c.host+"/"+c.share);put(r,cols,DocumentsContract.Root.COLUMN_FLAGS,DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD);put(r,cols,DocumentsContract.Root.COLUMN_ICON,R.mipmap.ic_launcher)};return out}
 override fun queryDocument(id:String,p:Array<out String>?):Cursor{val cols=p?:DOC_COLS;val out=MatrixCursor(cols);val c=config();val ref=decode(id);SmbClientFactory.file(c,ref.first,ref.second).use{f->if(!f.exists())throw FileNotFoundException(id);include(out,cols,c,ref.first,f)};return out}
 override fun queryChildDocuments(id:String,p:Array<out String>?,s:String?):Cursor{val cols=p?:DOC_COLS;val out=MatrixCursor(cols);val c=config();val parent=decode(id).first;SmbClientFactory.file(c,parent,true).use{d->if(!d.exists()||!d.isDirectory)throw FileNotFoundException(id);val children=d.listFiles()?:emptyArray();try{children.sortedWith(compareBy<SmbFile>({!it.isDirectory},{it.name.lowercase()})).forEach{f->val n=f.name.trimEnd('/');include(out,cols,c,if(parent.isBlank())n else parent.trimEnd('/')+"/"+n,f)}}finally{children.forEach{runCatching{it.close()}}}};return out}
 override fun openDocument(id:String,mode:String,signal:CancellationSignal?):ParcelFileDescriptor{if(mode!="r")throw FileNotFoundException("read only");if(Build.VERSION.SDK_INT<26)throw FileNotFoundException("SMB seek requires Android 8+");val c=config();val ref=decode(id);if(ref.second)throw FileNotFoundException(id);val f=SmbClientFactory.file(c,ref.first,false);if(!f.exists()||f.isDirectory)throw FileNotFoundException(id);val sm=context!!.getSystemService(StorageManager::class.java);return sm.openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY,SmbProxyFileCallback(f),handler)}
 override fun getDocumentType(id:String):String{val r=decode(id);return if(r.second)DocumentsContract.Document.MIME_TYPE_DIR else mime(r.first.substringAfterLast('/'))}
 override fun isChildDocument(parentDocumentId:String,documentId:String):Boolean{val p=decode(parentDocumentId).first.trim('/');val c=decode(documentId).first.trim('/');return if(p.isBlank())c.isNotBlank() else c.startsWith(p+"/")}
 private fun include(cur:MatrixCursor,cols:Array<out String>,c:SmbConfig,path:String,f:SmbFile){val dir=f.isDirectory;val name=if(path.isBlank())c.displayName else f.name.trimEnd('/');val r=cur.newRow();put(r,cols,DocumentsContract.Document.COLUMN_DOCUMENT_ID,encode(path,dir));put(r,cols,DocumentsContract.Document.COLUMN_DISPLAY_NAME,name);put(r,cols,DocumentsContract.Document.COLUMN_MIME_TYPE,if(dir)DocumentsContract.Document.MIME_TYPE_DIR else mime(name));put(r,cols,DocumentsContract.Document.COLUMN_SIZE,if(dir)null else f.length());put(r,cols,DocumentsContract.Document.COLUMN_LAST_MODIFIED,f.lastModified())}
 private fun config()=store.load()?.takeIf{it.validate()==null}?:throw FileNotFoundException("請先設定 SMB")
 private fun mime(n:String)=MimeTypeMap.getSingleton().getMimeTypeFromExtension(n.substringAfterLast('.', "").lowercase())?:when(n.substringAfterLast('.', "").lowercase()){"flac"->"audio/flac";"opus"->"audio/opus";"lrc"->"text/plain";"srt"->"application/x-subrip";"vtt"->"text/vtt";else->"application/octet-stream"}
 private fun encode(p:String,d:Boolean):String{if(p.isBlank())return "root";val e=android.util.Base64.encodeToString(p.toByteArray(),android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING);return(if(d)"d:" else "f:")+e}
 private fun decode(id:String):Pair<String,Boolean>{if(id=="root")return "" to true;val d=when{ id.startsWith("d:")->true;id.startsWith("f:")->false;else->throw FileNotFoundException(id)};return runCatching{android.util.Base64.decode(id.substring(2),android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING).toString(Charsets.UTF_8) to d}.getOrElse{throw FileNotFoundException(id)}}
 private fun put(r:MatrixCursor.RowBuilder,c:Array<out String>,n:String,v:Any?){if(c.contains(n))r.add(n,v)}
 companion object{const val AUTHORITY_SUFFIX=".smb.documents";private val ROOT_COLS=arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID,DocumentsContract.Root.COLUMN_DOCUMENT_ID,DocumentsContract.Root.COLUMN_TITLE,DocumentsContract.Root.COLUMN_SUMMARY,DocumentsContract.Root.COLUMN_FLAGS,DocumentsContract.Root.COLUMN_ICON);private val DOC_COLS=arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED)}
}
