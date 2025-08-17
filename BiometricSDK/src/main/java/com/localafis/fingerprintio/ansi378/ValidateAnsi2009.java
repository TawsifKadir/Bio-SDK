// Part of FingerprintIO: https://fingerprintio.machinezoo.com
package com.localafis.fingerprintio.ansi378;

import com.localafis.fingerprintio.utils.Validate;

class ValidateAnsi2009 {
	static void quality(int value, String message) {
		Validate.condition(value >= 0 && value <= 100 || value == 254 || value == 255, message);
	}
}
