package com.asmr.player.smb
import android.app.Activity
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.InputType
import android.view.*
import android.widget.*
import jcifs.smb.SmbException
import java.util.concurrent.Executors

class SmbSettingsActivity:Activity(){
 private val exec=Executors.newSingleThreadExecutor();private lateinit var store:SmbConfigStore
 private lateinit var display:EditText;private lateinit var host:EditText;private lateinit var share:EditText;private lateinit var path:EditText;private lateinit var domain:EditText;private lateinit var user:EditText;private lateinit var pass:EditText;private lateinit var status:TextView;private lateinit var test:Button
 override fun onCreate(b:Bundle?){super.onCreate(b);title="Eara SMB3 設定";store=SmbConfigStore(this);setContentView(ui());store.load()?.let{display.setText(it.displayName);host.setText(it.host);share.setText(it.share);path.setText(it.basePath);domain.setText(it.domain);user.setText(it.username);pass.setText(it.password)}}
 override fun onDestroy(){exec.shutdownNow();super.onDestroy()}
 private fun ui():View{val d=resources.displayMetrics.density;fun dp(x:Int)=(x*d).toInt();val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(18),dp(20),dp(24))}
  c.addView(TextView(this).apply{text="SMB2 / SMB3 NAS";textSize=24f});c.addView(TextView(this).apply{text="支援 SMB 2.0.2～3.1.1。儲存後在 Eara → 媒體庫 → 加入資料夾，選擇 Eara SMB。"})
  fun field(label:String,hint:String):EditText{c.addView(TextView(this).apply{text=label;setPadding(0,dp(10),0,0)});return EditText(this).apply{this.hint=hint;singleLine=true;c.addView(this)}}
  display=field("顯示名稱","我的 NAS");host=field("NAS IP / Host","192.168.4.3");share=field("Share 名稱","大倉NAS");path=field("起始子資料夾","music/ASMR");domain=field("Domain（通常留白）","");user=field("帳號","");pass=field("密碼","").apply{inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD}
  status=TextView(this).apply{setPadding(0,dp(12),0,dp(10))};c.addView(status);val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.END};test=Button(this).apply{text="測試連線";setOnClickListener{test()}};row.addView(test);row.addView(Button(this).apply{text="儲存";setOnClickListener{save()}});c.addView(row);return ScrollView(this).apply{addView(c)}
 }
 private fun read()=SmbConfig(display.text.toString(),host.text.toString(),share.text.toString(),path.text.toString(),domain.text.toString(),user.text.toString(),pass.text.toString()).normalized()
 private fun save(){val c=read();c.validate()?.let{status.text=it;return};runCatching{store.save(c)}.onSuccess{runCatching{contentResolver.notifyChange(DocumentsContract.buildRootsUri(packageName+SmbDocumentsProvider.AUTHORITY_SUFFIX),null)};status.text="已儲存"}.onFailure{status.text="儲存失敗："+(it.message?:it.javaClass.simpleName)}}
 private fun test(){val c=read();c.validate()?.let{status.text=it;return};test.isEnabled=false;status.text="測試 SMB3 連線中…";exec.execute{val r=runCatching{SmbClientFactory.rootFile(c).use{f->if(!f.exists())error("分享不存在");if(!f.isDirectory)error("不是資料夾");"連線成功，可讀取 "+(f.listFiles()?.size?:0)+" 個項目"}};runOnUiThread{test.isEnabled=true;status.text=r.fold({it},{"連線失敗："+failure(it)})}}}
 private fun failure(e:Throwable):String{val s=generateSequence(e){it.cause}.filterIsInstance<SmbException>().firstOrNull();return if(s!=null)(s.message?:s.javaClass.simpleName)+"（NTSTATUS 0x"+"%08X".format(s.ntStatus)+"）" else e.message?:e.javaClass.simpleName}
}
