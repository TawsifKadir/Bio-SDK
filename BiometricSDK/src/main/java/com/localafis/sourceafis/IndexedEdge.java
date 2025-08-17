// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.localafis.sourceafis;

class IndexedEdge extends EdgeShape {
	final int reference;
	final int neighbor;
	IndexedEdge(ImmutableMinutia[] minutiae, int reference, int neighbor) {
		super(minutiae[reference], minutiae[neighbor]);
		this.reference = reference;
		this.neighbor = neighbor;
	}
}
