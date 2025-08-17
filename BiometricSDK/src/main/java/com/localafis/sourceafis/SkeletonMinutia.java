// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.localafis.sourceafis;

import java.util.ArrayList;
import java.util.List;

class SkeletonMinutia {
	final IntPoint position;
	final List<SkeletonRidge> ridges = new ArrayList<>();
	SkeletonMinutia(IntPoint position) {
		this.position = position;
	}
	void attachStart(SkeletonRidge ridge) {
		if (!ridges.contains(ridge)) {
			ridges.add(ridge);
			ridge.start(this);
		}
	}
	void detachStart(SkeletonRidge ridge) {
		if (ridges.contains(ridge)) {
			ridges.remove(ridge);
			if (ridge.start() == this)
				ridge.start(null);
		}
	}
	@Override public String toString() {
		return String.format("%s*%d", position.toString(), ridges.size());
	}
}
