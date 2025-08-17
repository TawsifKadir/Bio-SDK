// Part of FingerprintIO: https://fingerprintio.machinezoo.com
package com.localafis.fingerprintio.ansi378;

import com.localafis.fingerprintio.utils.Validate;

class ValidateAnsi {
	static void angle(int value, String message) {
		Validate.range(value, 0, 179, message);
	}
}
