package com.asmr.player.smb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class SmbConfigTest {
 @Test fun normalizesFnOsPathParts(){val c=SmbConfig(" 我的 NAS ","smb://192.168.4.3/","/大倉NAS/","/music/ASMR/",""," user ","pw").normalized();assertEquals("192.168.4.3",c.host);assertEquals("大倉NAS",c.share);assertEquals("music/ASMR",c.basePath);assertNull(c.validate())}
 @Test fun rejectsHostWithPath(){assertEquals("主機請只填 IP 或主機名稱，不要包含資料夾路徑",SmbConfig(host="192.168.4.3/music",share="ASMR").validate())}
}
