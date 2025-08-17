// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.localafis.sourceafis;

enum SkeletonType {
	RIDGES("ridges-"), VALLEYS("valleys-");
	final String prefix;
	SkeletonType(String prefix) {
		this.prefix = prefix;
	}
}
