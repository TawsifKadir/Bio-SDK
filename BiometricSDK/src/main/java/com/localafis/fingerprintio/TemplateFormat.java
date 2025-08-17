// Part of FingerprintIO: https://fingerprintio.machinezoo.com
package com.localafis.fingerprintio;

import com.localafis.fingerprintio.ansi378.Ansi378Template;
import com.localafis.fingerprintio.ansi378.Ansi378v2009Am1Template;
import com.localafis.fingerprintio.ansi378.Ansi378v2009Template;
import com.localafis.fingerprintio.iso19794p2.Iso19794p2Template;
import com.localafis.fingerprintio.ansi378.*;
import com.localafis.fingerprintio.iso19794p2.*;

public enum TemplateFormat {
	ANSI_378,
	ANSI_378_2009,
	ANSI_378_2009_AM1,
	ISO_19794_2;
	public static TemplateFormat detect(byte[] template) {
		if (Ansi378Template.accepts(template))
			return ANSI_378;
		if (Ansi378v2009Template.accepts(template))
			return ANSI_378_2009;
		if (Ansi378v2009Am1Template.accepts(template))
			return ANSI_378_2009_AM1;
		if (Iso19794p2Template.accepts(template))
			return ISO_19794_2;
		return null;
	}
}
